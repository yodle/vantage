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
package com.yodle.vantage.component.service;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.google.common.collect.Lists;

public class MavenVersionUtilsTest {

    @Test
    public void givenNoVersions_isMavenStrategy_returnsTrue() {
        assertTrue("If there are no versions we should assume maven strategy", MavenVersionUtils.isMavenStrategy(new ArrayList<>()));
    }

    @Test
    public void givenVersionWithDots_isMavenStrategy_returnsTrue() {
        assertTrue("1.0 is a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("1.0")));
    }

    @Test
    public void givenVersionWithDashes_isMavenStrategy_returnsTrue() {
        assertTrue("1-0 is a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("1-0")));
    }

    @Test
    public void givenPurelyNumericVersion_isMavenStrategy_returnsTrue() {
        assertTrue("17 is a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("17")));
    }

    @Test
    public void givenPurelyAlphanumericVersion_isMavenStrategy_returnsFalse() {
        assertFalse("17aef is not a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("17aef")));
    }

    @Test
    public void givenMixOfNumericAndAlphanumericVersion_isMavenStrategy_returnsFalse() {
        assertFalse("17aef is not a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("51513", "17aef")));
    }

    @Test
    public void givenMavenVersionContainsLetters_isMavenStrategy_returnsTrue() {
        assertTrue("1.0.0-alpha is a maven version", MavenVersionUtils.isMavenStrategy(Lists.newArrayList("1.0.0-alpha")));
    }
}