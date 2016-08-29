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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.yodle.vantage.component.domain.Issue;
import com.yodle.vantage.component.domain.IssueLevel;
import com.yodle.vantage.component.domain.Version;

@Component
public class IssueDao {
    @Autowired private JdbcTemplate jdbcTemplate;

    public Issue createOrUpdate(Issue issue) {
        Map<String, Object> issueMap = jdbcTemplate.queryForMap(
                "MERGE (i:Issue {id:{1}}) return i",
                issue.getId()
        );

        String level = issue.getLevel() != null ? issue.getLevel().name() : (String) issueMap.get("level");
        String message = issue.getMessage() != null ? issue.getMessage() : (String) issueMap.get("message");

        jdbcTemplate.update(
                "MATCH (i:Issue {id:{1}}) set i.level = {2}, i.message = {3}",
                issue.getId(), level, message
        );

        if (issue.getAffectsVersion() != null) {
            jdbcTemplate.update(
                    "MATCH (i:Issue {id:{1}}), " +
                            "(v:Version {version:{2}})-[:VERSION_OF]->(c:Component {name:{3}}) " +
                            "OPTIONAL MATCH (i)-[cur_affects:AFFECTS]->(av:Version) " +
                            "WHERE av.version <> {2} " +
                            "DELETE cur_affects " +
                            "MERGE (i)-[:AFFECTS]->(v)",
                    issue.getId(), issue.getAffectsVersion().getVersion(), issue.getAffectsVersion().getComponent()
            );
        }

        if (issue.getFixVersion() != null) {
            jdbcTemplate.update(
                    "MATCH (i:Issue {id:{1}}), " +
                            "(v:Version {version:{2}})-[:VERSION_OF]->(c:Component {name:{3}}) " +
                            "OPTIONAL MATCH (i)-[cur_fix:FIXED_BY]->(fv:Version) " +
                            "DELETE cur_fix " +
                            "MERGE (i)-[:FIXED_BY]->(v)",
                    issue.getId(), issue.getFixVersion().getVersion(), issue.getFixVersion().getComponent()
            );
        } else {
            jdbcTemplate.update(
                    "MATCH (i:Issue {id:{1}})-[cur_fix:FIXED_BY]->(fv:Version) " +
                            "DELETE cur_fix;",
                    issue.getId()
            );
        }

        return getIssue(issue.getId()).orElseThrow(() -> new RuntimeException("Cannot find issue [" + issue + "] right after creating it"));
    }

    public Map<String, Collection<Issue>> getIssuesDirectlyAffectingVersions(String component) {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                "MATCH (c:Component {name:{1}})<-[:VERSION_OF]-(v:Version)<-[:PRECEDES|:AFFECTS*]-(i:Issue)" + 
                        "MATCH (i)-[:AFFECTS]->(av:Version)" + 
                        "WHERE NOT (v)<-[:PRECEDES|:FIXED_BY*]-(i)" + 
                        "OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" + 
                        "RETURN c.name, v.version, i.id, i.level, i.message, av.version, fv.version",
                component
        );

