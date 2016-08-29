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

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yodle.vantage.component.dao.IssueDao;
import com.yodle.vantage.component.domain.Issue;

@Transactional
@Component
public class IssueService {
    @Autowired private IssueDao issueDao;
    @Autowired private ComponentService componentService;


    public Issue createOrUpdate(Issue issue) {
        if (issue.getAffectsVersion() != null) {
            componentService.ensureVersionExists(issue.getAffectsVersion().getComponent(), issue.getAffectsVersion().getVersion());
        }
        if (issue.getFixVersion() != null) {
            componentService.ensureVersionExists(issue.getFixVersion().getComponent(), issue.getFixVersion().getVersion());
        }

        return issueDao.createOrUpdate(issue);
    }

    public Optional<Issue> getIssue(String issueId) {
        return issueDao.getIssue(issueId);
    }

    public Set<Issue> getIssues() {
        return issueDao.getIssues();
    }
}
