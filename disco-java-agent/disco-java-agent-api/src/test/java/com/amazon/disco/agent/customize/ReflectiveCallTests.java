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

package com.amazon.disco.agent.customize;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

public class ReflectiveCallTests {
    @Test
    public void testAgentIsNotPresent() {
        Assert.assertFalse(ReflectiveCall.isAgentPresent());
    }

    @Test
    public void testSetUncaughtExceptionHandler() {
        UncaughtExceptionHandler handler = Mockito.mock(UncaughtExceptionHandler.class);
        ReflectiveCall.installUncaughtExceptionHandler(handler);
        Assert.assertEquals(handler, ReflectiveCall.uncaughtExceptionHandler);
        ReflectiveCall.installUncaughtExceptionHandler(null);
        Assert.assertNull(ReflectiveCall.uncaughtExceptionHandler);
    }

    @Test
    public void testBeanGetters() {
        ReflectiveCall call = ReflectiveCall.returning(Long.class)
                .ofClass(".Foo")
                .ofMethod("Bar")
                .withArgTypes(Integer.class);
        Assert.assertEquals(Long.class, call.getReturnType());
        Assert.assertEquals("com.amazon.disco.agent.Foo", call.getClassName());
        Assert.assertEquals("Bar", call.getMethodName());
        Assert.assertEquals(Integer.class, call.getArgTypes()[0]);
        Assert.assertEquals(1, call.getArgTypes().length);
    }

    @Test
    public void testToString() {
        ReflectiveCall call = ReflectiveCall.returning(Long.class)
                .ofClass(".package.Foo")
                .ofMethod("Bar")
                .withArgTypes(Integer.class, Boolean.class);
        Assert.assertEquals("java.lang.Long com.amazon.disco.agent.package.Foo::Bar(java.lang.Integer, java.lang.Boolean)", call.toString());
    }

    @Test
    public void testMethodFound() {
        ReflectiveCall reflectiveCall = ReflectiveCall.returningVoid()
                .ofClass(".customize.ReflectiveCallTests")
                .ofMethod("testMethod");
        Assert.assertTrue(reflectiveCall.methodFound());
    }

    @Test
    public void testReflectiveCallDispatchesException() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        ReflectiveCall.returningVoid()
                .ofClass(".customize.ReflectiveCallTests")
                .ofMethod("testMethod")
                .call();

        Assert.assertTrue(caught.get() instanceof RuntimeException);
        Assert.assertEquals("Test", caught.get().getMessage());
    }

    @Test
    public void testIllegalAccessIsSafe() {
        Object result = ReflectiveCall.returning(Object.class)
                .ofClass(".customize.ReflectiveCallTests")
                .ofMethod("privateTestMethod")
                .call();
        Assert.assertNull(result);
    }

    //methods to call reflectively, just for the test
    public static void testMethod() {
        throw new RuntimeException("Test");
    }
    private static Object privateTestMethod() {
        return new Object();
    }
}
