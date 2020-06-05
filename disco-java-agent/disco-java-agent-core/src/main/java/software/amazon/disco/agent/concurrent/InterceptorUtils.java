/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;

/**
 * Utility functions for Interceptors.
 */
public class InterceptorUtils {

    /**
     * Private constructor, use static methods directly.
     */
    private InterceptorUtils() {

    }

    /**
     * Configure the AgentBuilder to allow redefinition of an already-loaded class
     *
     * @param agentBuilder the AgentBuilder
     * @return the AgentBuilder, configured to redefine an already-loaded class
     */
    public static AgentBuilder configureRedefinition(AgentBuilder agentBuilder) {
        return agentBuilder
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE);
    }
}
