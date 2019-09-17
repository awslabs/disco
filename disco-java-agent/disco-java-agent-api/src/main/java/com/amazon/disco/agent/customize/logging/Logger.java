package com.amazon.disco.agent.customize.logging;

import com.amazon.disco.agent.customize.ReflectiveCall;
import com.amazon.disco.agent.logging.LoggerFactory;

import java.util.function.Consumer;

/**
 * Clients of the Customization package may supply logging callbacks for better visibility what is happening
 * when they call the methods provided by the package, and to enable Logging from the AlphaOne agent itself, using
 * a supplied LoggerFactory
 */
public class Logger {
    private static final String LOGMANAGER_CLASSNAME = ".logging.LogManager";
    private static boolean suppressIfAgentNotPresent = false;
    private static Consumer<String> debug = null;
    private static Consumer<String> info = null;
    private static Consumer<String> warn = null;
    private static Consumer<String> error = null;
    private static final String prefix = "AlphaOne(Customization) ";
    static com.amazon.disco.agent.logging.Logger log;

    static {
        log = ReflectiveCall.returning(com.amazon.disco.agent.logging.Logger.class)
            .ofClass(LOGMANAGER_CLASSNAME)
            .ofMethod("getLogger")
            .withArgTypes(Class.class)
            .call(Logger.class);
    }

    /**
     * Set a method to receive debug level of log
     * @param handler a method taking a String to delegate to the client's logging mechanism
     */
    @Deprecated
    public static void setDebugHandler(Consumer<String> handler) {
        debug = handler;
    }

    /**
     * Set a method to receive info level of log
     * @param handler a method taking a String to delegate to the client's logging mechanism
     */
    @Deprecated
    public static void setInfoHandler(Consumer<String> handler) {
        info = handler;
    }

    /**
     * Set a method to receive warn level of log
     * @param handler a method taking a String to delegate to the client's logging mechanism
     */
    @Deprecated
    public static void setWarnHandler(Consumer<String> handler) {
        warn = handler;
    }

    /**
     * Set a method to receive error level of log
     * @param handler a method taking a String to delegate to the client's logging mechanism
     */
    @Deprecated
    public static void setErrorHandler(Consumer<String> handler) {
        error = handler;
    }

    /**
     * Install a LoggerFactory to be used throughout the AlphaOne agent.
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
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.DEBUG, debug, s);
    }

    /**
     * Log an info message
     * @param s the message
     */
    public static void info (String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.INFO, info, s);
    }

    /**
     * Log a warning message
     * @param s the message
     */
    public static void warn (String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.WARN, warn, s);
    }

    /**
     * Log an error message
     * @param s the message
     */
    public static void error(String s) {
        maybeAccept(com.amazon.disco.agent.logging.Logger.Level.ERROR, error, s);
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
     * @param s the message
     */
    static void maybeAccept(com.amazon.disco.agent.logging.Logger.Level level, Consumer<String> handler, String s) {
        boolean suppress = !ReflectiveCall.isAgentPresent() && suppressIfAgentNotPresent;
        if (suppress) {
            return;
        }

        if (handler != null) {
            handler.accept(prefix + s);
        } else if (log != null) {
            log.log(level, prefix + s);
        }
    }
}