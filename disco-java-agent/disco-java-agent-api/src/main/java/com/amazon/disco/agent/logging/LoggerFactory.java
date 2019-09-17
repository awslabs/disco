package com.amazon.disco.agent.logging;

/**
 * By default AlphaOne has a null logging implementation, to ensure it is free of hazards and optimal by default. For
 * service owners wishing to have visible Logging, implement this class, and instances of Loggers which the LoggerFactory
 * is responsible for producing to e.g. redirect AlphaOne logging to the service-under-test's logging solution such as
 * log4j.
 */
public interface LoggerFactory {
    /**
     * Creates the named Logger. The created Logger may or may not honor the given name.
     * @param name the name to give the created Logger
     * @return the created Logger
     */
    Logger createLogger(String name);

    /**
     * It's typical for logging to be prefixed with the Class name which originated the log. This default convenience
     * method constructs a named logger with the given Class name.
     * @param clazz the Class requesting the named Logger
     * @return the created Logger
     */
    default Logger createLogger(Class clazz) {
        return createLogger(clazz.getName());
    }
}
