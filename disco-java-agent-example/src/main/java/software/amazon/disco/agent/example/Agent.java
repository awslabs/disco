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

package software.amazon.disco.agent.example;

import software.amazon.disco.agent.DiscoAgentTemplate;
import software.amazon.disco.agent.concurrent.ConcurrencySupport;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.web.WebSupport;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

public class Agent {
    /**
     * The agent is loaded by a -javaagent command line parameter, which will treat 'premain' as its
     * entrypoint, in the class referenced by the Premain-Class attribute in the manifest - which should be this one.
     *
     * @param agentArgs - any arguments passed as part of the -javaagent argument string
     * @param instrumentation - the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        //As a monolithic agent, we choose which specific treatments to install.
        //
        //We specifically prohibit configuring plugin behaviour, to make this agent final and unconfigurable
        //
        //There is nothing to prevent the creation of a 'partially monolithic' agent, which is opinionated about
        //the Installables, Listeners (and any other extension points) it provides, whilst also allowing plugins.
        //
        //We just make this one completely final for the purposes of an example.
        DiscoAgentTemplate agent = new DiscoAgentTemplate(agentArgs);
        agent.setAllowPlugins(false);

        Set<Installable> installables = new HashSet<>();
        installables.addAll(new ConcurrencySupport().get());
        installables.addAll(new WebSupport().get());
        agent.install(instrumentation, installables);
    }
}
