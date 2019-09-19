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

import com.amazon.disco.agent.logging.Logger;
import com.amazon.disco.agent.logging.LoggerFactory;

/**
 * By default, DiSCo has a null implementation of logging, to ensure that its behavior is safe and optimal by default.
 * Service owners may want to install a LoggerFactory of their own which redirects to e.g. their own logging solution such
 * as log4j, but as an example LoggerFactory, or as a quick way to get logging visible during tests or debugging, this
 * implementation can be installed, which will just direct all output to System.out.
 */
public class StandardOutputLoggerFactory implements LoggerFactory {

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
