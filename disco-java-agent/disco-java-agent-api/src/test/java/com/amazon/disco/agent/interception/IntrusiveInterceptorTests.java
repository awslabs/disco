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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class IntrusiveInterceptorTests {
    @Test
    public void testCreateContinuationContextAsContinueWithEvent() {
        Event e = Mockito.mock(Event.class);
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asContinue(e);
        Assert.assertNull(c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertEquals(c.getDecision(), IntrusiveInterceptor.Decision.CONTINUE);
    }

    @Test
    public void testCreateContinuationContextAsContinueWithEventAndUserdata() {
        Event e = Mockito.mock(Event.class);
        Object userData = new Object();
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asContinue(e, userData);
        Assert.assertEquals(userData, c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertEquals(c.getDecision(), IntrusiveInterceptor.Decision.CONTINUE);
    }

    @Test
    public void testCreateContinuationContextAsReplaceWithEvent() {
        Event e = Mockito.mock(Event.class);
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asReplace(e);
        Assert.assertNull(c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertEquals(c.getDecision(), IntrusiveInterceptor.Decision.REPLACE);
    }

    @Test
    public void testCreateContinuationContextAsReplaceWithEventAndUserdata() {
        Event e = Mockito.mock(Event.class);
        Object userData = new Object();
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asReplace(e, userData);
        Assert.assertEquals(userData, c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertTrue(c.getDecision().equals(IntrusiveInterceptor.Decision.REPLACE));
    }

    @Test
    public void testCreateContinuationContextAsTransformWithEvent() {
        Event e = Mockito.mock(Event.class);
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asTransform(e);
        Assert.assertNull(c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertTrue(c.getDecision().equals(IntrusiveInterceptor.Decision.TRANSFORM));
    }

    @Test
    public void testCreateContinuationContextAsTransformWithEventAndUserdata() {
        Event e = Mockito.mock(Event.class);
        Object userData = new Object();
        IntrusiveInterceptor.ContinuationContext c = IntrusiveInterceptor.ContinuationContext.asTransform(e, userData);
        Assert.assertEquals(userData, c.getUserData());
        Assert.assertEquals(e, c.getEvent());
        Assert.assertTrue(c.getDecision().equals(IntrusiveInterceptor.Decision.TRANSFORM));
    }

    @Test(expected = IllegalStateException.class)
    public void testIntrusiveInterceptorReplaceDefaultThrows() throws Throwable {
        class TestIntrusiveInterceptor implements IntrusiveInterceptor {
            @Override
            public ContinuationContext decide(Event event) {
                return ContinuationContext.asReplace(event);
            }
        }
        IntrusiveInterceptor i = new TestIntrusiveInterceptor();
        i.replace(i.decide(Mockito.mock(Event.class)));
    }

    @Test
    public void testIntrusiveInterceptorTransformDefaultWithoutThrowableReturns() throws Throwable {
        class TestIntrusiveInterceptor implements IntrusiveInterceptor {
            @Override
            public ContinuationContext decide(Event event) {
                return ContinuationContext.asTransform(event);
            }
        }
        IntrusiveInterceptor i = new TestIntrusiveInterceptor();
        Object result = new Object();
        Assert.assertEquals(result, i.transform(i.decide(Mockito.mock(Event.class)), result, null));
    }

    @Test(expected = IllegalStateException.class)
    public void testIntrusiveInterceptorTransformDefaultWithThrowableThrows() throws Throwable {
        class TestIntrusiveInterceptor implements IntrusiveInterceptor {
            @Override
            public ContinuationContext decide(Event event) {
                return ContinuationContext.asTransform(event);
            }
        }
        IntrusiveInterceptor i = new TestIntrusiveInterceptor();
        Object result = new Object();
        Throwable expected = new IllegalStateException();
        Assert.assertEquals(result, i.transform(i.decide(Mockito.mock(Event.class)), result, expected));
    }
}
