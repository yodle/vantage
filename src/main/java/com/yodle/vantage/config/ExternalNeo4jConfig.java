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
package com.yodle.vantage.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@Profile("!embedded")
public class ExternalNeo4jConfig {
    @Value("${neo4j.server:localhost}") String neo4jServer;
    @Value("${neo4j.port:7474}") int neo4jPort;
    @Value("${neo4j.user:neo4j}") String neo4jUsername;
    @Value("${neo4j.user:password}") String neo4jPassword;

    @Bean
    public DataSource neo4jDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                String.format(
                        "jdbc:neo4j://%s:%d",
                        neo4jServer,
                        neo4jPort
                ),
                neo4jUsername,
                neo4jPassword
        );
        return ds;
    }

}
