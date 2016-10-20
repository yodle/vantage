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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yodle.vantage.component.dao.ComponentDao;
import com.yodle.vantage.component.dao.IssueDao;
import com.yodle.vantage.component.dao.QueueDao;
import com.yodle.vantage.component.dao.VersionDao;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.IssueLevel;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.domain.VersionId;

@RunWith(MockitoJUnitRunner.class)
public class ComponentServiceTest {
    private static final String COMPONENT = "component";
    private static final String VERSION = "version";

    @InjectMocks private ComponentService componentService;
    @Mock private ComponentDao componentDao;
    @Mock private QueueDao queueDao;
    @Mock private IssueDao issueDao;
    @Mock private VersionDao versionDao;
    @Mock private PrecedenceFixer precedenceFixer;
    @Mock(answer = Answers.CALLS_REAL_METHODS) private VersionPurifier versionPurifier;

    /**
     * createOrUpdateVersion
     */

    @Test
    public void givenVersionHasNoDependencies_createOrUpdateVersion_justCreatesVersion() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));

        componentService.createOrUpdateVersion(new Version(COMPONENT, VERSION));

        verify(componentDao).ensureCreated(COMPONENT);
        verify(versionDao).createNewVersion(COMPONENT, VERSION);
    }

    @Test
    public void givenVersionHasResolvedDependencies_createOrUpdateVersion_createsResolvedDependencyVersions() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));

        Version version = new Version(COMPONENT, VERSION);
        Dependency dependency = createDependency();
        version.setResolvedDependencies(Sets.newHashSet(dependency));

        componentService.createOrUpdateVersion(version);

        verify(componentDao).ensureCreated(dependency.getVersion().getComponent());
        verify(versionDao).createResolvedDependency(version, dependency.getVersion(), dependency.getProfiles());
    }

    @Test
    public void givenVersionHasRequestedDependencies_createOrUpdateVersion_createsRequestedDependencyVersions() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));

        Version version = new Version(COMPONENT, VERSION);
        Dependency dependency = createDependency();
        version.setRequestedDependencies(Sets.newHashSet(dependency));

        componentService.createOrUpdateVersion(version);

        verify(componentDao).ensureCreated(dependency.getVersion().getComponent());
        verify(versionDao).createRequestedDependency(version, dependency.getVersion(), versionPurifier.purifyVersion(dependency.getVersion()).getVersion(), dependency.getProfiles());
    }


    @Test
    public void givenVersionHasResolvedDependenciesWithRequestedDependencies_createOrUpdateVersion_createsResolvedDependencysRequestedDependencies() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));

        Version version = new Version(COMPONENT, VERSION);
        Dependency dependency = createDependency();
        Dependency requestedDependency = createDependency();
        dependency.getVersion().setRequestedDependencies(Sets.newHashSet(requestedDependency));
        version.setResolvedDependencies(Sets.newHashSet(dependency));

        componentService.createOrUpdateVersion(version);

        verify(componentDao).ensureCreated(requestedDependency.getVersion().getComponent());
        verify(versionDao).createRequestedDependency(
                dependency.getVersion(),
                requestedDependency.getVersion(),
                versionPurifier.purifyVersion(requestedDependency.getVersion()).getVersion(),
                requestedDependency.getProfiles()
        );
    }

    @Test
    public void givenVersionsActuallyCreated_createOrUpdateVersion_fixesPrecedenceForThoseVersions() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));

        Version version = new Version(COMPONENT, VERSION);
        Dependency createdResolvedDependency = createDependency("DependencyA");
        Dependency existingResolvedDependency = createDependency();
        Dependency createdDependencyRequestedDependency = createDependency("DependencyB");
        Dependency existingDependencyRequestedDependency = createDependency();
        Dependency createdRequestedDependency = createDependency("DependencyC");
        Dependency existingRequestedDependency = createDependency();
        createdResolvedDependency.getVersion().setRequestedDependencies(Sets.newHashSet(createdDependencyRequestedDependency, existingDependencyRequestedDependency));
        version.setResolvedDependencies(Sets.newHashSet(createdResolvedDependency, existingResolvedDependency));
        version.setRequestedDependencies(Sets.newHashSet(createdRequestedDependency, existingRequestedDependency));

        when(versionDao.createNewVersion(COMPONENT, VERSION)).thenReturn(true);
        when(versionDao.createNewVersion(
                createdRequestedDependency.getVersion().getComponent(),
                createdRequestedDependency.getVersion().getVersion()

        )).thenReturn(true);
        when(versionDao.createNewVersion(createdDependencyRequestedDependency.getVersion().getComponent(), createdDependencyRequestedDependency.getVersion().getVersion())).thenReturn(true);
        when(versionDao.createNewVersion(createdResolvedDependency.getVersion().getComponent(), createdResolvedDependency.getVersion().getVersion())).thenReturn(true);

        componentService.createOrUpdateVersion(version);

        verify(precedenceFixer).fixPrecedence(Lists.newArrayList(
                createdResolvedDependency.getVersion().toId(),
                createdDependencyRequestedDependency.getVersion().toId(),
                createdRequestedDependency.getVersion().toId(),
                version.toId()
        ));
    }

    /**
     * getVersions
     */
    @Test
    public void givenComponentDoesNotExist_getVersions_returnsEmptyOptional() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.empty());

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertFalse("Return value should be empty optional because component does not exist", versions.isPresent());
    }

    @Test
    public void givenComponentExistsButHasNoVersions_getVersions_returnsOptionalWithEmptyList() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.of(new VantageComponent(COMPONENT, "")));
        when(versionDao.getVersions(COMPONENT)).thenReturn(new ArrayList<>());

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertTrue("Return value should be present because component exists", versions.isPresent());
        assertTrue("Returned list should be empty because the component has no versions", versions.get().isEmpty());
    }

    @Test
    public void givenComponentExistsAndHasVersions_getVersions_marksActiveVersionsAsActive() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.of(new VantageComponent(COMPONENT, "")));
        when(versionDao.getVersions(COMPONENT)).thenReturn(Lists.newArrayList(VERSION + "1", VERSION + "2"));
        when(versionDao.getActiveVersions(COMPONENT)).thenReturn(Lists.newArrayList(VERSION + 1));

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertTrue("Return value should be present because component exists", versions.isPresent());
        assertEquals(2, versions.get().size());

        Map<String, Version> versionByVersionString = Maps.uniqueIndex(versions.get(), Version::getVersion);
        assertTrue("Version 1 should be active", versionByVersionString.get(VERSION + "1").isActive());
        assertFalse("Version 2 should not be active", versionByVersionString.get(VERSION + "2").isActive());
    }

    @Test
    public void givenComponentExistsAndHasVersions_getVersions_attachesIssuesToVersions() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.of(new VantageComponent(COMPONENT, "")));
        when(versionDao.getVersions(COMPONENT)).thenReturn(Lists.newArrayList(VERSION ));
        Set<Issue> directIssues = Sets.newHashSet(createIssue(COMPONENT, VERSION));
        when(issueDao.getIssuesDirectlyAffectingVersions(COMPONENT)).thenReturn(
                ImmutableMap.of(
                        VERSION, directIssues
                )
        );

        Set<Issue> transitiveIssues = Sets.newHashSet(createIssue(COMPONENT, VERSION));
        when(issueDao.getIssuesTransitivelyAffectingVersions(COMPONENT)).thenReturn(
                ImmutableMap.of(
                        VERSION, transitiveIssues
        ));

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertTrue("Return value should be present because component exists", versions.isPresent());
        assertEquals(1, versions.get().size());

        assertEquals(directIssues, versions.get().get(0).getDirectIssues());
        assertEquals(transitiveIssues, versions.get().get(0).getTransitiveIssues());
    }

    @Test
    public void givenComponentExistsAndHasVersionsWithMavenVersionScheme_getVersions_sortsIssuesByMavenScheme() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.of(new VantageComponent(COMPONENT, "")));
        when(versionDao.getVersions(COMPONENT)).thenReturn(Lists.newArrayList("1.0.0", "2.0.0", "1.0.1", "1.5.1"));
        List<String> expectedVersionOrdering = Lists.newArrayList("2.0.0", "1.5.1", "1.0.1", "1.0.0");

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertTrue("Return value should be present because component exists", versions.isPresent());
        assertEquals(expectedVersionOrdering, versions.get().stream().map(Version::getVersion).collect(Collectors.toList()));
    }

    @Test
    public void givenComponentExistsAndHasVersionsWithMavenVersionScheme_getVersions_doesNotSortVersions() {
        when(componentDao.getComponent(COMPONENT)).thenReturn(Optional.of(new VantageComponent(COMPONENT, "")));
        List<String> expectedVersionOrdering = Lists.newArrayList("abe46", "de351");
        when(versionDao.getVersions(COMPONENT)).thenReturn(expectedVersionOrdering);

        Optional<List<Version>> versions = componentService.getVersions(COMPONENT);

        assertTrue("Return value should be present because component exists", versions.isPresent());
        assertEquals(expectedVersionOrdering, versions.get().stream().map(Version::getVersion).collect(Collectors.toList()));
    }

    /**
     * getVersion
     */
    @Test
    public void givenVersionDoesNotExist_getVersion_returnsEmpty() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.empty());
        Optional<Version> version = componentService.getVersion(COMPONENT, VERSION);
        assertFalse("Version should be empty because it doesn't exist", version.isPresent());
    }

    @Test
    public void givenVersionExist_getVersion_returnsDirectIssues() {
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(new Version(COMPONENT, VERSION)));
        List<Issue> issues = Lists.newArrayList(
                createIssue(COMPONENT, VERSION),
                createIssue(COMPONENT, VERSION)
        );
        when(issueDao.getIssuesDirectlyAffectingVersion(COMPONENT, VERSION)).thenReturn(issues);

        Optional<Version> version = componentService.getVersion(COMPONENT, VERSION);

        assertTrue("Version should be present", version.isPresent());
        assertEquals(new HashSet<>(issues), version.get().getDirectIssues());
    }

    @Test
    public void givenVersionHasDependenciesWithIssues_getVersion_attachesIssuesToThoseDependencies() {
        Version daoVersion = new Version(COMPONENT, VERSION);
        Dependency dep1 = createDependency();
        Dependency dep2 = createDependency();
        daoVersion.setResolvedDependencies(Sets.newHashSet(
                dep1,
                dep2
        ));
        when(versionDao.getVersion(COMPONENT, VERSION)).thenReturn(Optional.of(daoVersion));

        Set<Issue> dep1Issues = Sets.newHashSet(createIssue(dep1.getVersion()));
        Set<Issue> dep2Issues = Sets.newHashSet(createIssue(dep2.getVersion()), createIssue(dep2.getVersion())
        );
        when(issueDao.getIssuesByDependenciesOf(COMPONENT, VERSION)).thenReturn(
                ImmutableMap.of(
                        dep1.getVersion(), dep1Issues,
                        dep2.getVersion(), dep2Issues
        ));

        Optional<Version> version = componentService.getVersion(COMPONENT, VERSION);

        assertTrue("Version should be present", version.isPresent());
        Map<VersionId, Dependency> dependenciesByVersion = Maps.uniqueIndex(version.get().getResolvedDependencies(), d -> d.getVersion().toId());

        assertEquals(dep1Issues, dependenciesByVersion.get(dep1.getVersion().toId()).getVersion().getDirectIssues());
        assertEquals(dep2Issues, dependenciesByVersion.get(dep2.getVersion().toId()).getVersion().getDirectIssues());
        Set<Issue> transitiveIssues = new HashSet<>(dep1Issues);
        transitiveIssues.addAll(dep2Issues);
        assertEquals(transitiveIssues, version.get().getTransitiveIssues());
    }

    private Dependency createDependency() {
        return createDependency("Dependency");
    }

    private Dependency createDependency(String componentPrefix) {
        Version version = new Version(
                componentPrefix + "-" + RandomStringUtils.randomAlphanumeric(10),
                "Version-" + RandomStringUtils.randomAlphanumeric(10)
        );
        return new Dependency(version, Sets.newHashSet("compile"));
    }

    private Issue createIssue(String component, String version) {
        return createIssue(new Version(component, version));
    }

    private Issue createIssue(Version version) {
        Issue issue = new Issue();
        issue.setId(RandomStringUtils.randomAlphanumeric(10));
        issue.setAffectsVersion(version);
        issue.setLevel(IssueLevel.CRITICAL);
        issue.setMessage("message");
        return issue;
    }
}