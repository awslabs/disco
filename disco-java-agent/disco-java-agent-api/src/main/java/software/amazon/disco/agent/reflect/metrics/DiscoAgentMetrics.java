/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.reflect.metrics;

import software.amazon.disco.agent.reflect.ReflectiveCall;

/**
 * Clients of the disco.agent.reflect package may retrieve Disco agent metrics information, such as how long the agent
 * has been running.
 */
public class DiscoAgentMetrics {
    static final String DISCOAGENTMETRICS_CLASS = ".metrics.DiscoAgentMetrics";

    /**
     * Simple utility method to observe how long the Disco agent has been running on this system.
     *
     * @return The time in nanoseconds since the agent started on this machine.
     */
    public static long getAgentUptime() {
        Long returnValue = ReflectiveCall
                .returning(Long.class)
                .ofClass(DISCOAGENTMETRICS_CLASS)
                .ofMethod("getAgentUptime")
                .call();

        return returnValue == null ? -1L : returnValue;
    }
}
