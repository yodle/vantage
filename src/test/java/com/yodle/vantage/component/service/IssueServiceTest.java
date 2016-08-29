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

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.yodle.vantage.component.dao.IssueDao;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.Version;

@RunWith(MockitoJUnitRunner.class)
public class IssueServiceTest {
    @InjectMocks private IssueService issueService;
    @Mock private IssueDao issueDao;
    @Mock private ComponentService componentService;

    @Test
    public void givenNoFixOrAffectsVersion_createOrUpdate_justCreates() {
        Issue issue = new Issue();
        issue.setId("wat");
        issueService.createOrUpdate(issue);

        verify(issueDao).createOrUpdate(issue);
    }

    @Test
    public void givenFixVersion_createOrUpdate_createsFixVersionFirst() {
        Issue issue = new Issue();
        issue.setId("wat");
        issue.setFixVersion(new Version("component", "version"));
        issueService.createOrUpdate(issue);

        InOrder inOrder = Mockito.inOrder(componentService, issueDao);
        inOrder.verify(componentService).ensureVersionExists("component", "version");
        inOrder.verify(issueDao).createOrUpdate(issue);
    }

    @Test
    public void givenAffectsVersion_createOrUpdate_createsaffectsVersionFirst() {
        Issue issue = new Issue();
        issue.setId("wat");
        issue.setAffectsVersion(new Version("component", "version"));

        issueService.createOrUpdate(issue);

        InOrder inOrder = Mockito.inOrder(componentService, issueDao);
        inOrder.verify(componentService).ensureVersionExists("component", "version");
        inOrder.verify(issueDao).createOrUpdate(issue);
    }



}