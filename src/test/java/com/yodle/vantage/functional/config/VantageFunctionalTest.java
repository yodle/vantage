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
package com.yodle.vantage.functional.config;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.ServerControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.yodle.vantage.Application;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.IssueLevel;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.service.QueueService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, TestConfig.class})
@WebAppConfiguration
@IntegrationTest({"server.port=0"})
@ActiveProfiles(profiles = {"test","embedded"})
public abstract class VantageFunctionalTest {
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServerControls serverControls;
    @Autowired private QueueService queueService;

    @Value("${local.server.port}") private int port;
    protected VantageApi vantageApi;

    @PostConstruct
    public void initVantageApi() {
        vantageApi = Feign.builder()
                .encoder(new JacksonEncoder(objectMapper)).decoder(new JacksonDecoder(objectMapper))
                .logger(new Slf4jLogger()).logLevel(feign.Logger.Level.FULL)
                .target(VantageApi.class, "http://localhost:" + port);
    }

    @Before
    public void cleanupBetweenTests() {
        GraphDatabaseService db = serverControls.graph();
        Transaction tx = db.beginTx();
        db.execute("MATCH (n) DETACH DELETE n");
        tx.success();
        tx.close();
    }

    /**
     * Utility methods
     */

    protected Version validateVersion(Version expected) {
        Version actual = vantageApi.getVersion(expected.getComponent(), expected.getVersion());

        return validateVersion(expected, actual, true);
    }

    protected Version validateVersion(Version expected, Version actual) {
        return validateVersion(expected, actual, false);
    }

    protected Version validateVersion(Version expected, Version actual, boolean fetchActual) {
        assertEquals(expected.getComponent(), actual.getComponent());
        assertEquals(expected.getVersion(), actual.getVersion());
        compareResolvedDependencies(Sets.newHashSet(expected.getRequestedDependencies()), Sets.newHashSet(actual.getRequestedDependencies()));
        compareResolvedDependencies(expected.getResolvedDependencies(), actual.getResolvedDependencies());

        if (fetchActual) {
            expected.getResolvedDependencies().stream().forEach(d -> validateDependency(d, expected));
        } else {
            //validateDependency validates the dependency's dependents field, but we don't have that since we can't fetch
            //the dependency directly and that field isn't set on the nested dependency version
            Map<Version, Dependency> actualDependenciesByVersion = actual.getResolvedDependencies().stream().collect(Collectors.toMap(Dependency::getVersion, d -> d));
            expected.getResolvedDependencies().stream().forEach(d -> validateVersion(d.getVersion(), actualDependenciesByVersion.get(d.getVersion()).getVersion()));
        }

        return actual;
    }

    //when we're doing this comparison, the expected dependencies may have requested dependencies themselves left over
    //from the create command.  We don't receive second level dependencies when fetching the top level version though
    //so strip them out before doing the comparison
    protected void compareResolvedDependencies(Set<Dependency> expected, Set<Dependency> actual) {
        Set<Dependency> sanitized = expected.stream().map(this::sanitizeDependency).collect(Collectors.toSet());
        assertEquals(sanitized, actual);
    }

    protected Dependency sanitizeDependency(Dependency d) {
        Version version = d.getVersion();
        Version sanitizedVersion = new Version(version.getComponent(), version.getVersion());
        sanitizedVersion.setDirectIssues(version.getDirectIssues());
        return new Dependency(sanitizedVersion, d.getProfiles());
    }

    protected void validateDependency(Dependency expectedDependency, Version expectedParent) {
        Version actualDependencyVersion = validateVersion(expectedDependency.getVersion());
        assertEquals(Sets.newHashSet(new Dependency(expectedParent.toId().toVersion(), expectedDependency.getProfiles())), actualDependencyVersion.getDependents());
    }

    protected Version createVersion(String component, String version) {
        return createVersion(component, version, new HashSet<>());
    }

    protected Version createVersion(String component, String version, Set<Dependency> requestedDependencies) {
        return createVersion(component, version, requestedDependencies, new HashSet<>());
    }

    protected Version createVersion(String component, String version, Set<Dependency> requestedDependencies, Set<Dependency> resolvedDependencies) {
        Version v = new Version(component, version);
        v.setRequestedDependencies(requestedDependencies);
        v.setResolvedDependencies(resolvedDependencies);

        vantageApi.createOrUpdateVersion(component, version, false, v);
        queueService.processFrontOfQueue();

        v.setActive(true); //it's probably going to be active

        return v;
    }

    protected Dependency createDependency(String component, String version) {
        return new Dependency(new Version(component, version), Sets.newHashSet("compile", "testCompile"));
    }

    protected Issue createIssue(Version affectsVersion) {
        return createIssue(affectsVersion, null);
    }

    protected Issue createIssue(Version affectsVersion, Version fixVersion) {
        Issue issue = new Issue();
        issue.setId("issue-id" + RandomStringUtils.randomAlphanumeric(8));
        issue.setAffectsVersion(affectsVersion);
        issue.setFixVersion(fixVersion);
        issue.setLevel(IssueLevel.CRITICAL);
        issue.setMessage("message");
        vantageApi.createOrUpdateIssue(issue.getId(), issue);
        return issue;
    }
}
