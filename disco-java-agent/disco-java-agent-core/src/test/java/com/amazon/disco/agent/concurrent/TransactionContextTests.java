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

package com.amazon.disco.agent.concurrent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tests for the TransactionContext class
 */
public class TransactionContextTests {
    int initializerValue = 0;

    @Before
    public void before() {
        TransactionContext.clear();
    }

    @Test
    public void testInitialValue() {
        Assert.assertEquals(TransactionContext.UNINITIALIZED_TRANSACTION_CONTEXT_VALUE, TransactionContext.get());
    }

    @Test
    public void testCreateDestroy() {
        TransactionContext.putMetadata("foo", "bar");
        TransactionContext.destroy();
        Assert.assertEquals(1, TransactionContext.getPrivateMetadata().size()); //only TransactionId

        TransactionContext.create();
        Assert.assertEquals(1, TransactionContext.getReferenceCounter().get());
        Assert.assertEquals(2, TransactionContext.getPrivateMetadata().size()); //only TransactionId and Ref Counter
        TransactionContext.putMetadata("foo", "bar");
        Assert.assertEquals(3, TransactionContext.getPrivateMetadata().size()); //only TransactionId and Ref Counter and Foobar

        // Excessive destroy calls should still remain as destroyed.
        TransactionContext.destroy();
        TransactionContext.destroy();
        TransactionContext.destroy();
        Assert.assertEquals(1, TransactionContext.getPrivateMetadata().size());

        // Create to represent 3 layers and then destroy 3 should represent clearing.
        TransactionContext.create();
        Assert.assertEquals(2, TransactionContext.getPrivateMetadata().size());
        Assert.assertEquals(1, TransactionContext.getReferenceCounter().get());
        TransactionContext.create();
        TransactionContext.create();
        Assert.assertEquals(3, TransactionContext.getReferenceCounter().get());
        TransactionContext.destroy();
        TransactionContext.destroy();
        TransactionContext.destroy();
        Assert.assertEquals(1, TransactionContext.getPrivateMetadata().size()); //only TransactionId

        TransactionContext.create();
        TransactionContext.create();
        TransactionContext.create();
        TransactionContext.create();
        Assert.assertEquals(4, TransactionContext.getReferenceCounter().get());

        // Clear should wipe out the context.
        TransactionContext.clear();
        Assert.assertEquals(1, TransactionContext.getPrivateMetadata().size()); //only TransactionId

    }

    @Test
    public void testCreate() {
        TransactionContext.putMetadata("foo", "bar");
        TransactionContext.create();
        Assert.assertNotEquals(TransactionContext.UNINITIALIZED_TRANSACTION_CONTEXT_VALUE, TransactionContext.get());
        Assert.assertEquals(2, TransactionContext.getPrivateMetadata().size()); //only TransactionId in map and ref counter
    }

    @Test
    public void testSetValue() {
        TransactionContext.set("foo");
        Assert.assertEquals("foo", TransactionContext.get());
    }

    @Test
    public void testClear() {
        TransactionContext.set("bar");
        TransactionContext.clear();
        testInitialValue();
    }

    @Test
    public void testWithinCreatedContext() {
        TransactionContext.create();
        Assert.assertTrue(TransactionContext.isWithinCreatedContext());
    }

    @Test
    public void testOutsideServiceActivity() {
        Assert.assertFalse(TransactionContext.isWithinCreatedContext());
    }

    @Test
    public void testMetadata() {
        TransactionContext.putMetadata("foo", "bar");
        String bar = String.class.cast(TransactionContext.getMetadata("foo"));
        Assert.assertEquals("bar", bar);
    }

    @Test
    public void testTaggedMetadata() {
        TransactionContext.putMetadata("foo1", "bar1");
        TransactionContext.putMetadata("foo2", "bar2");
        TransactionContext.setMetadataTag("foo1", "tag1");
        TransactionContext.setMetadataTag("foo2", "tag2");
        Map<String, Object> taggedMap = TransactionContext.getMetadataWithTag("tag1");
        Assert.assertEquals(1, taggedMap.size());
        Assert.assertEquals(true, taggedMap.containsKey("foo1"));
        Assert.assertEquals(false, taggedMap.containsKey("foo2"));
        TransactionContext.clearMetadataTag("foo1", "tag1");
        Map<String, Object> taggedMap2 = TransactionContext.getMetadataWithTag("tag1");
        Assert.assertEquals(0, taggedMap2.size());
    }

    @Test
    public void testGetPrivateMetadata() {
        TransactionContext.putMetadata("foo", "bar");
        ConcurrentMap<String, MetadataItem> metadata = TransactionContext.getPrivateMetadata();
        Assert.assertEquals(TransactionContext.UNINITIALIZED_TRANSACTION_CONTEXT_VALUE, metadata.get(TransactionContext.TRANSACTION_ID_KEY).get());
        Assert.assertEquals("bar", metadata.get("foo").get());
    }

    @Test
    public void testSetPrivateMetadata() {
        ConcurrentMap<String, MetadataItem> metadata = new ConcurrentHashMap<>();
        metadata.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("foo"));
        metadata.put("key", new MetadataItem("value"));
        TransactionContext.setPrivateMetadata(metadata);
        Assert.assertEquals("foo", TransactionContext.get());
        Assert.assertEquals("value", TransactionContext.getMetadata("key"));
    }

    @Test
    public void testMetadataSharedAcrossThreadBoundary() throws Exception {
        ConcurrentMap<String, MetadataItem> metadata = new ConcurrentHashMap<>();
        metadata.put(TransactionContext.TRANSACTION_ID_KEY, new MetadataItem("foo"));
        metadata.put("key", new MetadataItem("value"));
        TransactionContext.setPrivateMetadata(metadata);
        Thread child = new Thread(() -> {
            TransactionContext.setPrivateMetadata(metadata);
            TransactionContext.putMetadata("key", "value2");
        });
        child.start();
        child.join();
        Assert.assertEquals("value2", TransactionContext.getMetadata("key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutMetadataThrowsWithIllegalIdentifier() {
        TransactionContext.putMetadata("discoTransactionId", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMetadataThrowsWithIllegalIdentifier() {
        TransactionContext.getMetadata("discoTransactionId");
    }
}
