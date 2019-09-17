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
