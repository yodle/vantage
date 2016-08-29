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
package com.yodle.vantage.component.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.service.ComponentService;
import com.yodle.vantage.component.service.QueueService;
import com.yodle.vantage.exception.NoComponentFoundException;
import com.yodle.vantage.exception.NoVersionFoundException;

@RestController
@RequestMapping("/api/v1/components")
public class ComponentController {

    @Autowired private ComponentService componentService;
    @Autowired private QueueService queueService;
    private final static Logger l = LoggerFactory.getLogger(ComponentController.class);

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<VantageComponent> getAllComponents() {
        return componentService.getAllComponents();
    }

    @RequestMapping(value = "/{component:.+}", method = RequestMethod.PUT)
    public VantageComponent createOrUpdateComponent(@RequestBody VantageComponent vantageComponent) {
        return componentService.createOrUpdateComponent(vantageComponent);
    }

    @RequestMapping(value = "/{component:.+}", method = RequestMethod.GET)
    public VantageComponent getComponent(@PathVariable String component) {
        Optional<VantageComponent> componentOpt = componentService.getComponent(component);

        return componentOpt.orElseThrow(() -> new NoComponentFoundException(component));
    }

    @RequestMapping(value = "/{component}/versions/{version:.+}", method = RequestMethod.PUT, consumes = "application/json")
    public ResponseEntity<Version> createOrUpdateVersion(@PathVariable String component,
                                                             @RequestParam (required = false, defaultValue = "false") boolean dryRun,
                                                             @RequestBody Version version) throws Exception {
        if (dryRun) {
            l.info("Performing dry-run create for {}:{}", component, version.getVersion());
            Version createdVersion = componentService.createOrUpdateDryRunVersion(version);
            l.info("Found {} issues for {}:{}", createdVersion.getTransitiveIssues().size(), component, createdVersion.getVersion());
            return  new ResponseEntity<>(createdVersion, HttpStatus.OK);
        }
        queueService.queueCreateRequest(version);
        return new ResponseEntity<>(version, HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/{component}/versions", method = RequestMethod.GET)
    public List<Version> getVersionsOfComponent(@PathVariable String component) {
        return componentService.getVersions(component).orElseThrow(() -> new NoComponentFoundException(component));
    }

    @RequestMapping(value = "/{component}/versions/{version:.+}", method = RequestMethod.GET)
    public Version getVersion(@PathVariable String component, @PathVariable String version) {
        return componentService.getVersion(component, version).orElseThrow(() -> new NoVersionFoundException(component, version));
    }
}
