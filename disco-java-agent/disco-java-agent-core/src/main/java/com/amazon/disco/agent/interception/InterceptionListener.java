package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

/**
 * An implementation of a ByteBuddy listener, to spy on interception, for debugging.
 */
public class InterceptionListener implements AgentBuilder.Listener {
    private static Logger log = LogManager.getLogger(InterceptionListener.class);
    private final boolean shouldTrace;

    public static AgentBuilder.Listener INSTANCE = new InterceptionListener();

    private InterceptionListener() {
        shouldTrace = LogManager.isTraceEnabled();
    }

    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("Discovered " + typeName + (loaded?" (after":" (before") + " loading)");
        }
    }

    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        log.debug("Transforming " + typeDescription.getName() + (loaded?" (after":" (before") + " loading)");
    }

    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("Ignoring " + typeDescription.getName() + (loaded ? " (after" : " (before") + " loading)");
        }
    }

    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        log.warn("Failed to transform " + typeName + (loaded?" (after":" (before") + " loading) in classloader " + classLoader.toString(),
                throwable);
    }

    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (shouldTrace) {
            log.trace("Completed " + typeName + (loaded ? "(after" : "(before") + " loading)");
        }
    }
}
