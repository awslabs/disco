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

import com.amazon.disco.agent.customize.ReflectiveCall;


/**
 * Global configuration of DiSCo
 */
@Deprecated
public class Config {
    /**
     * Config class name to append after DISCO_AGENT_PACKAGE_ROOT
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
