/*
 * Copyright 2016 Yodle, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yodle.vantage.component.service;

import static com.yodle.vantage.component.service.MavenVersionUtils.isMavenStrategy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.google.common.collect.Sets;
import com.yodle.vantage.component.dao.ComponentDao;
import com.yodle.vantage.component.dao.IssueDao;
import com.yodle.vantage.component.dao.QueueDao;
import com.yodle.vantage.component.dao.VersionDao;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.domain.VersionId;

@Component
@Transactional
public class ComponentService {
    @Autowired private ComponentDao componentDao;
    @Autowired private VersionDao versionDao;
    @Autowired private IssueDao issueDao;
    @Autowired private QueueDao queueDao;
    @Autowired private PrecedenceFixer precedenceFixer;

    private Logger l = LoggerFactory.getLogger(ComponentService.class);

    public Optional<VantageComponent> getComponent(String name) {
        return componentDao.getComponent(name);
    }

    public Version createOrUpdateDryRunVersion(Version version) {
        //Currently we have concurrency issued doing multiple create/updates at the same time, so take the queue head lock
        //to sync with both other dry run creates and with real creates
        queueDao.lockQueueHead();
        //we do not save requested dependencies for dry-run creates since we're doing this to find out what issues
        //affect this version and the requested dependencies don't contribute to that.
        Version resolvedVersion = createOrUpdateVersion(version, true);
        //Force a rollback because this is a dry run create
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return resolvedVersion;
    }

    public VantageComponent createOrUpdateComponent(VantageComponent component) {
        return componentDao.createOrUpdateComponent(component);
    }

    public Version createOrUpdateVersion(Version version) {
        return createOrUpdateVersion(version, false);
    }

    private Version createOrUpdateVersion(Version version, boolean excludeRequestedDependencies) {
        l.info("Creating version [{}] for component [{}]", version.getVersion(), version.getComponent());
        //ensure component exists
        componentDao.ensureCreated(version.getComponent());
        Set<VersionId> componentsWithVersionsCreated = new HashSet<>();

        //create new version node and link it w/ component + previous versions
        if (versionDao.createNewVersion(version.getComponent(), version.getVersion())) {
            componentsWithVersionsCreated.add(version.toId());
        }
        l.info("Saving [{}] resolved dependencies for version [{}], component [{}]", version.getResolvedDependencies().size(), version.getVersion(), version.getComponent());
        //create resolved dependencies as implicit versions
        for (Dependency dep : version.getResolvedDependencies()) {
            componentDao.ensureCreated(dep.getVersion().getComponent());
            if (versionDao.createResolvedImplicitDependency(version, dep.getVersion(), dep.getProfiles())) {
                componentsWithVersionsCreated.add(dep.getVersion().toId());
            }
            if (!excludeRequestedDependencies) {
                saveRequestedDependencies(dep.getVersion(), componentsWithVersionsCreated);
            }
        }

        if (!excludeRequestedDependencies) {
            l.info("Saving [{}] requested dependencies for version [{}], component [{}]", version.getRequestedDependencies().size(), version.getVersion(), version.getComponent());
            saveRequestedDependencies(version, componentsWithVersionsCreated);
        }

        l.info("Fixing precedence for [{}] created versions for version [{}], component [{}]", componentsWithVersionsCreated.size(), version.getVersion(), version.getComponent());
        precedenceFixer.fixPrecedence(componentsWithVersionsCreated);

        l.info("Finished saving [{}]:[{}]", version.getComponent(), version.getVersion());
        return getVersion(version.getComponent(), version.getVersion()).orElseThrow(() -> new RuntimeException("Could not find a version [" + version.getVersion() + "] for component [" + version.getComponent() + "] after creating it"));
    }


    private void saveRequestedDependencies(Version parent, Set<VersionId> componentsWithVersionsCreated) {
        for (Dependency dep : parent.getRequestedDependencies()) {
            componentDao.ensureCreated(dep.getVersion().getComponent());
            if (versionDao.createRequestedImplicitDependency(parent, dep.getVersion(), dep.getProfiles())) {
                componentsWithVersionsCreated.add(dep.getVersion().toId());
            }
        }
    }

    public Optional<List<Version>> getVersions(String component) {
        return getComponent(component).map(c -> {
            List<String> versions = versionDao.getVersions(component);
            Set<String> activeVersions = Sets.newHashSet(versionDao.getActiveVersions(component));
            Map<String,Collection<Issue>> directIssues = issueDao.getIssuesDirectlyAffectingVersions(component);
            Map<String,Collection<Issue>> transitiveIssues = issueDao.getIssuesTransitivelyAffectingVersions(component);

            List<Version> vs = versions.stream().map(vName -> {
                Version  v = new Version(component, vName, activeVersions.contains(vName));
                v.setDirectIssues(directIssues.get(v.getVersion()));
                v.setTransitiveIssues(transitiveIssues.get(v.getVersion()));
                return v;
            }).collect(Collectors.toList());

            if (isMavenStrategy(versions)) {
                vs = vs.stream()
                        .sorted((v1, v2) ->
                                new ComparableVersion(v2.getVersion()).compareTo(new ComparableVersion(v1.getVersion()))
                        )
                        .collect(Collectors.toList());
            }

            return vs;
        });
    }

    public Optional<Version> getVersion(String component, String version) {
        Optional<Version> versionOpt = versionDao.getVersion(component, version);
        versionOpt.ifPresent( v -> {
            List<Issue> directIssues = issueDao.getIssuesDirectlyAffectingVersion(component, version);
            v.setDirectIssues(directIssues);

            Map<Version,Collection<Issue>> issuesByDependency = issueDao.getIssuesByDependenciesOf(component, version);
            v.getResolvedDependencies().stream().forEach(dep -> dep.getVersion().setDirectIssues(issuesByDependency.get(dep.getVersion())));
            v.setTransitiveIssues(issuesByDependency.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

        });

        return versionOpt;
    }

    public void ensureVersionExists(String component, String version) {
        componentDao.ensureCreated(component);
        boolean created = versionDao.createNewVersion(component, version);
        if (created) {
            precedenceFixer.fixPrecendence(new Version(component, version));
        }
    }


    public List<VantageComponent> getAllComponents() {
        return componentDao.getComponentsWithMostRecentVersion();
    }
}
