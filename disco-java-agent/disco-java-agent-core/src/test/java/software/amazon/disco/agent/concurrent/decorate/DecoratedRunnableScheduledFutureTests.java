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

package software.amazon.disco.agent.concurrent.decorate;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DecoratedRunnableScheduledFutureTests {

    @Test
    public void testDecoration() {
        RunnableScheduledFuture r = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture d = DecoratedRunnableScheduledFuture.maybeCreate(r);
        Assert.assertNotEquals(r, d);
        Assert.assertTrue(d instanceof DecoratedRunnableScheduledFuture);
        Assert.assertEquals(r, ((DecoratedRunnableScheduledFuture) d).target);
    }

    @Test
    public void testDoubleDecoration() {
        RunnableScheduledFuture r = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture d = DecoratedRunnableScheduledFuture.maybeCreate(r);
        RunnableScheduledFuture dd = DecoratedRunnableScheduledFuture.maybeCreate(d);
        Assert.assertEquals(d, dd);
    }

    @Test
    public void testNullDecoration() {
        Assert.assertNull(DecoratedRunnableScheduledFuture.maybeCreate(null));
    }

    @Test
    public void testCompareToWithoutUnwrap() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture comparer = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Runnable r = () -> {
        };
        RunnableScheduledFuture dummy = new DummyRunnableScheduledFuture<Void>(r, null);

        comparer.compareTo(dummy);

        //verify DecoratedRunnableScheduledFuture.compareTo propagates to wrapped object
        Mockito.verify(mock, Mockito.times(1)).compareTo(dummy);
    }

    @Test
    public void testCompareToWithUnwrap() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture comparer = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Runnable r = () -> {
        };
        RunnableScheduledFuture dummy = new DummyRunnableScheduledFuture<Void>(r, null);
        RunnableScheduledFuture decoratedDummy = DecoratedRunnableScheduledFuture.maybeCreate(dummy);

        comparer.compareTo(decoratedDummy);

        //DecoratedRunnableScheduledFuture.compareTo(other) should unwrap other if other instanceof DecoratedRunnableScheduledFuture
        //verify compareTo is called with dummy and not decorated
        Mockito.verify(mock, Mockito.times(1)).compareTo(dummy);
    }

    @Test
    public void testIsPeriodic() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.isPeriodic()).thenReturn(false);

        Assert.assertFalse(decorated.isPeriodic());
        Mockito.verify(mock, Mockito.times(1)).isPeriodic();
    }

    @Test
    public void testIsDone() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.isDone()).thenReturn(true);

        Assert.assertTrue(decorated.isDone());
        Mockito.verify(mock, Mockito.times(1)).isDone();
    }

    @Test
    public void testGetDelay() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.getDelay(TimeUnit.NANOSECONDS)).thenReturn(1234L);

        Assert.assertEquals(decorated.getDelay(TimeUnit.NANOSECONDS), 1234);
        Mockito.verify(mock, Mockito.times(1)).getDelay(TimeUnit.NANOSECONDS);
    }

    @Test
    public void testCancel() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.cancel(false)).thenReturn(true);

        Assert.assertTrue(decorated.cancel(false));
        Mockito.verify(mock, Mockito.times(1)).cancel(false);
    }

    @Test
    public void testIsCanceled() {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.isCancelled()).thenReturn(true);

        Assert.assertTrue(decorated.isCancelled());
        Mockito.verify(mock, Mockito.times(1)).isCancelled();
    }

    @Test
    public void testGetNoParams() throws InterruptedException, ExecutionException {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.get()).thenReturn(null);

        Assert.assertNull(decorated.get());
        Mockito.verify(mock, Mockito.times(1)).get();
    }

    @Test
    public void testGetWithParams() throws InterruptedException, ExecutionException, TimeoutException {
        RunnableScheduledFuture mock = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture decorated = DecoratedRunnableScheduledFuture.maybeCreate(mock);

        Mockito.when(mock.get(1234L, TimeUnit.NANOSECONDS)).thenReturn(null);

        Assert.assertNull(decorated.get(1234L, TimeUnit.NANOSECONDS));
        Mockito.verify(mock, Mockito.times(1)).get(1234L, TimeUnit.NANOSECONDS);
    }

    class DummyRunnableScheduledFuture<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {

        DummyRunnableScheduledFuture(Runnable r, V result) {
            super(r, result);
        }

        public boolean isPeriodic() {
            return false;
        }

        public long getDelay(TimeUnit tu) {
            return 0;
        }

        public int compareTo(Delayed d) {
            return 0;
        }
    }
}
