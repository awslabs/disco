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