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
import static org.junit.Assert.fail;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.Set;

import feign.FeignException;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.IssueLevel;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.functional.config.VantageFunctionalTest;

public class IssueGetCreateAndUpdateTest extends VantageFunctionalTest {
    @Test
    public void createIssueWithoutFixVersionToNonExistentVersion() {
        Issue issue = createIssue(new Version("component", "version"));

        Version version = vantageApi.getVersion("component", "version");

        assertEquals(Sets.newHashSet(issue), version.getDirectIssues());
    }

    @Test
    public void createIssueWithFixVersionToNonExistentVersion() {
        Issue issue = createIssue(new Version("component", "version"), new Version("component", "version2"));

        Version v1 = vantageApi.getVersion("component", "version");
        assertEquals(Sets.newHashSet(issue), v1.getDirectIssues());
        Version v2 = vantageApi.getVersion("component", "version2");
        assertTrue("v2 should have no issues since it is the fix version", v2.getDirectIssues().isEmpty());
    }

    @Test
    public void getNonexistentIssue() {
        try {
            vantageApi.getIssue("non-existent-issue");
            fail("Should have thrown a 404");
        } catch (FeignException e) {
            assertEquals(404, e.status());
        }
    }

    @Test
    public void getIssueWithFixVersion() {
        Issue issue = createIssue(new Version("component", "version"), new Version("component", "version2"));

        Issue returnedIssue = vantageApi.getIssue(issue.getId());

        assertEquals(issue, returnedIssue);
    }

    @Test
    public void getIssueWithoutFixVersion() {
        Issue issue = createIssue(new Version("component", "version"));

        Issue returnedIssue = vantageApi.getIssue(issue.getId());

        assertEquals(issue, returnedIssue);
    }

    @Test
    public void getIssues() {
        Issue issue1 = createIssue(new Version("component1", "version"), new Version("component1", "version2"));
        Issue issue2 = createIssue(new Version("component2", "version"), new Version("component2", "version2"));
        Issue issue3 = createIssue(new Version("component3", "version"), new Version("component3", "version2"));

        Set<Issue> returnedIssues = vantageApi.getIssues();

        assertEquals(Sets.newHashSet(issue1, issue2, issue3), returnedIssues);
    }

    @Test
    public void updateIssueWithNochanges() {
        Issue i = createIssue(createVersion("component", "version"));
        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }

    @Test
    public void updateIssueLevel() {
        Issue i = createIssue(createVersion("component", "version"));

        i.setLevel(IssueLevel.MINOR);

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }

    @Test
    public void updateMessage() {
        Issue i = createIssue(createVersion("component", "version"));

        i.setMessage("I am an updated string");

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }

    @Test
    public void setFixVersionWhenUnset() {
        Issue i = createIssue(createVersion("component", "version"));

        i.setFixVersion(createVersion("component", "version2"));

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }

    @Test
    public void updateFixVersionWhenAlreadySet() {
        Issue i = createIssue(createVersion("component", "version"), createVersion("component", "version2"));

        i.setFixVersion(createVersion("component", "version3"));

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }

    @Test
    public void removeFixVersionWhenAlreadySet() {
        Issue i = createIssue(createVersion("component", "version"), createVersion("component", "version2"));

        i.setFixVersion(null);

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }


    @Test
    public void updateAffectsVersionWhenAlreadySet() {
        Issue i = createIssue(createVersion("component", "version"));

        i.setAffectsVersion(createVersion("component", "version2"));

        vantageApi.createOrUpdateIssue(i.getId(), i);

        assertEquals(i, vantageApi.getIssue(i.getId()));
    }
}
