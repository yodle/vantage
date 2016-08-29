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
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.yodle.vantage.component.dao.VersionDao;
import com.yodle.vantage.component.domain.VersionId;

@RunWith(MockitoJUnitRunner.class)
public class PrecedenceFixerTest {
    private static final String COMPONENT = "component";
    @InjectMocks private PrecedenceFixer precedenceFixer;
    @Mock private VersionDao versionDao;

    @Test
    public void givenVersionsFollowMavenPrecedence_fixPrecedence_insertsBetweenMavenVersions() {
        when(versionDao.getVersions("component")).thenReturn(Lists.newArrayList("1.5.0", "1.0.1", "2.0.0", "1.0.0"));
        precedenceFixer.fixPrecendence(new VersionId(COMPONENT, "1.3.0"));

        verify(versionDao).insertPrecendenceBetween(COMPONENT, "1.3.0", "1.0.1", "1.5.0");
    }

    @Test
    public void givenVersionsFollowMavenPrecedenceAndNoPrevious_fixPrecedence_insertsPrecedenceProperly() {
        when(versionDao.getVersions("component")).thenReturn(Lists.newArrayList("1.5.0", "1.0.1", "2.0.0", "1.0.0"));
        precedenceFixer.fixPrecendence(new VersionId(COMPONENT, "0.9.0"));

        verify(versionDao).insertPrecendenceBetween(COMPONENT, "0.9.0", null, "1.0.0");
    }

    @Test
    public void givenVersionsFollowMavenPrecedenceAndNoNext_fixPrecedence_insertsPrecedenceProperly() {
        when(versionDao.getVersions("component")).thenReturn(Lists.newArrayList("1.5.0", "1.0.1", "2.0.0", "1.0.0"));
        precedenceFixer.fixPrecendence(new VersionId(COMPONENT, "3.0.0"));

        verify(versionDao).insertPrecendenceBetween(COMPONENT, "3.0.0", "2.0.0", null);
    }

    @Test
    public void givenVersionsDontFollowMavenPrecedence_fixPrecedence_insertsPrecedenceAtEnd() {
        when(versionDao.getVersions("component")).thenReturn(Lists.newArrayList("abc123", "def345", "1f3eac"));
        precedenceFixer.fixPrecendence(new VersionId(COMPONENT, "125fed"));

        verify(versionDao).insertPrecedenceAtEnd(COMPONENT, "125fed");
    }
}