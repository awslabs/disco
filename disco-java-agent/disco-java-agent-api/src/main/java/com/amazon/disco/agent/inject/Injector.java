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

package com.amazon.disco.agent.inject;

import com.sun.tools.attach.VirtualMachine;

import java.lang.management.ManagementFactory;

/**
 * In an already running application, such as in AWS Lambda where users cannot control the JVM arguments, we can instead
 * 'inject' an Agent via the shim Agent disco-java-agent-injector.
 */
public class Injector {
    /**
     * Load an agent into an already running app. Use of this method requires the Java tools.jar to be available.
     * In AWS Lambda, only the Java JRE is provided, not the full JDK, so this is missing. Users are responsible for
     * adding it to their runtime, in a layer or by any other mechanism.
     *
     * @param injectorPath path to the built disco-java-agent-injector JAR file
     * @param agentJarPath path to the 'real' agent desired to be installed
     * @param agentArgs arguments to be passed to the real agent
     * @throws Exception may be thrown
     */
    public static void loadAgent(String injectorPath, String agentJarPath, String agentArgs) throws Exception {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(injectorPath, agentJarPath + "=" + agentArgs);
        vm.detach();
    }
}
