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
package com.yodle.vantage.component.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.Version;

@Component
public class VersionDao {
    public static final String UNKNOWN_VERSION = "unknown";
    public static final String LATEST_VERSION = "latest";
    public static final String UNDEFINED_VERSION = "undefined"; //Gradle will set versions to undefined if not set
    public static final Set<String> RESERVED_VERSIONS = Sets.newHashSet(UNDEFINED_VERSION, UNKNOWN_VERSION, LATEST_VERSION);
    @Autowired private JdbcTemplate jdbcTemplate;

    public boolean createNewVersion(String componentName, String version) {
        if (!isRealVersion(version)) {
            throw new RuntimeException(
                    String.format("You cannot create a dynamic version [%s] for component [%s]", version, componentName)
            );
        }

        if (isLatestVersion(version)) {
            throw new RuntimeException(
                    String.format("You cannot specify a version of 'latest' when creating a version for component [%s]", componentName)
            );
        }

        return (jdbcTemplate.queryForMap(
                "MATCH (c:Component {name: {1}}) " +
                        "MERGE (v:Version {version: {2}})-[:VERSION_OF]->(c) " +
                        "ON CREATE SET v.created=timestamp() " +
                        "ON MATCH SET v.matched=true " +
                        "return has(v.matched) as matched",
                componentName, version
        ).get("matched")).equals(false);
    }

    private void createUnknownVersion(String component) {
	createShadowVersion(component, UNKNOWN_VERSION);
    }

    private void createLatestVersion(String component) {
	createShadowVersion(component, LATEST_VERSION);
    }

    private void createShadowVersion(String component, String type) {
        jdbcTemplate.update(
                "MATCH (c:Component {name: {1}}) " +
                        "MERGE (v:Version {version: {2}, unknown: true})-[:VERSION_OF]->(c) ON CREATE SET v.created=timestamp() ",
                component, type
        );
    }

    public boolean createResolvedImplicitDependency(Version version, Version dep, Collection<String> profiles) {
	boolean created = false;
        String resolvedVersion = dep.getVersion();
        if (isLatestVersion(dep.getVersion())) {
	    createLatestVersion(dep.getComponent());
	    resolvedVersion = LATEST_VERSION;
        } else {
            created = createNewVersion(dep.getComponent(), dep.getVersion());
	}

        jdbcTemplate.update(
                "MATCH (c:Component {name: {1}})<-[:VERSION_OF]-(v:Version {version:{2}})," +
                        "(c_new:Component {name: {4}})<-[:VERSION_OF]-(v_new:Version {version:{3}}) " +
                        "CREATE UNIQUE (v)-[:DEPENDS_ON {profiles:{5}}]->(v_new)",
                version.getComponent(), version.getVersion(), resolvedVersion, dep.getComponent(), profiles
        );
        return created;
    }

    public boolean createRequestedImplicitDependency(Version version, Version dep, Collection<String> profiles) {
        boolean created = false;
        String resolvedVersion = dep.getVersion();
        if (isLatestVersion(dep.getVersion())) {
            createLatestVersion(dep.getComponent());
            resolvedVersion = LATEST_VERSION;
        } else if (isRealVersion(dep.getVersion())) {
            created = createNewVersion(dep.getComponent(), dep.getVersion());
        } else {
            createUnknownVersion(dep.getComponent());
            resolvedVersion = UNKNOWN_VERSION;
        }
        String requestedVersion = dep.getVersion() == null? "" : dep.getVersion();
        jdbcTemplate.update(
                "MATCH (c:Component {name: {1}})<-[:VERSION_OF]-(v:Version {version:{2}})," +
                        "(c_new:Component {name: {4}})<-[:VERSION_OF]-(v_dep:Version {version:{3}}) " +
                        "MERGE (v)-[:REQUESTS {profiles:{5}, requestVersion:{6}}]->(v_dep)",
                version.getComponent(), version.getVersion(), resolvedVersion, dep.getComponent(), profiles, requestedVersion
        );

        return created;
    }

    private static final Pattern REAL_VERSION_PATTERN = Pattern.compile("^[\\-.0-9a-zA-Z_]+$");
    private boolean isRealVersion(String version) {
        return version != null && !RESERVED_VERSIONS.contains(version) && REAL_VERSION_PATTERN.matcher(version).matches();
    }
    private boolean isLatestVersion(String version) {
	return version != null && version.toLowerCase().equals(LATEST_VERSION);
    }

