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

package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.event.Event;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;


public class IntrusiveInterceptorRegistryTests {
    @Before
    public void before() {
        IntrusiveInterceptorRegistry.uninstall();
    }

    @After
    public void after() {
        IntrusiveInterceptorRegistry.uninstall();
    }

    @Test
    public void testInstallAndGet() {
        IntrusiveInterceptor i = Mockito.mock(IntrusiveInterceptor.class);
        Assert.assertEquals(IntrusiveInterceptorRegistry.DefaultIntrusiveInterceptor.class, IntrusiveInterceptorRegistry.install(i).getClass());
        Assert.assertEquals(i, IntrusiveInterceptorRegistry.get());
    }

    @Test
    public void testIntrudeWhenDefaultInterceptorInstalled() throws Throwable {
        Assert.assertTrue(IntrusiveInterceptorRegistry.get() instanceof IntrusiveInterceptorRegistry.DefaultIntrusiveInterceptor);
        Object result = new Object();
        Callable c = ()-> result;
        Object actualResult = IntrusiveInterceptorRegistry.intrude(Mockito.mock(Event.class), c);
        Assert.assertEquals(result, actualResult);
    }

    @Test
    public void testDefaultIntrusiveInterceptorContinues() {
        Assert.assertEquals(new IntrusiveInterceptorRegistry.DefaultIntrusiveInterceptor().decide(
                Mockito.mock(Event.class)).getDecision(), IntrusiveInterceptor.Decision.CONTINUE);
    }

    @Test
    public void testNullContext() throws Throwable {
        Object result = new Object();
        Callable c = ()-> result;
        Event e = Mockito.mock(Event.class);
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(null, null, null);
        IntrusiveInterceptorRegistry.install(interceptor);
        Object actualResult = IntrusiveInterceptorRegistry.intrude(e, c);
        Assert.assertEquals(result, actualResult);
        Assert.assertFalse(interceptor.testableOutput.replaceCalled);
        Assert.assertFalse(interceptor.testableOutput.transformCalled);
    }

