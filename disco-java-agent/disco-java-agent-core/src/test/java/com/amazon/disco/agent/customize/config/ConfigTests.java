/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.disco.agent.customize.config;

import com.amazon.disco.agent.config.AgentConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class ConfigTests {
    @BeforeClass
    public static void beforeClass() {
        AgentConfig config = new AgentConfig(null) {
            {
                setApplicationName("DummyApplicationName");
            }
        };

        com.amazon.disco.agent.config.Config.init(config);
    }

    @Test
    public void testGetVictimApplicationNameWhenAlphaOneLoaded() {
        Assert.assertEquals("DummyApplicationName", Config.getVictimApplicationName());
    }
}
