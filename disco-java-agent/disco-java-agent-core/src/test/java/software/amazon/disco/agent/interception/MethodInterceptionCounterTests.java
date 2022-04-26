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

package software.amazon.disco.agent.interception;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.interception.MethodInterceptionCounter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MethodInterceptionCounterTests {
    private MethodInterceptionCounter counter;

    @Before
    public void before() {
        counter = new MethodInterceptionCounter();
    }

    @Test
    public void testReferenceCounter() {
        int methodChained10Times = 10;

        assertEquals(0, counter.localReferenceCounter.get().get());
        assertFalse(counter.hasIntercepted());

        for (int i = 1; i <= methodChained10Times; i++) {
            counter.increment();
            assertEquals(i, counter.localReferenceCounter.get().get());
            assertTrue(counter.hasIntercepted());
        }

        for (int i = methodChained10Times; i > 0; i--) {
            assertEquals(i, counter.localReferenceCounter.get().get());
            assertTrue(counter.hasIntercepted());
            counter.decrement();
        }

        assertEquals(0, counter.localReferenceCounter.get().get());
        assertFalse(counter.hasIntercepted());
    }

    @Test
    public void testDecrementBelowZeroAndIncrement() {
        assertEquals(0, counter.localReferenceCounter.get().get());
        assertFalse(counter.hasIntercepted());

        int minusOne = counter.decrement();
        Assert.assertEquals(-1, minusOne);

        assertEquals(0, counter.localReferenceCounter.get().get());
        assertFalse(counter.hasIntercepted());

        counter.increment();

        assertEquals(1, counter.localReferenceCounter.get().get());
        assertTrue(counter.hasIntercepted());
    }
}