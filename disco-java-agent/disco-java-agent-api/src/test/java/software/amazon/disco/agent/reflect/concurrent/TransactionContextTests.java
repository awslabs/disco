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

package software.amazon.disco.agent.reflect.concurrent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import software.amazon.disco.agent.reflect.UncaughtExceptionHandler;
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
    public void testRemoveMetadataDoesNotThrowIfReservedIdentifierWhenAgentNotLoaded() {
        TransactionContext.removeMetadata("$amazon.discoIdentifier");
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
        TransactionContext.putMetadata("discoIdentifier", "value");
    }

    @Test
    public void testGetMetadataDoesNotThrowIfReservedIdentifierWhenAgentNotLoaded() {
        TransactionContext.getMetadata("discoIdentifier");
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

        TransactionContext.putMetadata("$amazon.discoIdentifier", new Object());

        UncaughtExceptionHandler.install(null);
        Assert.assertTrue(caught.get() instanceof IllegalArgumentException);
    }

    @Test
    public void testCatchIllegalArgumentExceptionWhenGetMetadataCalledWithReservedIdentifierWhenAgentNotLoaded() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown)-> {
            caught.set(thrown);
        });

        TransactionContext.getMetadata("$amazon.discoIdentifier");

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
