package com.amazon.disco.agent.customize.concurrent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.amazon.disco.agent.customize.UncaughtExceptionHandler;
import org.junit.Assert;
import org.junit.Test;

public class TransactionContextTests {
    @Test
    public void testTransactionContextCreateNoAgentLoaded() {
        TransactionContext.create();
    }

    @Test
    public void testTransactionContextSetNoAgentLoaded() {
        TransactionContext.set("foo");
        Assert.assertNull(TransactionContext.get());
    }

    @Test
    public void testTransactionContextGetNoAgentLoaded() {
        Assert.assertEquals(null, TransactionContext.get());
    }

    @Test
    public void testTransactionContextSetMetadataNoAgentLoaded() {
        TransactionContext.putMetadata("metadata", "value");
        Assert.assertNull(TransactionContext.getMetadata("metadata"));
    }

    @Test
    public void testTransactionContextGetMetadataNoAgentLoaded() {
        Assert.assertNull(TransactionContext.getMetadata("metadata"));
    }

    @Test
    public void testTransactionContextSetMetadataTagNoAgentLoaded() {
        TransactionContext.putMetadata("identifier", "test_value");
        TransactionContext.setMetadataTag("identifier", "custom_tag");

        final Map<String, Object> metadata = TransactionContext.getMetadataWithTag("custom_tag");

        Assert.assertNotNull(metadata);
        Assert.assertNull(metadata.get("identifier"));
    }

    @Test
    public void testTransactionContextClearNoAgentLoaded() {
        TransactionContext.clear();
        Assert.assertNull(TransactionContext.get());
    }

    @Test
    public void testPutMetadataDoesNotThrowIfReservedIdentifierWhenAgentNotLoaded() {
        TransactionContext.putMetadata("alphaOneIdentifier", "value");
    }

    @Test
    public void testGetMetadataDoesNotThrowIfReservedIdentifierWhenAgentNotLoaded() {
        TransactionContext.getMetadata("alphaOneIdentifier");
    }

    @Test
    public void testGetUninitializedTransactionContextValueWhenAgentNotLoaded() {
        TransactionContext.getUninitializedTransactionContextValue();
    }

    @Test
    public void testCatchIllegalArgumentExceptionWhenPutMetadataCalledWithReservedIdentifierWhenAgentNotLoaded() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        TransactionContext.putMetadata("alphaOneIdentifier", new Object());

        UncaughtExceptionHandler.install(null);
        Assert.assertTrue(caught.get() instanceof IllegalArgumentException);
    }

    @Test
    public void testCatchIllegalArgumentExceptionWhenGetMetadataCalledWithReservedIdentifierWhenAgentNotLoaded() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        TransactionContext.getMetadata("alphaOneIdentifier");

        UncaughtExceptionHandler.install(null);
        Assert.assertTrue(caught.get() instanceof IllegalArgumentException);
    }

    @Test
    public void testNoIllegalArgumentExceptionWhenPutMetadataCalledWithLegalIdentifierWhenAgentNotLoaded() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        TransactionContext.putMetadata("legalIdentifier", new Object());

        UncaughtExceptionHandler.install(null);
        Assert.assertNull(caught.get());
    }

    @Test
    public void testNoIllegalArgumentExceptionWhenGetMetadataCalledWithLegalIdentifierWhenAgentNotLoaded() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        TransactionContext.getMetadata("legalIdentifier");

        UncaughtExceptionHandler.install(null);
        Assert.assertNull(caught.get());
    }

    @Test
    public void testIsWithinCreatedContextWhenAgentNotLoaded() {
        TransactionContext.isWithinCreatedContext();
    }
}
