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

package com.amazon.disco.agent.customize.event;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.Listener;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EventBusTests {
    @Test
    public void testPublishWhenAlphaOneNotLoaded() {
        EventBus.publish(Mockito.mock(Event.class));
    }

    @Test
    public void testAddListenerWhenAlphaOneNotLoaded() {
        EventBus.addListener(Mockito.mock(Listener.class));
    }

    @Test
    public void testRemoveListenerWhenAlphaOneNotLoaded() {
        EventBus.removeListener(Mockito.mock(Listener.class));
    }

    @Test
    public void testRemoveAllListenersWhenAlphaOneNotLoaded() {
        EventBus.removeAllListeners();
    }

    @Test
    public void testIsListenerPresentWhenAlphaOneNotLoaded() {
        Listener listener = Mockito.mock(Listener.class);
        EventBus.addListener(listener);
        Assert.assertFalse(EventBus.isListenerPresent(listener));
    }
}
