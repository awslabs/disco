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

package software.amazon.disco.agent.integtest.concurrent;

import software.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObject;
import software.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObjectImpl;
import software.amazon.disco.agent.integtest.concurrent.source.ForceConcurrency;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ParallelStreamTests {
    static private final List<String> list = Arrays.asList("0", "1", "2", "3");
    private TestableConcurrencyObject testableConcurrencyObject;

    @Rule
    public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

    @Before
    public void before() {
        testableConcurrencyObject = new TestableConcurrencyObjectImpl();
        TestableConcurrencyObjectImpl.before();
        testableConcurrencyObject.testBeforeInvocation();
    }

    @After
    public void after() {
        testableConcurrencyObject.testAfterConcurrentInvocation();
        TestableConcurrencyObjectImpl.after();
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
        testableConcurrencyObject.perform();
    }

}
