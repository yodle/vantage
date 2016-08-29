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

import java.io.File;

import javax.sql.DataSource;

import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@Profile("embedded")
public class EmbeddedNeo4jConfig {

    private static final Logger l = LoggerFactory.getLogger(EmbeddedNeo4jConfig.class);

    @Bean
    public ServerControls serverControls() {
        return TestServerBuilders.newInProcessBuilder(new File("build/neo4j")).newServer();
    }

    @Bean
    public DataSource neo4jDataSource() {
        l.info("Starting up embedded neo4j server at {}:{}", serverControls().httpURI().getHost(), serverControls().httpURI().getPort());
        DriverManagerDataSource ds = new DriverManagerDataSource(
                String.format(
                        "jdbc:neo4j://%s:%d",
                        serverControls().httpURI().getHost(),
                        serverControls().httpURI().getPort()
                ),
                "neo4j",
                "password"
        );
        return ds;
    }

}
