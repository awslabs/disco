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

import java.lang.reflect.Method;

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
     * This is all handled reflectively, which looks unpleasant, but relieves users of disco-java-agent-api from needing
     * a compile-time dependency on Java's tools.jar, which may not be present.
     *
     * @param injectorPath path to the built disco-java-agent-injector JAR file
     * @param agentJarPath path to the 'real' agent desired to be installed
     * @param agentArgs arguments to be passed to the real agent
     * @throws Exception may be thrown
     */
    public static void loadAgent(String injectorPath, String agentJarPath, String agentArgs) {
        try {
            //get hold of RuntimeMXBean of the running process
            Class<?> managementFactory = Class.forName("java.lang.management.ManagementFactory");
            Method getRuntimeMXBean = managementFactory.getDeclaredMethod("getRuntimeMXBean");
            Object mxBean = getRuntimeMXBean.invoke(null);

            //get the name of the MX bean, which encapsulates the process id
            Class<?> runtimeMxBean = getRuntimeMXBean.getReturnType();
            Method getName = runtimeMxBean.getDeclaredMethod("getName");
            String nameOfRunningVM = (String)getName.invoke(mxBean);

            //extract the pid from the name
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

            //create a VirtualMachine instance from the pid
            Class<?> virtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method attach = virtualMachine.getDeclaredMethod("attach", String.class);
            Object vm = attach.invoke(null, pid);

            //load the agent onto the running process (i.e. our own process)
            Method loadAgent = virtualMachine.getDeclaredMethod("loadAgent", String.class, String.class);
            loadAgent.invoke(vm, injectorPath, agentJarPath + "=" + agentArgs);

            //finally detach from the process after agent load complete
            Method detach = virtualMachine.getDeclaredMethod("detach");
            detach.invoke(vm);
        } catch (Throwable t) {
            //survive any error
        }

    }
}
