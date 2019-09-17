package com.amazon.disco.agent.config;

import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;

/**
 * This contains legacy config from now-deprecated agents.
 */
@Deprecated
public class Config {
    private static Logger log = LogManager.getLogger(Config.class);
    private static String victimApplicationName = null;

    /**
     * Initialize Config
     *
     * @param config the startup config used by AlphaOne
     */
    public static void init(AgentConfig config) {
        log.debug("AlphaOne(Core) initializing for " + config.getApplicationName());
        victimApplicationName = config.getApplicationName();
    }

    /**
     * Get the Application name that was passed in on the command line, which is the name of the service-under-test
     * @return - the victim application name
     */
    public static String getVictimApplicationName() {
        return victimApplicationName;
    }

    /**
     * Set or override the victim application name, for tests.
     * @param victimAppName - the victim application name
     */
    public static void setVictimApplicationName(String victimAppName) {
        victimApplicationName = victimAppName;
    }
}
