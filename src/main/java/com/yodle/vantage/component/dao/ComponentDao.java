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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yodle.vantage.component.domain.VantageComponent;

@Component
public class ComponentDao {

    private static final String ALL_COMPONENTS_QUERY = "MATCH (c:Component) " +
            "OPTIONAL MATCH (c)<-[:VERSION_OF]-(v:Version) " +
            "WHERE NOT (v)-[:PRECEDES]->() AND NOT has(v.unknown)" +
            "return c.name, c.description, v.version ";
    private static final String ONE_COMPONENT_QUERY = "MATCH (c:Component {name:{1}}) " +
                "OPTIONAL MATCH (c)<-[:VERSION_OF]-(v:Version) " +
                "WHERE NOT (v)-[:PRECEDES]->() AND NOT has(v.unknown)" +
                "return c.name, c.description, v.version ";
    @Autowired private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void setupIndices() {
        jdbcTemplate.execute("CREATE CONSTRAINT ON (c:Component) ASSERT c.name IS UNIQUE;");
        jdbcTemplate.execute("CREATE CONSTRAINT ON (i:ISSUE) ASSERT i.id IS UNIQUE;");
    }

    @SuppressWarnings("unchecked")
    public Optional<VantageComponent> getComponent(String name) {
        List<Map<String,Object>> components = jdbcTemplate.queryForList(
                ONE_COMPONENT_QUERY,
                name
        );

        if (components.size() > 1) {
            throw new IllegalStateException("Found multiple matches for component [" + name + "]");
        } else if (components.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(toVantageComponent(components.get(0)));
        }
    }

    public VantageComponent createOrUpdateComponent(VantageComponent component) {
        String name = component.getName();
        String description = component.getDescription();

        return createOrUpdate(name, description);
    }

    public void ensureCreated(String name) {
        createOrUpdate(name, null);
    }

    private VantageComponent createOrUpdate(String name, String description) {
        Map<String, Object> component = mergeComponent(name, description);
        return new VantageComponent((String)component.get("name"), (String)component.get("description"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeComponent(String name, String description) {
        if (description != null) {
            return (Map<String, Object>) jdbcTemplate.queryForMap("MERGE (c:Component {name:{1}}) SET c.description = {2} RETURN c", name, description).get("c");
        } else {
            return (Map<String, Object>) jdbcTemplate.queryForMap("MERGE (c:Component {name:{1}}) RETURN c", name).get("c");
        }
    }

    public List<VantageComponent> getComponentsWithMostRecentVersion() {
        List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                ALL_COMPONENTS_QUERY
        );

        return rs.stream().map(this::toVantageComponent).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private VantageComponent toVantageComponent(Object row) {
        Map<String, Object> m = (Map<String, Object>)  row;

        VantageComponent vc = new VantageComponent(
                (String) m.get("c.name"),
                (String) m.get("c.description")
        );

        vc.setMostRecentVersion((String) m.get("v.version"));

        return vc;
    }
}
