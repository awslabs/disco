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

package com.amazon.disco.agent.reflect.logging;

import com.amazon.disco.agent.reflect.ReflectiveCall;
import com.amazon.disco.agent.logging.LoggerFactory;

import java.util.function.Consumer;

/**
 * Clients of the disco.agent.reflect package may supply logging callbacks for better visibility what is happening
 * when they call the methods provided by the package, and to enable Logging from the DiSCo agent itself, using
 * a supplied LoggerFactory
 */
public class Logger {
    private static final String LOGMANAGER_CLASSNAME = ".logging.LogManager";
    private static boolean suppressIfAgentNotPresent = false;
    private static final String prefix = "DiSCo(Reflect) ";
    static com.amazon.disco.agent.logging.Logger log;

    static {
        log = ReflectiveCall.returning(com.amazon.disco.agent.logging.Logger.class)
            .ofClass(LOGMANAGER_CLASSNAME)
            .ofMethod("getLogger")
            .withArgTypes(Class.class)
            .call(Logger.class);
    }

    /**
     * Install a LoggerFactory to be used throughout the DiSCo agent.
     * @param loggerFactory the client-supplied LoggerFactory instance to be used
     */
    public static void installLoggerFactory(LoggerFactory loggerFactory) {
        ReflectiveCall.returningVoid()
            .ofClass(LOGMANAGER_CLASSNAME)
            .ofMethod("installLoggerFactory")
            .withArgTypes(LoggerFactory.class)
            .call(loggerFactory);
    }

    /**
     * Log a debug message
     * @param s the message
     */
    public static void debug (String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.DEBUG, s);
    }

    /**
     * Log an info message
     * @param s the message
     */
    public static void info (String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.INFO, s);
    }

    /**
     * Log a warning message
     * @param s the message
     */
    public static void warn (String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.WARN, s);
    }

    /**
     * Log an error message
     * @param s the message
     */
    public static void error(String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.ERROR, s);
    }

    /**
     * Set whether to suppress messages when the agent is not present. By default logging is produced
     * regardless of the Agent being present or not
     * @param suppress false to suppress logging when no Agent is present
     */
    public static void suppressIfAgentNotPresent(boolean suppress) {
        suppressIfAgentNotPresent = suppress;
    }

    /**
     * Accept the logging of this string if conditions are met
     * @param level the logging level requested
     * @param s the message
     */
    static void maybeAccept(com.amazon.disco.agent.logging.Logger.Level level, String s) {
        boolean suppress = !ReflectiveCall.isAgentPresent() && suppressIfAgentNotPresent;
        if (suppress) {
            return;
        }

        if (log != null) {
            log.log(level, prefix + s);
        }
    }
}
