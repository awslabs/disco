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

package software.amazon.disco.agent.reflect.logging;

import software.amazon.disco.agent.logging.Logger;
import org.junit.Test;

public class StandardOutputLoggerFactoryTests {
    @Test
    public void testLoggerCreationIsSafe() {
        software.amazon.disco.agent.logging.Logger logger = new StandardOutputLoggerFactory().createLogger("name");
    }

    @Test
    public void testLogMessageIsSafe() {
        software.amazon.disco.agent.logging.Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info("message");
    }

    @Test
    public void testLogThrowableIsSafe() {
        software.amazon.disco.agent.logging.Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info(new RuntimeException());
    }

    @Test
    public void testLogThrowableWithMessageIsSafe() {
        Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info("message", new RuntimeException());
    }
}
