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

package com.amazon.disco.agent.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * AlphaOne supplies informative logging information, if needed or required by service owners. By default, AlphaOne logs
 * to a null/no-op implementation of a Logger, but clients may supply a LoggerFactory which produces more useful/visible
 * implementations.
 */
public class LogManager {
    /**
     * This map should be treated as 'put once'. Classes will take static references to created Loggers, so take care
     * to only mutate the underlying delegate, and not to re-construct or replace the LevelAwareDelegatingLogger contained.
     */
    private static final Map<String, LevelAwareDelegatingLogger> namedLoggers = new HashMap<>();
    private static Logger.Level minimumLevel = Logger.Level.INFO;
    private static int minimumLevelOrdinal = minimumLevel.ordinal();
    private static final int traceLevelOrdinal = Logger.Level.TRACE.ordinal();
    private static final int debugLevelOrdinal = Logger.Level.DEBUG.ordinal();
    private static LoggerFactory installedLoggerFactory = new NullLoggerFactory();

    /**
     * Set the minimum Level of logging which is passed to the underling Logger. By default this is the INFO level.
     *
     * @param level the desired minimum level of logging
     */
    public static void setMinimumLevel(Logger.Level level) {
        minimumLevel = level;
        minimumLevelOrdinal = level.ordinal();
    }

    /**
     * Get the minimum Level of logging which is passed to the underling Logger. By default this is the INFO level.
     *
     * @return the currently configured minimum level of logging
     */
    public static Logger.Level getMinimumLevel() {
        return minimumLevel;
    }

    /**
     * Determine if TRACE logging is enabled, to short circuit string evaluation (e.g. concatenation) where required.
     *
     * @return true if TRACE logging is enabled.
     */
    public static boolean isTraceEnabled() {
        return traceLevelOrdinal >= minimumLevelOrdinal;
    }

    /**
     * Determine if DEBUG logging is enabled, to short circuit string evaluation (e.g. concatenation) where required.
     *
     * @return true if DEBUG logging is enabled.
     */
    public static boolean isDebugEnabled() {
        return debugLevelOrdinal >= minimumLevelOrdinal;
    }

    /**
     * Get a Logger instance, named after the given Class
     *
     * @param clazz the Class which supplies the name of the created/supplied Logger
     * @return a Logger which has the given Class name as its name
     */
    public static Logger getLogger(Class clazz) {
        if (namedLoggers.containsKey(clazz.getName())) {
            return namedLoggers.get(clazz.getName());
        } else {
            LevelAwareDelegatingLogger logger = new LevelAwareDelegatingLogger(installedLoggerFactory.createLogger(clazz));
            namedLoggers.put(clazz.getName(), logger);
            return logger;
        }
    }

    /**
     * Install a LoggerFactory which will 1) be responsible for creating Loggers when new calls to getLogger are made and
     * 2) will be called once per each Logger already created/supplied via getLogger, to replace them with a Logger of its
     * own creation
     *
     * @param loggerFactory the LoggerFactory to use from now on, and to retroactively apply to already-created Loggers
     */
    public static void installLoggerFactory(LoggerFactory loggerFactory) {
        installedLoggerFactory = loggerFactory;
        for (Map.Entry<String, LevelAwareDelegatingLogger> entry: namedLoggers.entrySet()) {
            entry.getValue().setDelegate(loggerFactory.createLogger(entry.getKey()));
        }
    }

    /**
     * An indirection of the real factoried Logger, to gate log requests depending on if they are emitted at greater
     * than or equal to the configured minimum Level.
     */
    private static class LevelAwareDelegatingLogger implements Logger {
        Logger delegate;

        /**
         * Construct a new LevelAwareDelegatingLogger which delgates to the supplied Logger when requested to log at a
         * permitted Level
         *
         * @param delegate the Logger to which to delegate
         */
        LevelAwareDelegatingLogger(final Logger delegate) {
            this.delegate = delegate;
        }

        /**
         * Set and replace the underlying Logger delagate
         *
         * @param delegate the Logger to which to delegate
         * @return this LevelAwareDelegatingLogger
         */
        LevelAwareDelegatingLogger setDelegate(final Logger delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, String message) {
            if (level.ordinal() >= minimumLevel.ordinal()) {
                delegate.log(level, message);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, Throwable t) {
            if (level.ordinal() >= minimumLevel.ordinal()) {
                delegate.log(level, t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, String message, Throwable t) {
            if (level.ordinal() >= minimumLevel.ordinal()) {
                delegate.log(level, message, t);
            }
        }
    }

    /**
     * A default Null implementation of a LoggerFactory, which will always supply a no-op Logger
     */
    private static class NullLoggerFactory implements LoggerFactory {
        static final NullLogger nullLogger = new NullLogger();

        /**
         * {@inheritDoc}
         */
        @Override
        public Logger createLogger(String name) {
            return nullLogger;
        }

        /**
         * A default Null implementation of a Logger, whose log() methods have no effects
         */
        private static class NullLogger implements Logger {
            /**
             * {@inheritDoc}
             */
            @Override
            public void log(Level level, String message) {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void log(Level level, Throwable t) {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void log(Level level, String message, Throwable t) {
            }
        }
    }
}
