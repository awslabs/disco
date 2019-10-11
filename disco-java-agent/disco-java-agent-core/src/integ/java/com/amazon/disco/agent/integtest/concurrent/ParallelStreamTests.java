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

package com.amazon.disco.agent.integtest.concurrent;

import com.amazon.disco.agent.integtest.concurrent.source.ConcurrencyCanBeRetriedException;
import com.amazon.disco.agent.reflect.concurrent.TransactionContext;
import com.amazon.disco.agent.integtest.concurrent.source.ForceConcurrency;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ParallelStreamTests {
    static private final List<String> list = Arrays.asList("0", "1", "2", "3");
    private long parentThreadId;
    private Set<Long> threadIds;

    @Rule
    public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

    @Before
    public void before() {
        parentThreadId = Thread.currentThread().getId();
        threadIds = new HashSet<>();
        threadIds.add(parentThreadId);
        TransactionContext.clear();
        TransactionContext.set("foo");
        TransactionContext.putMetadata("metadata", "value");
    }

    @After
    public void after() {
        testConcurrency();
        TransactionContext.clear();
    }

    @Test
    public void testForEachNonLambda() {
        list.parallelStream().forEach(new StringConsumer());
    }

    @Test
    public void testForEachLambda() {
        list.parallelStream().forEach((s) -> impl(s));
    }

    @Test
    public void testForEachMethodReference() {
        list.parallelStream().forEach(this::impl);
    }

    class StringConsumer implements Consumer<String> {
        @Override
        public void accept(String s) {
            impl(s);
        }
    }

    private void impl(String s) {
        threadIds.add(Thread.currentThread().getId());
        Assert.assertEquals("foo", TransactionContext.get());
        Assert.assertEquals("value", TransactionContext.getMetadata("metadata"));
    }

    private void testConcurrency() {
        //as long as something was parallelized, there will be more than 1 threadId in this set
        if(threadIds.size() == 1) {
            throw new ConcurrencyCanBeRetriedException();
        }
    }
}
