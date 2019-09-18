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

import com.amazon.disco.agent.interception.Installable;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ConcurrencySupportTests {
    @Test
    public void testPackageContentCorrect() {
        List<Installable> installables = (List<Installable>)new ConcurrencySupport().get();
        Assert.assertEquals(5, installables.size());
        Assert.assertEquals(ExecutorInterceptor.class, installables.get(0).getClass());
        Assert.assertEquals(ForkJoinPoolInterceptor.class, installables.get(1).getClass());
        Assert.assertEquals(ForkJoinTaskInterceptor.class, installables.get(2).getClass());
        Assert.assertEquals(ThreadInterceptor.class, installables.get(3).getClass());
        Assert.assertEquals(ThreadSubclassInterceptor.class, installables.get(4).getClass());
    }
}
