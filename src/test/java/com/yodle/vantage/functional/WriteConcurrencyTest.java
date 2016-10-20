package com.yodle.vantage.functional;


import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.service.ComponentService;
import com.yodle.vantage.functional.config.VantageFunctionalTest;

/**
 * These tests verify that we can concurrently save multiple versions at the same time without encountering deadlocks.  Deadlocks
 * can occur because two different versions may create the same dependency components/versions in the same transaction, but in
 * a different order.  Vantage originally handled this by single-threading all version creations.  This was fine for queued creates
 * but it imposes a scalability issue with synchronous dry-run creates.
 */
public class WriteConcurrencyTest extends VantageFunctionalTest {
    private static final int NUM_DEPENDENCIES = 100;
    @Autowired private ComponentService componentService;
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    private Set<Dependency> createNDependencies(int n) {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (int i = 0; i < n; ++i) {
            dependencies.add(createDependency("dep" + i, "depversion"));
        }

        return dependencies;
    }

    private LinkedHashSet<Dependency> createNReversedDependencies(int n) {
        return Sets.newLinkedHashSet(Lists.reverse(Lists.newArrayList(createNDependencies(n))));
    }

    @Test
    public void multipleSimultaneousCreatesWithMatchingResolvedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setResolvedDependencies(createNReversedDependencies(100));

        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependenciesAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setResolvedDependencies(createNReversedDependencies(100));

        componentService.createOrUpdateVersion(toSave1);
        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependencyComponentsAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setResolvedDependencies(createNReversedDependencies(100));

        createDependencyComponents(toSave1);

        createSimultaneously(toSave1, toSave2);
    }

    private void createDependencyComponents(Version toSave1) {
        toSave1.getResolvedDependencies().stream()
                .forEach(dep -> componentService.createOrUpdateComponent(new VantageComponent(dep.getVersion().getComponent(), "")));
    }

    @Test
    public void multipleSimultaneousCreatesWithMatchingResolvedAndRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setRequestedDependencies(createNReversedDependencies(100));


        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependenciesAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedAndRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setRequestedDependencies(createNReversedDependencies(100));

        componentService.createOrUpdateVersion(toSave1);
        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependencyComponentsAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedAndRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNDependencies(100));

        Version toSave2 = new Version("component2", "version");
        toSave2.setRequestedDependencies(createNReversedDependencies(100));

        createDependencyComponents(toSave1);
        createSimultaneously(toSave1, toSave2);
    }


    @Test
    public void multipleSimultaneousCreatesWithMatchingResolvedAndResolvedRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNReversedDependencies(NUM_DEPENDENCIES));

        Version toSave2 = new Version("component2", "version");
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        for (int i = 0; i < 100; ++i) {
            Dependency dependency = createDependency("other-dep" + i, "depversion");
            dependencies.add(dependency);
            dependency.getVersion().setRequestedDependencies(Sets.newHashSet(createDependency("dep" + i, "depversion")));
        }
        toSave2.setResolvedDependencies(dependencies);

        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependenciesAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedAndResolvedRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNReversedDependencies(NUM_DEPENDENCIES));

        Version toSave2 = new Version("component2", "version");
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        for (int i = 0; i < 100; ++i) {
            Dependency dependency = createDependency("other-dep" + i, "depversion");
            dependencies.add(dependency);
            dependency.getVersion().setRequestedDependencies(Sets.newHashSet(createDependency("dep" + i, "depversion")));
        }
        toSave2.setResolvedDependencies(dependencies);

        componentService.createOrUpdateVersion(toSave1);
        createSimultaneously(toSave1, toSave2);
    }

    @Test
    public void givenDependencyComponentsAlreadyExist_multipleSimultaneousCreatesWithMatchingResolvedAndResolvedRequestedDependenciesDontConflict() throws Exception {
        Version toSave1 = new Version("component1", "version");
        toSave1.setResolvedDependencies(createNReversedDependencies(NUM_DEPENDENCIES));

        Version toSave2 = new Version("component2", "version");
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        for (int i = 0; i < 100; ++i) {
            Dependency dependency = createDependency("other-dep" + i, "depversion");
            dependencies.add(dependency);
            dependency.getVersion().setRequestedDependencies(Sets.newHashSet(createDependency("dep" + i, "depversion")));
        }
        toSave2.setResolvedDependencies(dependencies);

        createDependencyComponents(toSave1);
        createSimultaneously(toSave1, toSave2);
    }


    private void createSimultaneously(Version toSave1, Version toSave2) throws InterruptedException, java.util.concurrent.ExecutionException {
        Future<Version> task1 = executorService.submit(() -> componentService.createOrUpdateVersion(toSave1));
        Future<Version> task2 = executorService.submit(() -> componentService.createOrUpdateVersion(toSave2));

        task1.get();
        task2.get();
    }
}
