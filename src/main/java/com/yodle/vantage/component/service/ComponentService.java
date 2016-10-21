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
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.google.common.collect.Lists;
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
    @Autowired private VersionPurifier versionPurifier;
    @Value("${vantage.require-dry-run-lock:false}") private boolean requireDryRunLock;

    private Logger l = LoggerFactory.getLogger(ComponentService.class);

    public Optional<VantageComponent> getComponent(String name) {
        return componentDao.getComponent(name);
    }

    public Version createOrUpdateDryRunVersion(Version version) {
        if (requireDryRunLock) {
            //Previous concurrency issues with multiple parallel dry-run creates should be fixed, however this has been
            //left in as an option as a safety valve in case any new ones are discovered
            queueDao.lockQueueHead();
        }
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

        //we ensure all components exist at first to prevent concurrency issues as ensuring the components exist
        //takes a write lock on those nodes
        ensureReferencedComponentsExist(version);

        //we'll need to fix precedence for any versions we create later
        List<VersionId> versionsCreated = ensureReferencedVersionsExist(version, excludeRequestedDependencies);

        l.info("Saving [{}] resolved dependencies for version [{}], component [{}]", version.getResolvedDependencies().size(), version.getVersion(), version.getComponent());
        //Add resolved dependency links
        for (Dependency dep : version.getResolvedDependencies()) {
            versionDao.createResolvedDependency(version, dep.getVersion(), dep.getProfiles());
            if (!excludeRequestedDependencies) {
                saveRequestedDependencies(dep.getVersion());
            }
        }

        if (!excludeRequestedDependencies) {
            l.info("Saving [{}] requested dependencies for version [{}], component [{}]", version.getRequestedDependencies().size(), version.getVersion(), version.getComponent());
            saveRequestedDependencies(version);
        }

        l.info("Fixing precedence for [{}] created versions for version [{}], component [{}]", versionsCreated.size(), version.getVersion(), version.getComponent());
        precedenceFixer.fixPrecedence(versionsCreated);

        l.info("Finished saving [{}]:[{}]", version.getComponent(), version.getVersion());
        return getVersion(version.getComponent(), version.getVersion()).orElseThrow(() -> new RuntimeException("Could not find a version [" + version.getVersion() + "] for component [" + version.getComponent() + "] after creating it"));
    }

    //This method implicitly guarantees that the returned 'actually created' version ids are sorted by component and version (in maven ordering)
    private List<VersionId> ensureReferencedVersionsExist(Version version, boolean excludeRequestedDependencies) {
        Set<VersionId> realVersionsToEnsureCreated = new HashSet<>();
        realVersionsToEnsureCreated.add(versionPurifier.requireRealVersion(version.toId()));
        realVersionsToEnsureCreated.addAll(
                version.getResolvedDependencies().stream()
                        //we don't just use isRealVersion to filter because if we have a resolved dependency that's not
                        //either latest or real we want to fail
                        .filter(dep -> !versionPurifier.isLatestVersion(dep.getVersion().getVersion()))
                        .map(dep -> versionPurifier.requireRealVersion(dep.getVersion().toId()))
                        .collect(Collectors.toSet())
        );
        if (!excludeRequestedDependencies) {
            realVersionsToEnsureCreated.addAll(
                    version.getResolvedDependencies().stream()
                            .flatMap(dep -> dep.getVersion().getRequestedDependencies().stream())
                            .filter(dep -> versionPurifier.isRealVersion(dep.getVersion().getVersion()))
                            .map(dep -> dep.getVersion().toId())
                            .collect(Collectors.toSet())
            );
            realVersionsToEnsureCreated.addAll(
                    version.getRequestedDependencies().stream()
                            .filter(dep -> versionPurifier.isRealVersion(dep.getVersion().getVersion()))
                            .map(dep -> dep.getVersion().toId())
                            .collect(Collectors.toSet())
            );
        }

        Set<VersionId> shadowVersionsToEnsureCreated = new HashSet<>();
        shadowVersionsToEnsureCreated.addAll(
                version.getResolvedDependencies().stream()
                        .filter(dep -> versionPurifier.isLatestVersion(dep.getVersion().getVersion()))
                        .map(dep -> dep.getVersion().toId())
                        .collect(Collectors.toSet())
        );

        if (!excludeRequestedDependencies) {
            shadowVersionsToEnsureCreated.addAll(
                    version.getResolvedDependencies().stream()
                            .flatMap(dep -> dep.getVersion().getRequestedDependencies().stream())
                            .filter(dep -> !versionPurifier.isRealVersion(dep.getVersion().getVersion()))
                            .map(dep -> dep.getVersion().toId())
                            .map(versionPurifier::purifyVersion)
                            .collect(Collectors.toSet())
            );

            shadowVersionsToEnsureCreated.addAll(
                    version.getRequestedDependencies().stream()
                            .filter(dep -> !versionPurifier.isRealVersion(dep.getVersion().getVersion()))
                            .map(dep -> dep.getVersion().toId())
                            .map(versionPurifier::purifyVersion)
                            .collect(Collectors.toSet())
            );
        }

        //Regardless of versioning scheme, we're doing maven ordering.  Maven versions need to be done in maven ordering
        //since when we fix precedence we want to fix precedence in maven order to prevent deadlocks.  Non-maven version
        // precedence is based on create
        //timestamp, which means that if we were ever fixing precedence for multiple non-maven versions for the same component
        //at the same time, it means we created two non-maven versions at the same time and we don't have an ordering
        //guarantee between them so we can insert them in the precedence list in an arbitrary order.  We choose maven
        //ordering to ensure we create them in a deterministic order so as to prevent deadlocks
        Comparator<VersionId> versionComparator = (o1, o2) -> {
            if (o1.getComponent().equals(o2.getComponent())) {
                return new ComparableVersion(o1.getVersion()).compareTo(new ComparableVersion(o2.getVersion()));
            } else {
                return o1.getComponent().compareTo(o2.getComponent());
            }

        };

        shadowVersionsToEnsureCreated
                .stream()
                .sorted(versionComparator)
                .forEach(v -> versionDao.createShadowVersion(v.getComponent(), v.getVersion()));

        return realVersionsToEnsureCreated
                .stream()
                .sorted(versionComparator)
                .filter(v -> versionDao.createNewVersion(v.getComponent(), v.getVersion()))
                .collect(Collectors.toList());
    }

    private void ensureReferencedComponentsExist(Version version) {
        Set<String> components = new HashSet<>(); //ensure we only ensure created for a component once
        components.add(version.getComponent());
        for (Dependency dep : version.getResolvedDependencies()) {
            components.add(dep.getVersion().getComponent());
            components.addAll(dep.getVersion().getRequestedDependencies().stream().map(d -> d.getVersion().getComponent()).collect(Collectors.toList()));
        }
        components.addAll(version.getRequestedDependencies().stream().map(d -> d.getVersion().getComponent()).collect(Collectors.toList()));
        ensureCreatedInAlphabeticalOrder(components);
    }

    private void ensureCreatedInAlphabeticalOrder(Set<String> components) {
        List<String> sortedComponents = Lists.newArrayList(components);
        Collections.sort(sortedComponents);
        sortedComponents.forEach(componentDao::ensureCreated);
    }

    private void saveRequestedDependencies(Version parent) {
        for (Dependency dep : parent.getRequestedDependencies()) {
            versionDao.createRequestedDependency(
                    parent,
                    dep.getVersion(),
                    versionPurifier.purifyVersion(dep.getVersion()).getVersion(),
                    dep.getProfiles()
            );
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
