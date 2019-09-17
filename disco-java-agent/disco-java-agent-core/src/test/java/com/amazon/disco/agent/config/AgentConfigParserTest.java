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
