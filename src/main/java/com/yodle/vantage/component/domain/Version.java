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
package com.yodle.vantage.component.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Version extends VersionId {

    private boolean active;
    private Set<Dependency> resolvedDependencies = new HashSet<>();
    private Set<Dependency> requestedDependencies = new HashSet<>();
    private Set<Dependency> dependents = new HashSet<>();

    private Collection<Issue> directIssues = new HashSet<>();
    private Collection<Issue> transitiveIssues = new HashSet<>();

    public Version() {}

    public Version(String component, String version, boolean active) {
        super(component, version);
        this.active = active;
    }

    public Version(String component, String version) {
        super(component, version);
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Dependency> getResolvedDependencies() {
        return resolvedDependencies;
    }

    public void setResolvedDependencies(Set<Dependency> resolvedDependencies) {
        this.resolvedDependencies = resolvedDependencies;
    }

    public Set<Dependency> getRequestedDependencies() {
        return requestedDependencies;
    }

    public void setRequestedDependencies(Set<Dependency> requestedDependencies) {
        this.requestedDependencies = requestedDependencies;
    }

    public Set<Dependency> getDependents() {
        return dependents;
    }

    public void setDependents(Set<Dependency> dependents) {
        this.dependents = dependents;
    }

    public Collection<Issue> getDirectIssues() {
        return directIssues;
    }

    public void setDirectIssues(Collection<Issue> directIssues) {
        if (directIssues != null) {
            this.directIssues = Sets.newHashSet(directIssues);
        }
    }

    public Collection<Issue> getTransitiveIssues() {
        return transitiveIssues;
    }

    public void setTransitiveIssues(Collection<Issue> transitiveIssues) {
        if (transitiveIssues != null) {
            this.transitiveIssues = Sets.newHashSet(transitiveIssues);
        }
    }
}
