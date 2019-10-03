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

package com.amazon.disco.agent.injector;

import com.amazon.disco.agent.inject.Injector;

import java.lang.instrument.Instrumentation;

public class Agent {
    /**
     * Entry point when running the Injection agent via a "-javaagent" command line param.
     *
     * @param agentArgs any arguments passed as part of the -javaagent argument string
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        agentmain(agentArgs, instrumentation);
    }

    /**
     * Alternative entry point for if Agent is loaded after application startup. Manifest must declare the Agent-Class
     * attribute accordingly.
     *
     * @param agentArgs any arguments passed during loading e.g. via VirtualMachine.attach()
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        try {
            //parse the target Agent JAR from the args
            int indexOfSplitter = agentArgs.indexOf('=');
            String jarPath = agentArgs.substring(0, indexOfSplitter == -1 ? agentArgs.length() : indexOfSplitter);
            String remainingArgs = indexOfSplitter == -1 ? null : agentArgs.substring(indexOfSplitter + 1);
            Injector.loadAgent(instrumentation, jarPath, remainingArgs);
        } catch (Throwable t) {}
    }
}
