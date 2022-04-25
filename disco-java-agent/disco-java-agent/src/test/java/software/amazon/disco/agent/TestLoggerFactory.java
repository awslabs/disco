/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent;

import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A logger to spy on what the Agent is logging for testing agent deduplication
 */
public class TestLoggerFactory implements LoggerFactory {
    public static final TestLogger INSTANCE = new TestLogger();
    @Override
    public Logger createLogger(String name) {
        return INSTANCE;
    }

    public static class TestLogger implements Logger {
        private boolean found = false;
        public List<String> messages = new ArrayList<>();

        @Override
        public void log(Level level, String message) {
            //catch the first log.warn(), which we assume to be the log produced by agent deduplication
            if (!found && level.equals(Level.WARN)) {
                messages.add(message);
                found = true;
            }
        }

        @Override
        public void log(Level level, Throwable t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, String message, Throwable t) {
            throw new UnsupportedOperationException();
        }

        public void reset() {
            found = false;
        }
    }
}