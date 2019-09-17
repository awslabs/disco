package com.amazon.disco.agent.customize.config;

import com.amazon.disco.agent.customize.ReflectiveCall;


/**
 * Global configuration of AlphaOne
 */
@Deprecated
public class Config {
    /**
     * Config class name to append after ALPHA_ONE_AGENT_PACKAGE_ROOT
     */
    private static final String CONFIG_CLASS = ".config.Config";

    /**
     * Get the Application name that was passed in on the command line, which is the name of the service-under-test
     * @return - the victim application name
     */
    public static String getVictimApplicationName() {
        return ReflectiveCall.returning(String.class)
            .ofClass(CONFIG_CLASS)
            .ofMethod("getVictimApplicationName")
            .call();
    }
}
