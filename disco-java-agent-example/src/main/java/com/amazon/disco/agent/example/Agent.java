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

package com.amazon.disco.agent.example;

import com.amazon.disco.agent.DiscoAgentTemplate;
import com.amazon.disco.agent.concurrent.ConcurrencySupport;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;

public class Agent {
    /**
     * The agent is loaded by a -javaagent command line parameter, which will treat 'premain' as its
     * entrypoint, in the class referenced by the Premain-Class attribute in the manifest - which should be this one.
     *
     * @param agentArgs - any arguments passed as part of the -javaagent argument string
     * @param instrumentation - the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        //install only the Concurrency support, just as the most simplistic test.
        new DiscoAgentTemplate(agentArgs).install(instrumentation, new HashSet<>(new ConcurrencySupport().get()));
    }
}
