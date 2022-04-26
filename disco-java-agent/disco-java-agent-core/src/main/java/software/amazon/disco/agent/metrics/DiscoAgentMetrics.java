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

package software.amazon.disco.agent.metrics;

/**
 * A dedicated class for recording and managing Disco metrics (e.g. agent uptime).
 */
public class DiscoAgentMetrics {

    static final long AGENT_NOT_STARTED_LONG = -1L;

    static long agentStartTime = AGENT_NOT_STARTED_LONG;

    /**
     * Simple utility method to observe how long the Disco agent has been running on this system.
     *
     * @return The time in nanoseconds since the agent started on this machine.
     */
    public static long getAgentUptime() {
        if (agentStartTime == AGENT_NOT_STARTED_LONG) {
            return AGENT_NOT_STARTED_LONG;
        }
        return System.nanoTime() - agentStartTime;
    }

    /**
     * Sets the "agentStartTime" static field, designating the time the agent considers itself started. If the field was
     * already set, the original value of "agentStartTime" is preserved.
     *
     * @return Returns the agentStartTime.
     */
    public static long setAgentStartTime() {
        agentStartTime = agentStartTime == AGENT_NOT_STARTED_LONG ? System.nanoTime() : agentStartTime;
        return agentStartTime;
    }
}
