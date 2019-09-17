package com.amazon.disco.agent.concurrent;

import org.junit.Assert;
import org.junit.Test;

public class MetadataItemTests {

    @Test
    public void testCreateMetadataItem() {
        MetadataItem metadataItem = new MetadataItem("foo");
        Assert.assertEquals("foo",String.class.cast(metadataItem.get()) );
    }

    @Test
    public void testCreateTag() {
        MetadataItem metadataItem = new MetadataItem("foo");
        Assert.assertEquals(metadataItem.getTags().size(), 0);
        metadataItem.setTag("bar");
        Assert.assertEquals(metadataItem.getTags().size(), 1);
        Assert.assertEquals(metadataItem.hasTag("bar"), true);
    }

    @Test
    public void testClearTag() {
        MetadataItem metadataItem = new MetadataItem("foo");
        Assert.assertEquals(metadataItem.getTags().size(), 0);
        metadataItem.setTag("bar1");
        metadataItem.setTag("bar2");
        Assert.assertEquals(metadataItem.getTags().size(), 2);
        Assert.assertEquals(metadataItem.hasTag("bar1"), true);
        metadataItem.clearTag("bar1");
        Assert.assertEquals(metadataItem.getTags().size(), 1);
        Assert.assertEquals(metadataItem.hasTag("bar1"), false);
        Assert.assertEquals(metadataItem.hasTag("bar2"), true);
    }

}