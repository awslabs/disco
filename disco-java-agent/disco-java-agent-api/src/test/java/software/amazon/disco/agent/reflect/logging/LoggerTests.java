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

import software.amazon.disco.agent.logging.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LoggerTests {
    @Test
    public void testCoreLoggerDebug() {
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.debug("test");
        Assert.assertEquals(software.amazon.disco.agent.logging.Logger.Level.DEBUG, resultHolder.wasCalledWithLevel);
        Assert.assertEquals("DiSCo(Reflect) test", resultHolder.wasCalledWithMessage);
    }

    @Test
    public void testCoreLoggerInfo() {
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.info("test");
        Assert.assertEquals(software.amazon.disco.agent.logging.Logger.Level.INFO, resultHolder.wasCalledWithLevel);
        Assert.assertEquals("DiSCo(Reflect) test", resultHolder.wasCalledWithMessage);
    }

    @Test
    public void testCoreLoggerWarn() {
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.warn("test");
        Assert.assertEquals(software.amazon.disco.agent.logging.Logger.Level.WARN, resultHolder.wasCalledWithLevel);
        Assert.assertEquals("DiSCo(Reflect) test", resultHolder.wasCalledWithMessage);
    }

    @Test
    public void testCoreLoggerError() {
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.error("test");
        Assert.assertEquals(software.amazon.disco.agent.logging.Logger.Level.ERROR, resultHolder.wasCalledWithLevel);
        Assert.assertEquals("DiSCo(Reflect) test", resultHolder.wasCalledWithMessage);
    }

    @Test
    public void testCoreLoggerDebugWhenSuppressed() {
        Logger.suppressIfAgentNotPresent(true);
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.debug("test");
        Assert.assertNull(resultHolder.wasCalledWithLevel);
        Assert.assertNull(resultHolder.wasCalledWithMessage);
        Logger.suppressIfAgentNotPresent(false);
    }


    @Test
    public void testCoreLoggerInfoWhenSuppressed() {
        Logger.suppressIfAgentNotPresent(true);
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.info("test");
        Assert.assertNull(resultHolder.wasCalledWithLevel);
        Assert.assertNull(resultHolder.wasCalledWithMessage);
        Logger.suppressIfAgentNotPresent(false);
    }


    @Test
    public void testCoreLoggerWarnWhenSuppressed() {
        Logger.suppressIfAgentNotPresent(true);
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.warn("test");
        Assert.assertNull(resultHolder.wasCalledWithLevel);
        Assert.assertNull(resultHolder.wasCalledWithMessage);
        Logger.suppressIfAgentNotPresent(false);
    }

    @Test
    public void testCoreLoggerErrorWhenSuppressed() {
        Logger.suppressIfAgentNotPresent(true);
        ResultHolder resultHolder = testCoreLoggerHelper();
        Logger.error("test");
        Assert.assertNull(resultHolder.wasCalledWithLevel);
        Assert.assertNull(resultHolder.wasCalledWithMessage);
        Logger.suppressIfAgentNotPresent(false);
    }

    @Test
    public void testInstallLoggerFactoryWhenDiscoNotLoaded() {
        Logger.installLoggerFactory(Mockito.mock(LoggerFactory.class));
    }

    private ResultHolder testCoreLoggerHelper() {
        ResultHolder resultHolder = new ResultHolder();

        class TestLogger implements software.amazon.disco.agent.logging.Logger {
            @Override
            public void log(Level level, String message) {
                resultHolder.wasCalledWithLevel = level;
                resultHolder.wasCalledWithMessage = message;
            }

            @Override
            public void log(Level level, Throwable t) {}

            @Override
            public void log(Level level, String message, Throwable t) {}
        }

        Logger.log = new TestLogger();
        return resultHolder;
    }

    private static class ResultHolder {
        software.amazon.disco.agent.logging.Logger.Level wasCalledWithLevel;
        String wasCalledWithMessage;
    }
}