    public Optional<Version> getVersion(String component, String version) {

        List<Map<String, Object>> matches = jdbcTemplate.queryForList("MATCH (v:Version {version:{1}})-[:VERSION_OF]->(c:Component {name:{2}}) RETURN v", version, component);
        if (matches.size() == 0) {
            return Optional.empty();
        }

        List<Map<String, Object>> rsInc = jdbcTemplate.queryForList(
                "MATCH (v:Version {version:{1}})-[:VERSION_OF]->(c:Component {name:{2}}), " +
                        "(c_dep:Component)<-[:VERSION_OF]-(v_dep:Version)-[r_dep:DEPENDS_ON]->(v), " +
                        "(v_active:Version)-[:DEPENDS_ON*0..]->(v_dep) " +
                        "WHERE NOT (v_active)-[:PRECEDES]->() " +
                        "RETURN c_dep.name, v_dep.version, r_dep.profiles",
                version, component
        );

        List<Map<String, Object>> rsDep = jdbcTemplate.queryForList(
                "MATCH (v:Version {version:{1}})-[:VERSION_OF]->(c:Component {name:{2}})," +
                        "(c_dep:Component)<-[:VERSION_OF]-(v_dep:Version)<-[r_dep:DEPENDS_ON]-(v) " +
                        "RETURN c_dep.name, v_dep.version, r_dep.profiles",
                version, component
        );

        List<Map<String, Object>> reqDep = jdbcTemplate.queryForList(
                "MATCH (v:Version {version:{1}})-[:VERSION_OF]->(c:Component {name:{2}})," +
                        "(c_dep:Component)<-[:VERSION_OF]-(v_dep:Version)<-[r_dep:REQUESTS]-(v) " +
                        "RETURN c_dep.name, v_dep.version, r_dep.profiles",
                version, component
        );

        Set<Dependency> incoming = transformToDependencies(rsInc);
        Set<Dependency> dependencies = transformToDependencies(rsDep);
        Set<Dependency> requestedDependencies = transformToDependencies(reqDep);

        Version v = new Version(component, version, getActiveVersions(component).contains(version));
        v.setDependents(incoming);
        v.setResolvedDependencies(dependencies);
        v.setRequestedDependencies(requestedDependencies);
        return Optional.of(v);
    }

    private Set<Dependency> transformToDependencies(List<Map<String, Object>> rsInc) {
        return rsInc.stream().map(this::toDepVersion).collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    public Dependency toDepVersion(Map<String, Object> m) {
        String vDep = (String) m.get("v_dep.version");
        String name = (String) m.get("c_dep.name");
        Collection<String> profiles = (Collection<String>) m.get("r_dep.profiles");
        return new Dependency(new Version(name, vDep), profiles);
    }

    public List<String> getVersions(String component) {
        List<Map<String,Object>> rs = jdbcTemplate.queryForList(
                "MATCH (v:Version)-[:VERSION_OF]->(c:Component {name:{1}}) " +
                        "WHERE NOT has(v.unknown) " +
                        "return v " +
                        "ORDER BY v.created DESC",
                component
        );

        return mapRsToVersion(rs);
    }

    public List<String> getActiveVersions(String component) {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                "MATCH (c:Component {name:{1}})<-[r:VERSION_OF]-(v) " +
                        "WHERE NOT (v)-[:PRECEDES]->() AND NOT has(v.unknown) " +
                        "RETURN v " +
                        "UNION " +
                        "MATCH (c:Component {name:{1}})<-[r:VERSION_OF]-(v)<-[r2:DEPENDS_ON*]-(v2) " +
                        "WHERE NOT (v2)-[:PRECEDES]->() AND NOT has(v2.unknown) " +
                        "RETURN v",
                component
        );

        return mapRsToVersion(rs);
    }

    @SuppressWarnings("unchecked")
    private List<String> mapRsToVersion(List<Map<String, Object>> rs) {
        return rs.stream().map(m -> (String) ((Map<String, Object>) m.get("v")).get("version")).collect(Collectors.toList());
    }

    public void insertPrecendenceBetween(String component, String version, String prev, String next) {
        if (prev != null && next != null) {
            jdbcTemplate.update(
                "MATCH (c:Component {name:{1}})" +
                        "<-[:VERSION_OF]-(prev:Version {version: {2}})" +
                        "-[p:PRECEDES]->(next:Version {version: {3}})" +
                        "-[:VERSION_OF]->(c) " +
                        "DELETE p",
                    component, prev, next
            );
        }

        if (prev != null) {
            jdbcTemplate.update(
                "MATCH (c:Component {name:{1}})" +
                        "<-[:VERSION_OF]-(prev:Version {version:{2}})," +
                        "(c)<-[:VERSION_OF]-(v:Version {version:{3}}) " +
                        "MERGE (prev)-[:PRECEDES]->(v)",
                    component, prev, version
            );
        }

        if (next != null) {
            jdbcTemplate.update(
                "MATCH (c:Component {name:{1}})" +
                        "<-[:VERSION_OF]-(next:Version {version:{2}})," +
                        "(c)<-[:VERSION_OF]-(v:Version {version:{3}}) " +
                        "MERGE (v)-[:PRECEDES]->(next)",
                    component, next, version
            );
        }
    }

    public void insertPrecedenceAtEnd(String component, String version) {
        jdbcTemplate.update(
                "MATCH (v_new:Version {version:{2}})-[:VERSION_OF]->(c:Component {name: {1}})<-[:VERSION_OF]-(v_prev:Version) " +
                        "WHERE NOT v_prev.version = {2} and not v_prev-[:PRECEDES]->() AND NOT has(v_prev.unknown) " +
                        "CREATE (v_prev)-[:PRECEDES]->(v_new)",
                component, version
        );
    }
}

