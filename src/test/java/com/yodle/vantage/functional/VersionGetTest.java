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
package com.yodle.vantage.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import feign.FeignException;

import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.domain.VersionId;
import com.yodle.vantage.functional.config.VantageFunctionalTest;

public class VersionGetTest extends VantageFunctionalTest {

    @Test
    public void getVersionsForNonexistentComponent() {
        try {
            vantageApi.getVersionsOfComponent("nonexistent component");
            fail("Should have thrown a 404");
        } catch(FeignException e) {
            assertEquals(404, e.status());
        }
    }

    @Test
    public void getVersionsForComponentWithNoVersions() {
        vantageApi.createOrUpdateComponent("component", new VantageComponent("component", ""));
        assertTrue("Newly created component shouldn't have any versions", vantageApi.getVersionsOfComponent("component").isEmpty());
    }

    @Test
    public void getVersionsForComponentWithVersion() {
        Version v = createVersion("component", "version");
        assertEquals(Lists.newArrayList(v), vantageApi.getVersionsOfComponent(v.getComponent()));
    }

    @Test
    public void getVersionsHasActiveFlagSetProperly() {
        Version v1 = createVersion("component", "version1");
        Version v2 = createVersion("component", "version2");
        List<Version> versions = vantageApi.getVersionsOfComponent("component");

        assertEquals(Lists.newArrayList(v2.toId(), v1.toId()), versions.stream().map(Version::toId).collect(Collectors.toList()));
        assertTrue("v2 should be active as its the most recent version", versions.get(0).isActive());
        assertFalse("v should not be active as its not the most recent version or depended on", versions.get(1).isActive());
    }

    @Test
    public void getVersionsForVersionWithDependencies() {
        Dependency dependency = createDependency("dep", "depversion");
        Version v = createVersion("component", "version", Sets.newHashSet(dependency), Sets.newHashSet(createDependency("dep", "depversion")));
        Issue transitiveIssue = createIssue(dependency.getVersion());
        Issue directIssue = createIssue(v);

        Version returnedVersion = vantageApi.getVersionsOfComponent(v.getComponent()).get(0);

        assertEquals(Sets.newHashSet(directIssue), new HashSet<>(returnedVersion.getDirectIssues()));
        assertEquals(Sets.newHashSet(transitiveIssue), new HashSet<>(returnedVersion.getTransitiveIssues()));
    }

    //The following 4 tests may seem like overkill, but the queries powering issue affects are complicated and have
    //already required several iterations to get right, so we're being overly cautious.
    @Test
    public void getVersionsForComponentWithDirectIssue() {
        Version v1 = createVersion("component", "v1");
        Version v2 = createVersion("component", "v2");
        Version v3 = createVersion("component", "v3");
        Version v4 = createVersion("component", "v4");
        Version v5 = createVersion("component", "v5");

        Issue issue = createIssue(v2, v4);

        Map<VersionId, Version> versions = vantageApi.getVersionsOfComponent("component").stream().collect(Collectors.toMap(Version::toId, v -> v));
        assertEquals(Sets.newHashSet(), versions.get(v1.toId()).getDirectIssues());
        assertEquals(Sets.newHashSet(issue), versions.get(v2.toId()).getDirectIssues());
        assertEquals(Sets.newHashSet(issue), versions.get(v3.toId()).getDirectIssues());
        assertEquals(Sets.newHashSet(), versions.get(v4.toId()).getDirectIssues());
        assertEquals(Sets.newHashSet(), versions.get(v5.toId()).getDirectIssues());
    }

