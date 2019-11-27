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

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

/**
 * An implementation of a ByteBuddy listener, to spy on interception, for debugging.
 */
class InterceptionListener implements AgentBuilder.Listener {
    private static Logger log = LogManager.getLogger(InterceptionListener.class);
    private final String prefix;
    private final boolean shouldTrace;

    private InterceptionListener(Installable installable) {
        shouldTrace = LogManager.isTraceEnabled();
        prefix = installable.getClass().getName();
    }

    static InterceptionListener create(Installable installable) {
        return new InterceptionListener(installable);
    }

    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("DiSCo(Core) " + prefix + " discovered " + typeName + (loaded?" (after":" (before") + " loading)");
        }
    }

    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        log.debug("DiSCo(Core) " + prefix + " transforming " + typeDescription.getName() + (loaded?" (after":" (before") + " loading)");
    }

    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("DiSCo(Core) " + prefix + " ignoring " + typeDescription.getName() + (loaded ? " (after" : " (before") + " loading)");
        }
    }

    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        log.warn("DiSCo(Core) " + prefix + " failed to transform " + typeName + (loaded?" (after":" (before") + " loading) in classloader " + classLoader.toString(),
                throwable);
    }

    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("DiSCo(Core) " + prefix + " completed " + typeName + (loaded ? "(after" : "(before") + " loading)");
        }
    }
}
