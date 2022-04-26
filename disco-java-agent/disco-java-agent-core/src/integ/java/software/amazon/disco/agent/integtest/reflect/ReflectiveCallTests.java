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

package software.amazon.disco.agent.integtest.reflect;

import org.junit.Test;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.integtest.reflect.source.TestEvent;
import software.amazon.disco.agent.integtest.reflect.source.TestListener;
import software.amazon.disco.agent.reflect.ReflectiveCall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReflectiveCallTests {
    private static final String EVENTBUS_CLASS = ".event.EventBus";

    @Test
    public void testAgentIsPresent() {
        assertTrue(ReflectiveCall.isAgentPresent());
    }

    @Test
    public void testAgentIsPresentAfterResettingCache() {
        ReflectiveCall.resetCache();
        assertTrue(ReflectiveCall.isAgentPresent());
    }

    @Test
    public void testReflectiveCallPublishesEvent() {
        TestListener listener = new TestListener();

        ReflectiveCall
            .returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("addListener")
            .withArgTypes(Listener.class)
            .call(listener);

        Event event = new TestEvent("test");

        ReflectiveCall.returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("publish")
            .withArgTypes(Event.class)
            .call(event);

        assertEquals(1, listener.events.size());
        assertEquals(event, listener.events.get(0));
        assertEquals("test", event.getOrigin());

        ReflectiveCall
            .returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("removeAllListeners")
            .call();
    }
}
