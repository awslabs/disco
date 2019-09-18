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

package com.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;

public class ThreadEventTests {
    @Test
    public void testThreadEnterEvent() {
        ThreadEvent event = new ThreadEnterEvent("Origin", 1L, 2L);
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(1L, event.getParentId());
        Assert.assertEquals(2L, event.getChildId());
        Assert.assertEquals(ThreadEvent.Operation.ENTERING, event.getOperation());
    }

    @Test
    public void testThreadExitEvent() {
        ThreadEvent event = new ThreadExitEvent("Origin", 1L, 2L);
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(1L, event.getParentId());
        Assert.assertEquals(2L, event.getChildId());
        Assert.assertEquals(ThreadEvent.Operation.EXITING, event.getOperation());
    }
}
