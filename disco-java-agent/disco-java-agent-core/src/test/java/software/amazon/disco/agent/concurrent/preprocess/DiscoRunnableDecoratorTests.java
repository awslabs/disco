/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.concurrent.preprocess;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DiscoRunnableDecoratorTests {
    @Before
    public void before() {
        DiscoRunnableDecorator.maybeDecorateFunction = null;
    }

    @Test
    public void testMaybeDecorateDecoratesReturnsDecoratedRunnable_whenDecorateFunctionIsNotNull() {
        AtomicBoolean removeTXValue = new AtomicBoolean(false);

        DiscoRunnableDecorator.maybeDecorateFunction = (Runnable target, Boolean removeTX) -> {
            removeTXValue.set(removeTX);
            return DecoratedRunnable.maybeCreate(target, removeTX);
        };
        Runnable runnable = Mockito.mock(Runnable.class);

        Runnable decorated = DiscoRunnableDecorator.maybeDecorate(runnable);

        assertTrue(decorated instanceof DecoratedRunnable);
        assertTrue(removeTXValue.get());
    }

    @Test
    public void testMaybeDecorateDecoratesReturnsRunnable_whenDecorateFunctionThrowsException() {
        DiscoRunnableDecorator.maybeDecorateFunction = (Runnable target, Boolean removeTX) -> {
            throw new RuntimeException();
        };
        Runnable runnable = Mockito.mock(Runnable.class);

        Runnable result = DiscoRunnableDecorator.maybeDecorate(runnable);

        assertFalse(result instanceof DecoratedRunnable);
        assertTrue(result instanceof Runnable);
    }

    @Test
    public void testMaybeDecorateDecoratesReturnsRunnable_whenDecorateFunctionIsNull() {
        Runnable runnable = Mockito.mock(Runnable.class);
        assertNull(DiscoRunnableDecorator.maybeDecorateFunction);

        Runnable result = DiscoRunnableDecorator.maybeDecorate(runnable);

        assertFalse(result instanceof DecoratedRunnable);
        assertTrue(result instanceof Runnable);
    }
}