    @Test
    public void getVersionsForComponentWithTransitiveIssue() {
        Version d1 = createVersion("dep", "v1");
        Version d2 = createVersion("dep", "v2");
        Version d3 = createVersion("dep", "v3");
        Version d4 = createVersion("dep", "v4");
        Version d5 = createVersion("dep", "v5");

        Version v1 = createVersion("component", "v1", new HashSet<>(), Sets.newHashSet(createDependency(d1.getComponent(), d1.getVersion())));
        Version v2 = createVersion("component", "v2", new HashSet<>(), Sets.newHashSet(createDependency(d2.getComponent(), d2.getVersion())));
        Version v3 = createVersion("component", "v3", new HashSet<>(), Sets.newHashSet(createDependency(d3.getComponent(), d3.getVersion())));
        Version v4 = createVersion("component", "v4", new HashSet<>(), Sets.newHashSet(createDependency(d4.getComponent(), d4.getVersion())));
        Version v5 = createVersion("component", "v5", new HashSet<>(), Sets.newHashSet(createDependency(d5.getComponent(), d5.getVersion())));

        Issue issue = createIssue(d2, d4);

        Map<VersionId, Version> versions = vantageApi.getVersionsOfComponent("component").stream().collect(Collectors.toMap(VersionId::toId, v -> v));
        assertEquals(Sets.newHashSet(), versions.get(v1.toId()).getTransitiveIssues());
        assertEquals(Sets.newHashSet(issue), versions.get(v2.toId()).getTransitiveIssues());
        assertEquals(Sets.newHashSet(issue), versions.get(v3.toId()).getTransitiveIssues());
        assertEquals(Sets.newHashSet(), versions.get(v4.toId()).getTransitiveIssues());
        assertEquals(Sets.newHashSet(), versions.get(v5.toId()).getTransitiveIssues());
    }

    @Test
    public void getVersionForComponentWithDirectIssue() {
        Version v1 = createVersion("component", "v1");
        Version v2 = createVersion("component", "v2");
        Version v3 = createVersion("component", "v3");
        Version v4 = createVersion("component", "v4");
        Version v5 = createVersion("component", "v5");

        Issue issue = createIssue(v2, v4);

        Version v1Returned = vantageApi.getVersion("component", "v1");
        Version v2Returned = vantageApi.getVersion("component", "v2");
        Version v3Returned = vantageApi.getVersion("component", "v3");
        Version v4Returned = vantageApi.getVersion("component", "v4");
        Version v5Returned = vantageApi.getVersion("component", "v5");

        assertEquals(Sets.newHashSet(), v1Returned.getDirectIssues());
        assertEquals(Sets.newHashSet(issue), v2Returned.getDirectIssues());
        assertEquals(Sets.newHashSet(issue), v3Returned.getDirectIssues());
        assertEquals(Sets.newHashSet(), v4Returned.getDirectIssues());
        assertEquals(Sets.newHashSet(), v5Returned.getDirectIssues());
    }

    @Test
    public void getVersionForComponentWithTransitiveIssue() {
        Version d1 = createVersion("dep", "v1");
        Version d2 = createVersion("dep", "v2");
        Version d3 = createVersion("dep", "v3");
        Version d4 = createVersion("dep", "v4");
        Version d5 = createVersion("dep", "v5");

        Version v1 = createVersion("component", "v1", new HashSet<>(), Sets.newHashSet(createDependency(d1.getComponent(), d1.getVersion())));
        Version v2 = createVersion("component", "v2", new HashSet<>(), Sets.newHashSet(createDependency(d2.getComponent(), d2.getVersion())));
        Version v3 = createVersion("component", "v3", new HashSet<>(), Sets.newHashSet(createDependency(d3.getComponent(), d3.getVersion())));
        Version v4 = createVersion("component", "v4", new HashSet<>(), Sets.newHashSet(createDependency(d4.getComponent(), d4.getVersion())));
        Version v5 = createVersion("component", "v5", new HashSet<>(), Sets.newHashSet(createDependency(d5.getComponent(), d5.getVersion())));

        Issue issue = createIssue(d2, d4);

        Version v1Returned = vantageApi.getVersion("component", "v1");
        Version v2Returned = vantageApi.getVersion("component", "v2");
        Version v3Returned = vantageApi.getVersion("component", "v3");
        Version v4Returned = vantageApi.getVersion("component", "v4");
        Version v5Returned = vantageApi.getVersion("component", "v5");


        validateTransitiveIssue(Sets.newHashSet(), v1Returned);
        validateTransitiveIssue(Sets.newHashSet(issue), v2Returned);
        validateTransitiveIssue(Sets.newHashSet(issue), v3Returned);
        validateTransitiveIssue(Sets.newHashSet(), v4Returned);
        validateTransitiveIssue(Sets.newHashSet(), v5Returned);
    }

