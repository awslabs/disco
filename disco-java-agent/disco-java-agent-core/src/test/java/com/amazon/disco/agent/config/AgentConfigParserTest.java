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

package com.amazon.disco.agent.config;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentConfigParserTest {
    @Test
    public void testArgumentParsing() {
        final String argLine = "agent.jar:dependencywhitelist=WhiteListValue:verbose:applicationname=appname";

        AgentConfig config = new AgentConfigParser().parseCommandLine(argLine);

        assertEquals("appname", config.getApplicationName());
        assertTrue(config.isVerbose());
        assertFalse(config.isExtraverbose());
    }

    @Test
    public void testColonAndEqualsEscaping() {
        final String argLine = "agent.jar:applicationname=appname-with\\:and\\=-is-cute";
        AgentConfig config = new AgentConfigParser().parseCommandLine(argLine);
        assertEquals("appname-with:and=-is-cute", config.getApplicationName());
    }
}
