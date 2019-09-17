package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.EventBus;
import com.amazon.disco.agent.event.Listener;
import com.amazon.disco.agent.event.ThreadEnterEvent;
import com.amazon.disco.agent.event.ThreadExitEvent;
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
    public void testSet() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        transactionContext.put("foo", new MetadataItem("bar"));
        ConcurrentUtils.set(-1, transactionContext);
        Assert.assertEquals("bar", TransactionContext.getMetadata("foo"));
        Assert.assertEquals(1, listener.received.size());
        Assert.assertEquals(ThreadEnterEvent.class, listener.received.iterator().next().getClass());
    }

    @Test
    public void testClear() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        transactionContext.put("foo", new MetadataItem("bar"));
        ConcurrentUtils.clear(-1, transactionContext);
        Assert.assertNull(TransactionContext.getMetadata("foo"));
        Assert.assertEquals(1, listener.received.size());
        Assert.assertEquals(ThreadExitEvent.class, listener.received.iterator().next().getClass());
    }

    @Test
    public void testSetWithNullContext() {
        ConcurrentUtils.set(-1, null);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testClearWithNullContext() {
        ConcurrentUtils.clear(-1, null);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testSetWithDefaultContext() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem(TransactionContext.getUninitializedTransactionContextValue()));
        ConcurrentUtils.set(-1, transactionContext);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testClearWithDefaultContext() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem(TransactionContext.getUninitializedTransactionContextValue()));
        ConcurrentUtils.clear(-1, transactionContext);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testSetWithSameThreadId() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        ConcurrentUtils.set(Thread.currentThread().getId(), transactionContext);
        Assert.assertTrue(listener.received.isEmpty());
    }

    @Test
    public void testClearWithSameThreadId() {
        ConcurrentMap<String, MetadataItem> transactionContext = new ConcurrentHashMap<>();
        transactionContext.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("id"));
        ConcurrentUtils.clear(Thread.currentThread().getId(), transactionContext);
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