        ImmutableListMultimap<String,Map<String,Object>> grouped = Multimaps.index(rs, (row -> (String) row.get("v.version")));
        return Multimaps.transformValues(
                grouped,
                (this::toIssue)
        ).asMap();
    }

    public List<Issue> getIssuesDirectlyAffectingVersion(String component, String version) {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                "MATCH (c:Component {name:{1}})<-[:VERSION_OF]-(v:Version {version:{2}})<-[:PRECEDES|:AFFECTS*]-(i:Issue)" +
		        "MATCH (i)-[:AFFECTS]->(av:Version)" +
			"WHERE NOT (v)<-[:PRECEDES|:FIXED_BY*]-(i)" +
			"OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" +
		        "RETURN i.id, i.level, i.message, av.version, fv.version, c.name, v.version",
                component, version
        );

        return rs.stream().map(this::toIssue).collect(Collectors.toList());
    }

    public Map<String, Collection<Issue>> getIssuesTransitivelyAffectingVersions(String component) {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                "MATCH (c_par:Component {name:{1}})" +
                        "<-[:VERSION_OF]-(v_par:Version)" +
                        "-[:DEPENDS_ON]->(v:Version)" +
                        "<-[:PRECEDES|:AFFECTS*]-(i:Issue)" +
                        "MATCH (v)-[:VERSION_OF]->(c:Component)" +
                        "MATCH (i)-[:AFFECTS]->(av:Version)" +
                        "WHERE NOT (v)<-[:PRECEDES|:FIXED_BY*]-(i) " +
                        "OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" +
			"RETURN v_par.version, i.id, i.level, i.message, av.version, fv.version, c.name, v.version",
                component
        );

        ImmutableListMultimap<String,Map<String,Object>> grouped = Multimaps.index(rs, (row -> (String) row.get("v_par.version")));
        return Multimaps.transformValues(
                grouped,
                (this::toIssue)
        ).asMap();
    }

    private Issue toIssue(Map<String, Object> issueMap) {
        Issue i = new Issue();
        i.setId((String) issueMap.get("i.id"));
        String levelName = (String) issueMap.get("i.level");
        if (levelName != null) {
            i.setLevel(IssueLevel.valueOf(levelName));
        }
        i.setMessage((String)issueMap.get("i.message"));

        String affectedVersion = (String) issueMap.get("av.version");
        String component = (String) issueMap.get("c.name");
        i.setAffectsVersion(new Version(component, affectedVersion));

        String fixVersion = (String) issueMap.get("fv.version");
        if (fixVersion != null) {
            i.setFixVersion(new Version(component, fixVersion));
        }

        return i;
    }

    public Map<Version, Collection<Issue>> getIssuesByDependenciesOf(String component, String version) {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                "MATCH (c_par:Component {name:{1}})<-[:VERSION_OF]-(v_par:Version {version:{2}})-[:DEPENDS_ON]->" +
                        "(v:Version)<-[:PRECEDES|:AFFECTS*]-(i:Issue)" +
                        "MATCH (v)-[:VERSION_OF]->(c:Component)" +
                        "MATCH (i)-[:AFFECTS]->(av:Version)" +
                        "WHERE NOT (v)<-[:PRECEDES|:FIXED_BY*]-(i) " +
                        "OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" +
                        "RETURN v.version, c.name, i.id, i.level, i.message, av.version, fv.version", 
                component, version
        );

        ImmutableListMultimap<Version, Map<String, Object>> grouped = Multimaps.index(rs, (row -> new Version((String) row.get("c.name"), (String) row.get("v.version"))));
        return Multimaps.transformValues(
                grouped,
                this::toIssue
        ).asMap();
    }

    public Optional<Issue> getIssue(String issueId) {
        List<Map<String, Object>> issues = jdbcTemplate.queryForList(
                "MATCH (i:Issue {id: {1}})-[:AFFECTS]->(av:Version)-[:VERSION_OF]->(c:Component)" +
		        "OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" +
			"RETURN i.id, i.level, i.message, av.version, c.name, fv.version",
                issueId
        );

        if (issues.size() > 1) {
            throw new IllegalStateException("Found multiple matches for issue [" + issueId + "]");
        } else if (issues.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(toIssue(issues.get(0)));
        }
    }

    public Set<Issue> getIssues() {
        List<Map<String, Object>> issueMaps = jdbcTemplate.queryForList(
                "MATCH (i:Issue)-[:AFFECTS]->(av:Version)-[:VERSION_OF]->(c:Component)" +
                        "OPTIONAL MATCH (i)-[:FIXED_BY]->(fv:Version)" +
                        "RETURN i.id, i.level, i.message, av.version, c.name, fv.version"
        );

        return issueMaps.stream().map(this::toIssue).collect(Collectors.toSet());
    }
}
