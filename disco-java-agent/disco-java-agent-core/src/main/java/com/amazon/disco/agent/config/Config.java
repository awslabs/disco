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
     * @param config the startup config used by DiSCo
     */
    public static void init(AgentConfig config) {
        log.debug("DiSCo(Core) initializing for " + config.getApplicationName());
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
