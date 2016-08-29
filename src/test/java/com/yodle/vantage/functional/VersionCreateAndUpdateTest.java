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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.service.QueueService;
import com.yodle.vantage.functional.config.VantageFunctionalTest;

public class VersionCreateAndUpdateTest extends VantageFunctionalTest {
    @Autowired private QueueService queueService;


    @Test
    public void createVersion() {
        Version v = createVersion("component", "version");

        //Ensure component got implicitly created
        VantageComponent component = vantageApi.getComponent(v.getComponent());
        assertEquals(v.getComponent(), component.getName());
        assertEquals(v.getVersion(), component.getMostRecentVersion());

        validateVersion(v);
    }

    @Test
    public void createVersionWhenComponentAlreadyExists() {
        VantageComponent c = new VantageComponent("component", "description");
        vantageApi.createOrUpdateComponent("component", c);

        Version v = createVersion("component", "version");

        //ensure component still is in expected state
        VantageComponent component = vantageApi.getComponent(c.getName());
        assertEquals(c.getName(), component.getName());
        assertEquals(v.getVersion(), component.getMostRecentVersion());
        assertEquals(c.getDescription(), component.getDescription());

        validateVersion(v);
    }

    @Test
    public void createVersionWithResolvedDependencies() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "dependencyversion"));
        Version v = createVersion("component", "version", new HashSet<>(), savedDependencies);

        validateVersion(v);
    }

    @Test
    public void createVersionWithRequestedDependencies() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "dependencyversion"));

        Version v = createVersion("component", "version", savedDependencies);

        validateVersion(v);
    }

    @Test
    public void createVersionWithRequestedDependencyWithDynamicVersion() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "1.0+"));

        Version v = createVersion("component", "version", savedDependencies);
        v.getRequestedDependencies().stream().forEach(d -> d.getVersion().setVersion("unknown"));

        validateVersion(v);
    }

    @Test
    public void createVersionWithRequestedDependencyWithUndefinedVersion() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "undefined"));

        Version v = createVersion("component", "version", savedDependencies);
        v.getRequestedDependencies().stream().forEach(d -> d.getVersion().setVersion("unknown"));

        validateVersion(v);
    }

    @Test
    public void createVersionWithRequestedDependencyWithLatestVersion() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "latest"));

        Version v = createVersion("component", "version", savedDependencies);

        validateVersion(v);
    }

    @Test
    public void createVersionWithResolvedDependencyWithLatestVersion() {
        Set<Dependency> savedDependencies = Sets.newHashSet(createDependency("dependency component", "latest"));

        Version v = createVersion("component", "version", new HashSet<>(), savedDependencies);

        validateVersion(v);
    }

    @Test
    public void createVersionWithResolvedDependenciesWithRequestedDependencies() {
        Dependency resolvedDependency = createDependency("dependency component", "dependencyversion");
        Set<Dependency> savedDependencies = Sets.newHashSet(resolvedDependency);
        savedDependencies.stream().forEach(d -> d.getVersion().setRequestedDependencies(Sets.newHashSet(createDependency("level 2 dependency", "1.0"))));

        Version v = createVersion("component", "version", new HashSet<>(), savedDependencies);

        validateVersion(v);
    }

    @Test
    public void updateVersionWithoutDepsToHaveDeps() {
        Version v = createVersion("component", "version");
        validateVersion(v);

        Dependency resolvedDependency = createDependency("dependency component", "dependencyversion");
        v = createVersion("component", "version", new HashSet<>(), Sets.newHashSet(resolvedDependency));

        validateVersion(v);
    }

    @Test
    public void updateVersionWithDepsToHaveMoreDeps() {
        Dependency resolvedDependency = createDependency("dependency component", "dependencyversion");
        Version v = createVersion("component", "version", new HashSet<>(), Sets.newHashSet());
        validateVersion(v);

        //add a profile to an existing dep
        resolvedDependency.getProfiles().add("new profile");
        //also add an entirely new dep
        Dependency resolvedDependency2 = createDependency("dependency component2", "dependencyversion2");

        v = createVersion("component", "version", new HashSet<>(), Sets.newHashSet(resolvedDependency, resolvedDependency2));

        validateVersion(v);
    }

    @Test
    public void versionsWithMavenVersionNumbersOrderedByMavenOrdering() {
        Version v20 = createVersion("component", "2.0");
        Version v10 = createVersion("component", "1.0");
        Version v15 = createVersion("component", "1.5");
        v10.setActive(false);
        v15.setActive(false);

        List<Version> versions = vantageApi.getVersionsOfComponent("component");

        assertEquals(Lists.newArrayList(v20.toId(), v15.toId(), v10.toId()), versions.stream().map(Version::toId).collect(Collectors.toList()));
    }

    @Test
    public void versionsWithNonMavenVersionsOrderdByCreated() {
        Version v1 = createVersion("component", "abc123");
        Version v2 = createVersion("component", "def456");
        Version v3 = createVersion("component", "123fed");
        v1.setActive(false);
        v2.setActive(false);

        List<Version> versions = vantageApi.getVersionsOfComponent("component");

        assertEquals(Lists.newArrayList(v3.toId(), v2.toId(), v1.toId()), versions.stream().map(Version::toId).collect(Collectors.toList()));
    }

    @Test
    public void dryRunDoesNotPersistNewVersionOrComponent() {
        Version toSave = new Version("component", "version");
        toSave.setRequestedDependencies(Sets.newHashSet(createDependency("dep", "depversion")));
        toSave.setResolvedDependencies(Sets.newHashSet(createDependency("dep", "depversion")));

        Version v = vantageApi.createOrUpdateVersion("component", "version", true, toSave);

        //We expect to not see any requested dependencies returned as dry-runs don't save them for performance reasons
        Version expected = new Version("component", "version");
        expected.setResolvedDependencies(Sets.newHashSet(createDependency("dep", "depversion")));

        assertTrue("There should be no components created since the create was a dry run", vantageApi.getAllComponents().isEmpty());

        validateVersion(expected, v);
    }

    @Test
    public void dryRunCreateReturnsIssues() {
        Dependency resolvedDependency = createDependency("dep component", "depversion");
        createVersion(resolvedDependency.getVersion().getComponent(), resolvedDependency.getVersion().getVersion());
        Version version = resolvedDependency.getVersion();

        Issue issue = createIssue(version);

        Version expected = new Version("component", "version");
        expected.setResolvedDependencies(Sets.newHashSet(createDependency(resolvedDependency.getVersion().getComponent(), resolvedDependency.getVersion().getVersion())));
        Version v = vantageApi.createOrUpdateVersion("component", "version", true, expected);

        assertTrue("Version should have transitive issue from its dependency", v.getTransitiveIssues().contains(issue));
        assertTrue("Dependency should have direct issue", Iterables.getOnlyElement(v.getResolvedDependencies()).getVersion().getDirectIssues().contains(issue));
    }


}