    private void validateTransitiveIssue(Set<Issue> expectedIssues, Version v) {
        assertEquals(expectedIssues, v.getTransitiveIssues());
        assertEquals(expectedIssues,  v.getResolvedDependencies().stream().flatMap(d->d.getVersion().getDirectIssues().stream()).collect(Collectors.toSet()));
    }

    @Test
    public void getVersionForNonexistentComponent() {
        try {
            vantageApi.getVersion("component", "version");
            fail("Should have thrown 404");
        } catch (FeignException e) {
            assertEquals(404, e.status());
        }
    }

    @Test
    public void getNonexistentVersionForExistentComponent() {
        vantageApi.createOrUpdateComponent("component", new VantageComponent("component", ""));
        try {
            vantageApi.getVersion("component", "version");
            fail("Should have thrown 404");
        } catch (FeignException e) {
            assertEquals(404, e.status());
        }
    }

    @Test
    public void getVersionIsActiveBecauseItsMostRecent() {
        Version v1 = createVersion("component", "version1");
        Version returnedVersion1 = vantageApi.getVersion(v1.getComponent(), v1.getVersion());
        assertTrue("v1 should be active as it's the most recent (and only) version", returnedVersion1.isActive());

        Version v2 = createVersion("component", "version2");
        Version returnedVersion2 = vantageApi.getVersion(v2.getComponent(), v2.getVersion());
        returnedVersion1 = vantageApi.getVersion(v1.getComponent(), v1.getVersion());

        assertTrue("v2 should now be active as it's now the most recent version", returnedVersion2.isActive());
        assertFalse("v1 is no longer active as it's no longer the most recent version", returnedVersion1.isActive());
    }

    @Test
    public void getVersionIsActiveBecauseItsDependedOn() {
        Version v1 = createVersion("component", "version1");
        Version v2 = createVersion("component", "version2");

        createVersion("component parent", "parentversion1", new HashSet<>(), Sets.newHashSet(createDependency(v1.getComponent(), v1.getVersion())));
        assertTrue("v1 should be active as its depended on by an active version", vantageApi.getVersion(v1.getComponent(), v1.getVersion()).isActive());

        createVersion("component parent", "parentversion2", new HashSet<>(), Sets.newHashSet(createDependency(v2.getComponent(), v2.getVersion())));
        assertFalse("v1 should no longer be active as its dependent is no longer active", vantageApi.getVersion(v1.getComponent(), v1.getVersion()).isActive());
    }

    @Test
    public void getVersionForVersionWithDependenciesAndIssues() {
        Dependency dependency = createDependency("dep", "depversion");
        Version v = createVersion("component", "version", Sets.newHashSet(dependency), Sets.newHashSet(createDependency("dep", "depversion")));
        Issue transitiveIssue = createIssue(dependency.getVersion());
        Issue directIssue = createIssue(v);

        Version returnedVersion = vantageApi.getVersion(v.getComponent(), v.getVersion());

        assertEquals(Sets.newHashSet(directIssue), new HashSet<>(returnedVersion.getDirectIssues()));
        assertEquals(Sets.newHashSet(transitiveIssue), new HashSet<>(returnedVersion.getTransitiveIssues()));
        assertEquals(Sets.newHashSet(transitiveIssue), Iterables.getOnlyElement(returnedVersion.getResolvedDependencies()).getVersion().getDirectIssues());
    }

    @Test
    public void getVersionForVersionWithDependents() {
        Dependency dependency = createDependency("dep", "depversion");
        Version v = createVersion("component", "version", Sets.newHashSet(dependency), Sets.newHashSet(createDependency("dep", "depversion")));

        Version returnedVersion = validateVersion(dependency.getVersion());

        assertEquals(Sets.newHashSet(sanitizeDependency(new Dependency(v, dependency.getProfiles()))), returnedVersion.getDependents());
    }
}
