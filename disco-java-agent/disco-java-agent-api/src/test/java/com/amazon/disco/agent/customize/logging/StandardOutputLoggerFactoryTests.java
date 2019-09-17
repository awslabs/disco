package com.amazon.disco.agent.customize.logging;

import com.amazon.disco.agent.logging.Logger;
import org.junit.Test;

public class StandardOutputLoggerFactoryTests {
    @Test
    public void testLoggerCreationIsSafe() {
        Logger logger = new StandardOutputLoggerFactory().createLogger("name");
    }

    @Test
    public void testLogMessageIsSafe() {
        Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info("message");
    }

    @Test
    public void testLogThrowableIsSafe() {
        Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info(new RuntimeException());
    }

    @Test
    public void testLogThrowableWithMessageIsSafe() {
        Logger logger = new StandardOutputLoggerFactory().createLogger("name");
        logger.info("message", new RuntimeException());
    }
}
