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

import java.security.Principal;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * See http://swagger.io/ for more info
 * This provides the /api-docs/ json metadata controller
 * The swagger-ui exists completely as static html/js in src/main/webapp
 */
@Configuration
@EnableSwagger
public class SwaggerConfig {

    @Autowired
    private SpringSwaggerConfig springSwaggerConfig;

    /**
     * Every SwaggerSpringMvcPlugin bean is picked up by the swagger-mvc framework - allowing for multiple
     * swagger groups i.e. same code base multiple swagger resource listings.
     * <p>
     * accessed by /api-docs (in other words, the default group)
     */
    @Bean
    public SwaggerSpringMvcPlugin publicApi() {
        return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
                .apiInfo(apiInfo())
                .apiVersion("1")
                .includePatterns("/api/.*")
                .ignoredParameterTypes(Principal.class);
    }


    private ApiInfo apiInfo() {
        return new ApiInfo(
                //title
                "Vantage",
                //description
                "Vantage is a tool that automates vulnerability and bug checking of dependencies",
                //terms of service
                "",
                //contact email
                "dkesler@yodle.com",
                //license type
                "Apache 2",
                //license url
                "http://www.apache.org/licenses/LICENSE-2.0"
        );
    }

}

