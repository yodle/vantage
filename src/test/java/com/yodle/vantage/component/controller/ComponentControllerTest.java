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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.yodle.vantage.component.domain.VantageComponent;
import com.yodle.vantage.component.service.ComponentService;

@RunWith(MockitoJUnitRunner.class)
public class ComponentControllerTest {
    @InjectMocks private ComponentController componentController;
    @Mock private ComponentService componentService;

    @Test
    public void getAllComponents_getsAllComponents() throws Exception {
        List<VantageComponent> returnedComponents = Lists.newArrayList(
                new VantageComponent("comp1", "desc1")
        );
        when(componentService.getAllComponents()).thenReturn(returnedComponents);

        List < VantageComponent > allComponents = componentController.getAllComponents();

        assertEquals(returnedComponents, allComponents);
    }
}
