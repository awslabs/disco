package com.amazon.disco.agent.config;

import org.junit.Assert;
import org.junit.Test;


public class ConfigTests {
    @Test
    public void testSetApplicationName() {
        String appName = Config.getVictimApplicationName();
        Config.setVictimApplicationName("foo");
        Assert.assertEquals("foo", Config.getVictimApplicationName());
        Config.setVictimApplicationName(appName);
    }
}
