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
