package com.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;

public class TransactionEventTests {
    @Test
    public void testTransactionBeginEvent() {
        TransactionEvent event = new TransactionBeginEvent("Origin");
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(TransactionEvent.Operation.BEGIN, event.getOperation());
    }

    @Test
    public void testTransactionEndEvent() {
        TransactionEvent event = new TransactionEndEvent("Origin");
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(TransactionEvent.Operation.END, event.getOperation());
    }
}