    @Test
    public void testIntrudeWhenContinue() throws Throwable {
        Object result = new Object();
        Callable c = ()-> result;
        Event e = Mockito.mock(Event.class);
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.CONTINUE, null, null);
        IntrusiveInterceptorRegistry.install(interceptor);
        Object actualResult = IntrusiveInterceptorRegistry.intrude(e, c);
        Assert.assertEquals(result, actualResult);
        Assert.assertFalse(interceptor.testableOutput.replaceCalled);
        Assert.assertFalse(interceptor.testableOutput.transformCalled);
    }

    @Test
    public void testIntrudeWhenContinueThrows() {
        Throwable thrown = null;
        Exception toThrow = new RuntimeException();
        Callable c = ()-> {throw toThrow;};
        Event e = Mockito.mock(Event.class);
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.CONTINUE, null, null);
        IntrusiveInterceptorRegistry.install(interceptor);
        try {
            IntrusiveInterceptorRegistry.intrude(e, c);
        } catch (Throwable t) {
            thrown = t;
        }
        Assert.assertEquals(toThrow, thrown);
        Assert.assertFalse(interceptor.testableOutput.replaceCalled);
        Assert.assertFalse(interceptor.testableOutput.transformCalled);
    }

    @Test
    public void testIntrudeWhenReplace() throws Throwable {
        AtomicBoolean callableCalled = new AtomicBoolean(false);
        Event e = Mockito.mock(Event.class);
        Object result = new Object();
        Object userData = new Object();
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.REPLACE, result, userData);
        IntrusiveInterceptorRegistry.install(interceptor);
        Object actualResult = IntrusiveInterceptorRegistry.intrude(e, ()->{
            callableCalled.set(true);
            return null;
        });
        Assert.assertEquals(result, actualResult);
        Assert.assertFalse(callableCalled.get());
        Assert.assertTrue(interceptor.testableOutput.replaceCalled);
        Assert.assertFalse(interceptor.testableOutput.transformCalled);
        Assert.assertEquals(interceptor.testableOutput.context.getDecision(), IntrusiveInterceptor.Decision.REPLACE);
        Assert.assertEquals(e, interceptor.testableOutput.context.getEvent());
        Assert.assertEquals(userData, interceptor.testableOutput.context.getUserData());
    }

    @Test
    public void testIntrudeWhenReplaceThrows() throws Throwable {
        AtomicBoolean callableCalled = new AtomicBoolean(false);
        Throwable thrown = null;
        Exception toThrow = new RuntimeException();
        Event e = Mockito.mock(Event.class);
        Object result = new Object();
        Object userData = new Object();
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.REPLACE, result, userData);
        interceptor.replaceShouldThrow = toThrow;
        IntrusiveInterceptorRegistry.install(interceptor);
        try {
            IntrusiveInterceptorRegistry.intrude(e, () -> {
                callableCalled.set(true);
                return null;
            });
        } catch (Throwable t) {
            thrown = t;
        }
        Assert.assertFalse(callableCalled.get());
        Assert.assertTrue(interceptor.testableOutput.replaceCalled);
        Assert.assertFalse(interceptor.testableOutput.transformCalled);
        Assert.assertEquals(interceptor.testableOutput.context.getDecision(), IntrusiveInterceptor.Decision.REPLACE);
        Assert.assertEquals(thrown, toThrow);
        Assert.assertEquals(e, interceptor.testableOutput.context.getEvent());
        Assert.assertEquals(userData, interceptor.testableOutput.context.getUserData());
    }

    @Test
    public void testIntrudeWhenTransform() throws Throwable {
        AtomicBoolean callableCalled = new AtomicBoolean(false);
        Event e = Mockito.mock(Event.class);
        Object originalResult = new Object();
        Object result = new Object();
        Object userData = new Object();
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.TRANSFORM, result, userData);
        IntrusiveInterceptorRegistry.install(interceptor);
        Object actualResult = IntrusiveInterceptorRegistry.intrude(e, () -> {
            callableCalled.set(true);
            return originalResult;
        });
        Assert.assertEquals(result, actualResult);
        Assert.assertTrue(callableCalled.get());
        Assert.assertFalse(interceptor.testableOutput.replaceCalled);
        Assert.assertTrue(interceptor.testableOutput.transformCalled);
        Assert.assertEquals(originalResult, interceptor.testableOutput.realOutput);
        Assert.assertNull(interceptor.testableOutput.thrown);
        Assert.assertEquals(interceptor.testableOutput.context.getDecision(), IntrusiveInterceptor.Decision.TRANSFORM);
        Assert.assertEquals(e, interceptor.testableOutput.context.getEvent());
        Assert.assertEquals(userData, interceptor.testableOutput.context.getUserData());
    }

    @Test
    public void testIntrudeWhenTransformThrows() {
        AtomicBoolean callableCalled = new AtomicBoolean(false);
        Event e = Mockito.mock(Event.class);
        Object result = new Object();
        Object userData = new Object();
        Exception toThrow = new RuntimeException();
        Throwable thrown = null;
        TestIntrusiveInterceptor interceptor =
                new TestIntrusiveInterceptor(IntrusiveInterceptor.Decision.TRANSFORM, result, userData);
        IntrusiveInterceptorRegistry.install(interceptor);
        try {
            IntrusiveInterceptorRegistry.intrude(e, () -> {
                callableCalled.set(true);
                throw toThrow;
            });
        } catch (Throwable t) {
            thrown = t;
        }
        Assert.assertEquals(thrown, toThrow);
        Assert.assertTrue(callableCalled.get());
        Assert.assertFalse(interceptor.testableOutput.replaceCalled);
        Assert.assertTrue(interceptor.testableOutput.transformCalled);
        Assert.assertEquals(toThrow, interceptor.testableOutput.thrown);
        Assert.assertEquals(interceptor.testableOutput.context.getDecision(), IntrusiveInterceptor.Decision.TRANSFORM);
        Assert.assertEquals(e, interceptor.testableOutput.context.getEvent());
        Assert.assertEquals(userData, interceptor.testableOutput.context.getUserData());
    }


    static class TestIntrusiveInterceptor implements IntrusiveInterceptor {
        private final Decision desiredDecision;
        private final Object desiredOutput;
        private final Object userData;
        Throwable replaceShouldThrow = null;
        class TestableOutput {
            boolean transformCalled = false;
            boolean replaceCalled = false;
            ContinuationContext context = null;
            Object realOutput = null;
            Throwable thrown = null;
        }
        TestableOutput testableOutput;

        TestIntrusiveInterceptor(Decision desiredDecision, Object desiredOutput, Object userData) {
            this.desiredDecision = desiredDecision;
            this.desiredOutput = desiredOutput;
            this.userData = userData;
            testableOutput = new TestableOutput();
        }

        @Override
        public ContinuationContext decide(Event event) {
            if(desiredDecision == null) {
                return null;
            }
            if (desiredDecision.equals(Decision.CONTINUE)) {
                return ContinuationContext.asContinue(event, userData);
            } else if (desiredDecision.equals(Decision.REPLACE)) {
                return ContinuationContext.asReplace(event, userData);
            } else {
                return ContinuationContext.asTransform(event, userData);
            }
        }

        @Override
        public Object replace(ContinuationContext context) throws Throwable {
            testableOutput.replaceCalled =true;
            testableOutput.context = context;
            if (replaceShouldThrow != null) throw replaceShouldThrow;
            return desiredOutput;
        }

        @Override
        public Object transform(ContinuationContext context, Object realOutput, Throwable thrown) throws Throwable {
            testableOutput.transformCalled = true;
            testableOutput.context = context;
            testableOutput.realOutput = realOutput;
            testableOutput.thrown = thrown;
            if (thrown != null) throw thrown;
            return desiredOutput;
        }
    }

}

