package com.amazon.disco.agent;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class AlphaOneHttpExceptionTest {
    private static final String TEST_MSG = "Test Msg";
    private static final Throwable CAUSE = new NullPointerException();

    @Test
    public void testExceptionConstructionString() {
        AlphaOneHttpException e = new AlphaOneHttpException(TEST_MSG);
        Assert.assertEquals(TEST_MSG, e.getMessage());
    }

    @Test
    public void testExceptionConstructionStringCause() {
        AlphaOneHttpException e = new AlphaOneHttpException(TEST_MSG, CAUSE);
        Assert.assertEquals(TEST_MSG, e.getMessage());
        assertSame(CAUSE, e.getCause());
    }

    @Test
    public void testExceptionConstructionCause() {
        AlphaOneHttpException e = new AlphaOneHttpException(CAUSE);
        assertSame(CAUSE, e.getCause());
    }
}
