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

package software.amazon.disco.agent.interception;

import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;
import java.util.List;

/**
 * DiSCo interceptions are broken apart by category e.g. Web, AWS, concurrency support, etc. This interface
 * describes a hook which can be installed, and all DiSCo support 'plugins' adhere to it
 */
public interface Installable {
    /**
     * Install the necessary hooks for this piece of DiSCo functionality.
     *
     * @param agentBuilder - an AgentBuilder to append instructions to
     * @return - the same AgentBuilder instance, for continuation/chaining, or null to prevent installation of the
     * agentBuilder, in case the Installable instance does not require this instrumentation to take place.
     */
    AgentBuilder install(AgentBuilder agentBuilder);

    /**
     * Given a list of command line arguments, handle them in whatever way makes sense for the implementing class.
     *
     * This should ultimately be deprecated in favor of a more structured API, where the key-value pairs are expressed
     * in a structured form, instead of expecting Installables to parse a list of raw strings.
     *
     * @param args command line arguments in the form ["key1=value1", "key2=value2,value3", "value4"]
     */
    default void handleArguments(List<String> args) {}
}
