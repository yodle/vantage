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
package com.yodle.vantage.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import feign.FeignException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.Dependency;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.domain.Version;
import com.yodle.vantage.component.service.QueueService;
import com.yodle.vantage.functional.config.VantageFunctionalTest;

public class ComponentGetCreateAndUpdateTest extends VantageFunctionalTest {
    @Autowired private QueueService queueService;

    @Test
    public void createComponent() {
        VantageComponent createdComponent = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        assertEquals("description", createdComponent.getDescription());
        assertEquals("component", createdComponent.getName());
        assertNull(createdComponent.getMostRecentVersion());

        VantageComponent returnedComponent = vantageApi.getComponent("component");
        assertEquals(createdComponent.getDescription(), returnedComponent.getDescription());
        assertEquals(createdComponent.getName(), returnedComponent.getName());
        assertNull(returnedComponent.getMostRecentVersion());
    }

    @Test
    public void updateComponent() {
        VantageComponent createdComponent = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        VantageComponent updatedComponent = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description 2"));

        assertEquals("description 2", updatedComponent.getDescription());

        VantageComponent returnedComponent = vantageApi.getComponent("component");
        assertEquals(updatedComponent.getDescription(), returnedComponent.getDescription());
    }

    @Test
    public void updateComponentWithNullDescription_doesNotRemoveDescription() {
        VantageComponent createdComponent = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        VantageComponent updatedComponent = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", null));

        assertEquals("description", updatedComponent.getDescription());

        VantageComponent returnedComponent = vantageApi.getComponent("component");
        assertEquals("description", returnedComponent.getDescription());
    }

    @Test
    public void getNonexistentComponent_throws404() {
        try {
            vantageApi.getComponent("non-existent component");
        } catch (FeignException fe) {
            assertEquals(404, fe.status());
            return;
        }

        fail("Should have thrown a 404");
    }

    @Test
    public void getAllComponents() {
        VantageComponent component1 = vantageApi.createOrUpdateComponent("component1", new VantageComponent("component1", "description1"));
        VantageComponent component2 = vantageApi.createOrUpdateComponent("component2", new VantageComponent("component2", null));

        List<VantageComponent> components = vantageApi.getAllComponents();
        Map<String, VantageComponent> componentsByName = components.stream().collect(Collectors.toMap(VantageComponent::getName, vc -> vc));
        assertTrue(componentsByName.containsKey("component1"));
        assertTrue(componentsByName.containsKey("component2"));

        assertEquals(component1.getDescription(), componentsByName.get(component1.getName()).getDescription());
        assertEquals(component2.getDescription(), componentsByName.get(component2.getName()).getDescription());
    }

    @Test
    public void getComponentWithMostRecentVersion() {
        VantageComponent component = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        vantageApi.createOrUpdateVersion(component.getName(), "1.0.0", false, new Version(component.getName(), "1.0.0"));
        queueService.processFrontOfQueue();
        Version recentVersion = vantageApi.createOrUpdateVersion(component.getName(), "2.0.0", false, new Version(component.getName(), "2.0.0"));
        queueService.processFrontOfQueue();

        VantageComponent returnedComponent = vantageApi.getComponent(component.getName());

        assertEquals(recentVersion.getVersion(), returnedComponent.getMostRecentVersion());
    }

    @Test
    public void getComponentMostRecentVersionIgnoresShadowVersions() {
        VantageComponent component = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        Version version = new Version(component.getName(), "1.0.0");
        Dependency dependency = new Dependency(new Version("dep version", "latest"), Sets.newHashSet("compile"));
        version.setResolvedDependencies(Sets.newHashSet(dependency));
        vantageApi.createOrUpdateVersion(component.getName(), "1.0.0", false, version);
        queueService.processFrontOfQueue();

        VantageComponent returnedComponent = vantageApi.getComponent(dependency.getVersion().getComponent());

        assertNull(returnedComponent.getMostRecentVersion());
    }

    @Test
    public void getComponentsWithMostRecentVersion() {
        VantageComponent component = vantageApi.createOrUpdateComponent("component", new VantageComponent("component", "description"));
        vantageApi.createOrUpdateVersion(component.getName(), "1.0.0", false, new Version(component.getName(), "1.0.0"));
        queueService.processFrontOfQueue();
        Version recentVersion = vantageApi.createOrUpdateVersion(component.getName(), "2.0.0", false, new Version(component.getName(), "2.0.0"));
        queueService.processFrontOfQueue();

        VantageComponent returnedComponent = vantageApi.getAllComponents().get(0);

        assertEquals(recentVersion.getVersion(), returnedComponent.getMostRecentVersion());
    }

}
