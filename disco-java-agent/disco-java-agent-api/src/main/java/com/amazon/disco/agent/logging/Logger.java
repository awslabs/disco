package com.amazon.disco.agent.logging;

/**
 * In a manner similar to Java logging frameworks such as log4j, AlphaOne provides a pluggable interface for attaching
 * named loggers e.g. by following the convention of a named logger per named Class, or by electing arbitrary names.
 * Internally AlphaOne constructs instances of Loggers named after its Class names, and by default the logging implementation
 * is a Null Logger which has no-op behaviors.
 *
 * Clients may instantiate an instance of the Logger interface, to direct AlphaOne's log to a real logging implementation
 * of their choosing - e.g. a simple wrapper around System.out (provided in the Customization package as a courtesy) or
 * by smarter implementations which use the desired logging framework supplied within the service-under-test.
 */
public interface Logger {
    /**
     * Levels of log, in increasing order of severity
     */
    enum Level {
        /**
         * The lowest and least useful form of Logging in most circumstances. Can tend to be extremely verbose, and is
         * unlikely to be useful except for AlphaOne developers, or under rare debugging scenarios. Enabled by the AlphaOne
         * 'extraverbose' command line argument.
         */
        TRACE,

        /**
         * A very low and rarely useful form of Logging. Can tend to be somewhat verbose, and is unlikely to be useful
         * except for AlphaOne developers, or under rare debugging scenarios. Enabled by the AlphaOne 'verbose' command
         * line argument.
         */
        DEBUG,

        /**
         * By default the minimum visible level of Logging. Info logs are notifications of major, normal, events during
         * the software lifetime.
         */
        INFO,

        /**
         * A warning is generally informative, but may be a canary for missing functionality. When only warnings are present
         * the software is expected to proceed without harm, but may be degraded.
         */
        WARN,

        /**
         * An error is an indication that AlphaOne has detected a serious problem, and was unable to proceed as requested.
         * There is a possibility or likelihood that the service-under-test will encounter its own problems or failures
         */
        ERROR,

        /**
         * A fatal error is the most serious kind of log. It indicates that the service-under-test is unlikely to be able to
         * continue operating normally.
         */
        FATAL
    };

    /**
     * Log the given message of the given severity.
     * @param level the Level of severity of the log content. Can be ignored by implementers as desired, since AlphaOne
     *              internally will manage aspects of verbosity
     * @param message the message to be logged
     */
    void log(Level level, String message);

    /**
     * Log the given Throwable object with the given message severity.
     * @param level the Level of severity of the log content. Can be ignored by implementers as desired, since AlphaOne
     *              internally will manage aspects of verbosity
     * @param t the Throwable object to be logged
     */
    void log(Level level, Throwable t);

    /**
     * Log the given Throwable object with the given Message at the given severity.
     * @param level the Level of severity of the log content. Can be ignored by implementers as desired, since AlphaOne
     *              internally will manage aspects of verbosity
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    void log(Level level, String message, Throwable t);

    /**
     * Log the given message at the TRACE level of severity.
     * @param message the message to be logged
     */
    default void trace(String message) {
        log(Level.TRACE, message);
    }

    /**
     * Log the given Throwable at the TRACE level of severity.
     * @param t the Throwable object to be logged
     */
    default void trace(Throwable t) {
        log(Level.TRACE, t);
    }

    /**
     * Log the given Throwable object with the given Message at the TRACE level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void trace(String message, Throwable t) {
        log(Level.TRACE, message, t);
    }

    /**
     * Log the given message at the DEBUG level of severity.
     * @param message the message to be logged
     */
    default void debug(String message) {
        log(Level.DEBUG, message);
    }

    /**
     * Log the given Throwable at the DEBUG level of severity.
     * @param t the Throwable object to be logged
     */
    default void debug(Throwable t) {
        log(Level.DEBUG, t);
    }

    /**
     * Log the given Throwable object with the given Message at the DEBUG level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void debug(String message, Throwable t) {
        log(Level.DEBUG, message, t);
    }

    /**
     * Log the given message at the INFO level of severity.
     * @param message the message to be logged
     */
    default void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Log the given Throwable at the INFO level of severity.
     * @param t the Throwable object to be logged
     */
    default void info(Throwable t) {
        log(Level.INFO, t);
    }

    /**
     * Log the given Throwable object with the given Message at the INFO level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void info(String message, Throwable t) {
        log(Level.INFO, message, t);
    }

    /**
     * Log the given message at the WARN level of severity.
     * @param message the message to be logged
     */
    default void warn(String message) {
        log(Level.WARN, message);
    }

    /**
     * Log the given Throwable at the WARN level of severity.
     * @param t the Throwable object to be logged
     */
    default void warn(Throwable t) {
        log(Level.WARN, t);
    }

    /**
     * Log the given Throwable object with the given Message at the WARN level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void warn(String message, Throwable t) {
        log(Level.WARN, message, t);
    }

    /**
     * Log the given message at the ERROR level of severity.
     * @param message the message to be logged
     */
    default void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Log the given Throwable at the ERROR level of severity.
     * @param t the Throwable object to be logged
     */
    default void error(Throwable t) {
        log(Level.ERROR, t);
    }

    /**
     * Log the given Throwable object with the given Message at the ERROR level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    /**
     * Log the given message at the FATAL level of severity.
     * @param message the message to be logged
     */
    default void fatal(String message) {
        log(Level.FATAL, message);
    }

    /**
     * Log the given Throwable at the FATAL level of severity.
     * @param t the Throwable object to be logged
     */
    default void fatal(Throwable t) {
        log(Level.FATAL, t);
    }

    /**
     * Log the given Throwable object with the given Message at the FATAL level of severity.
     * @param message the message to be logged
     * @param t the Throwable object to be logged
     */
    default void fatal(String message, Throwable t) {
        log(Level.FATAL, message, t);
    }
}
