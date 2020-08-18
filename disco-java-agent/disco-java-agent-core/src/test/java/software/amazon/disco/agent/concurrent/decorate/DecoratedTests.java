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

package software.amazon.disco.agent.concurrent.decorate;

import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DecoratedTests {
    MyListener listener;

    @Before
    public void before() {
        EventBus.addListener(listener = new MyListener());
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(listener);
    }

    @Test
    public void testBeforeSameThread() {
        Decorated d = new MyDecorated();
        d.before();
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testAfterSameThread() {
        Decorated d = new MyDecorated();
        d.after();
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testBeforeDifferentThread() {
        Decorated d = new MyDecorated();
        d.ancestralThreadId = -1L;
        d.before();
        Assert.assertTrue(listener.enter instanceof ThreadEnterEvent);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testAfterDifferentThread() {
        Decorated d = new MyDecorated();
        d.ancestralThreadId = -1L;
        d.after();
        Assert.assertNull(listener.enter);
        Assert.assertTrue(listener.exit instanceof ThreadExitEvent);
    }

    static class MyDecorated extends Decorated {

    }

    static class MyListener implements Listener {
        Event enter = null;
        Event exit = null;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ThreadEnterEvent) {
                enter = e;
            } else if (e instanceof ThreadExitEvent) {
                exit = e;
            }
        }
    }
}
