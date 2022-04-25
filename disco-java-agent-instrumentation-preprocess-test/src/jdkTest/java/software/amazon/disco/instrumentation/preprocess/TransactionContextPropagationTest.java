/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.util.concurrent.atomic.AtomicReference;

public class TransactionContextPropagationTest {
    private static final String TX_KEY = "key";
    private static final String TX_VALUE = "value";

    @Before
    public void before() {
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.destroy();
    }

    @Test
    public void testTransactionContextPropagationWithExplicitThreadCreation() throws Throwable {
        AtomicReference<Throwable> exceptionCaught = new AtomicReference<>();

        TransactionContext.putMetadata(TX_KEY, TX_VALUE);

        Thread worker = new Thread(() -> {
            try {
                Assert.assertTrue(TransactionContext.isWithinCreatedContext());
                Assert.assertEquals(TX_VALUE, TransactionContext.getMetadata(TX_KEY));
            } catch (Throwable t) {
                // exceptions thrown in background threads aren't picked up by JUnit, storing exceptions encountered in a variable for later validation
                exceptionCaught.set(t);
            }
        });

        worker.start();
        worker.join();

        if (exceptionCaught.get() != null) {
            throw exceptionCaught.get();
        }
    }
}
