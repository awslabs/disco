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

package software.amazon.disco.agent.integtest;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaCodeTest {

    @BeforeEach
    public void before() {
        TransactionContext.create();
    }

    @AfterEach
    public void after() {
        TransactionContext.destroy();
    }

    @Test
    public void testThatKotlinRunBlockingInJavaWorksCorrectly() throws InterruptedException {
        TransactionContext.putMetadata("test-id", "test-value");

        String result = BuildersKt.runBlocking(
            (CoroutineContext) Dispatchers.getDefault(),
            (coroutineScope, continuation) -> TransactionContext.getMetadata("test-id")
        );

        assertEquals("test-value", result);
    }

    @Test
    public void testThatRunningThreadInJavaWorksCorrectly() throws InterruptedException {
        TransactionContext.putMetadata("test-id", "test-value");
        AtomicBoolean expected = new AtomicBoolean(false);

        Thread thread = new Thread(() ->
            expected.set("test-value".equals(TransactionContext.getMetadata("test-id")))
        );
        thread.start();
        thread.join();

        assertTrue(expected.get());
    }
}
