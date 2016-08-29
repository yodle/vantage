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

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yodle.vantage.component.domain.Version;

@Component
public class QueueDao {
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    private static Logger l = LoggerFactory.getLogger(QueueDao.class);

    public void lockQueueTail() {
        String id = RandomStringUtils.randomAlphanumeric(16);
        jdbcTemplate.update(
                "MERGE (l:QueueLock {name:'tail'}) " +
                        "ON MATCH SET l._lock_ = {1}",
                id
        );
    }

    public void lockQueueHead() {
        String id = RandomStringUtils.randomAlphanumeric(16);
        jdbcTemplate.update(
                "MERGE (l:QueueLock {name:'head'}) " +
                        "ON MATCH SET l._lock_ = {1}",
                id
        );
    }

    public void saveCreateRequest(Version version) {
        try {
            String blob = objectMapper.writeValueAsString(version);
            String id = RandomStringUtils.randomAlphanumeric(16);
            jdbcTemplate.update(
                    "CREATE (c_new:QueueCreateRequest {blob:{1}, id: {2}, created: timestamp()})",
                    blob, id
            );

            jdbcTemplate.update(
                    "MATCH (c:QueueCreateRequest), " +
                            "(c_new:QueueCreateRequest {id:{1}}) " +
                            "WHERE NOT c.id = {1} AND NOT (c)-[:BEFORE]->() " +
                            "CREATE (c)-[:BEFORE]->(c_new)",
                    id
            );
        } catch (IOException e) {
            throw new RuntimeException("Exception when trying to serialize create queue request", e);
        }
    }

    public static class QueueCreateRequest {
        public final Version v;
        public final String id;

        public QueueCreateRequest(Version v, String id) {
            this.v = v;
            this.id = id;
        }
    }

    public Optional<QueueCreateRequest> getCreateRequest() {

        try {
            Map<String,Object> rs = jdbcTemplate.queryForMap(
                    "MATCH(c:QueueCreateRequest) " +
                            "WHERE NOT ()-[:BEFORE]->(c) " +
                            "return c.id, c.blob"
            );

            return Optional.of(
                    new QueueCreateRequest(
                            objectMapper.readValue((String) rs.get("c.blob"), Version.class),
                            (String) rs.get("c.id")
                    )
            );
        } /*catch (UncategorizedSQLException e) {
            l.debug("Exception occurred getting create request, probably because of queue locking", e);
        }*/ catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (IOException e) {
            l.error("Error deserializing queue entry", e);
            return Optional.empty();
        }
    }

    public void deleteCreateRequest(String createRequestId) {
        jdbcTemplate.update(
                "MATCH (c:QueueCreateRequest {id:{1}})" +
                        "OPTIONAL MATCH (c)-[r:BEFORE]->() " +
                        "DELETE c, r",
                createRequestId
        );
    }
}
