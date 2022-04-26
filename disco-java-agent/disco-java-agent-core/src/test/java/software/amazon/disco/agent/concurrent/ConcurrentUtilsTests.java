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

package software.amazon.disco.agent.concurrent;

import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentUtilsTests {
    private MyListener listener;

    @Before
    public void before() {
        TransactionContext.clear();
        EventBus.addListener(listener = new MyListener());
    }

    @After
    public void after() {
        EventBus.removeListener(listener);
        TransactionContext.clear();
    }

    @Test
    public void testEnter() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        transactionContext.put("foo", new MetadataItem("bar"));
        ConcurrentUtils.enter(-1, 0, transactionContext);
        Assert.assertEquals("bar", TransactionContext.getMetadata("foo"));
        Assert.assertEquals(1, listener.received.size());
        Event event = listener.received.iterator().next();
        Assert.assertEquals(ThreadEnterEvent.class, event.getClass());
        ThreadEvent threadEvent = (ThreadEvent)event;
        Assert.assertEquals(0, threadEvent.getParentId());
        Assert.assertEquals(Thread.currentThread().getId(), threadEvent.getChildId());
    }

    @Test
    public void testExit() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        TransactionContext.set("id");
        TransactionContext.putMetadata("foo", "bar");
        ConcurrentUtils.exit(-1, 0, TransactionContext.getPrivateMetadata(), false);
        Assert.assertEquals("id", TransactionContext.get());
        Assert.assertEquals("bar", TransactionContext.getMetadata("foo"));
        Assert.assertEquals(1, listener.received.size());
        Event event = listener.received.iterator().next();
        Assert.assertEquals(ThreadExitEvent.class, event.getClass());
        ThreadEvent threadEvent = (ThreadEvent)event;
        Assert.assertEquals(0, threadEvent.getParentId());
        Assert.assertEquals(Thread.currentThread().getId(), threadEvent.getChildId());
    }

    @Test
    public void testExitAndRemove() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        TransactionContext.set("id");
        TransactionContext.putMetadata("foo", "bar");
        ConcurrentUtils.exit(-1, 0, TransactionContext.getPrivateMetadata(), true);
        Assert.assertEquals("disco_null_id", TransactionContext.get());
        Assert.assertNull(TransactionContext.getMetadata("foo"));
        Assert.assertEquals(1, listener.received.size());
        Event event = listener.received.iterator().next();
        Assert.assertEquals(ThreadExitEvent.class, event.getClass());
        ThreadEvent threadEvent = (ThreadEvent)event;
        Assert.assertEquals(0, threadEvent.getParentId());
        Assert.assertEquals(Thread.currentThread().getId(), threadEvent.getChildId());
    }

    @Test
    public void testEnterWithNullContext() {
        ConcurrentUtils.enter(-1, 0, null);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testExitWithNullContext() {
        ConcurrentUtils.exit(-1, 0, null, false);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testEnterWithDefaultContext() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem(TransactionContext.getUninitializedTransactionContextValue()));
        ConcurrentUtils.enter(-1, 0, transactionContext);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testExitWithDefaultContext() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem(TransactionContext.getUninitializedTransactionContextValue()));
        ConcurrentUtils.exit(-1, 0, transactionContext, false);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testEnterWithSameThreadId() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        ConcurrentUtils.enter(Thread.currentThread().getId(), 0, transactionContext);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testExitWithSameThreadId() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        ConcurrentUtils.exit(Thread.currentThread().getId(), 0, transactionContext, false);
        Assert.assertTrue(listener.received.isEmpty());
    }

    class MyListener implements Listener {
        Set<Event> received = new HashSet<>();
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event event) {
            received.add(event);
        }
    }
}
