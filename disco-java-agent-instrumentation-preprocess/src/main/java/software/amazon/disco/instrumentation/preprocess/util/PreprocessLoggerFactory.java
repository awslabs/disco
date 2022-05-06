/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.util;

import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.logging.LoggerFactory;

/**
 * This implements the {@code LoggerFactory} interface and is used by preprocess to print out
 * its logs to stdout during built-time instrumentation. Other disco subprojects follow the
 * same logging mechanism, for example {@code StandardOutputLoggerFactory} used by disco agent.
 * On that part, we need to create a new logger factory implementation here instead of reusing
 * {@code StandardOutputLoggerFactory} because preprocess loads it as part of disco agent via
 * the bootstrap classloader. So, {@code StandardOutputLoggerFactory} cannot be cast to the
 * {@code LoggerFactory} interface which is loaded by the usual application classloader as a
 * normal dependency.
 */
public class PreprocessLoggerFactory implements LoggerFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public Logger createLogger(String name) {
        return new NameAwareStandardOutputLogger(name);
    }

    /**
     *
     */
    private static class NameAwareStandardOutputLogger implements Logger {
        private final String name;

        /**
         *
         * @param name
         */
        public NameAwareStandardOutputLogger(final String name) {
            this.name = "["+name+"] ";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, String message) {
            System.out.println(name + message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, Throwable t) {
            log(level, t.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void log(Level level, String message, Throwable t) {
            log(level, message + "\t" + t.toString());
        }
    }
}
