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

import java.util.List;
import java.util.Set;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;

@Headers(value = {"Accept: application/json", "Content-Type: application/json"})
public interface VantageApi {
    @RequestLine("GET /api/v1/components")
    List<VantageComponent> getAllComponents();

    @RequestLine("PUT /api/v1/components/{component}")
    VantageComponent createOrUpdateComponent(@Param("component") String componentName, VantageComponent component);

    @RequestLine("GET /api/v1/components/{component}")
    VantageComponent getComponent(@Param("component") String component);

    @RequestLine("PUT /api/v1/components/{component}/versions/{version}?dryRun={dryRun}")
    Version createOrUpdateVersion(@Param("component") String component, @Param("version") String versionId, @Param("dryRun") boolean dryRun, Version version);

    @RequestLine("GET /api/v1/components/{component}/versions")
    List<Version> getVersionsOfComponent(@Param("component") String compomnent);

    @RequestLine("GET /api/v1/components/{component}/versions/{version}")
    Version getVersion(@Param("component") String component, @Param("version") String version);

    @RequestLine("PUT /api/v1/issues/{issue}")
    Issue createOrUpdateIssue(@Param("issue") String issueId, Issue issue);

    @RequestLine("GET /api/v1/issues/{issue}")
    Issue getIssue(@Param("issue") String issueId);

    @RequestLine("GET /api/v1/issues")
    Set<Issue> getIssues();
}
