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

package software.amazon.disco.agent.integtest.concurrent.source;

import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import org.junit.Assert;

import java.util.concurrent.locks.ReentrantLock;


public class TestableConcurrencyObjectImpl implements TestableConcurrencyObject {
    private boolean concurrencyMethodWasCalled;
    private Long beforeThreadId;
    private String beforeTransactionId;
    private String beforeMetadataValue;
    private Long executionThreadId;
    private String executionTransactionId;
    private String executionMetadataValue;
    protected Throwable thrown;
    private static final String transactionIdPrefix = "transactionId-";
    private static final String metadataKey = "abstractTestableConcurrencyObjectMetadata";
    private static final String metadataValue = "value";
    private static final ReentrantLock lock = new ReentrantLock();

    private TestListener listener;

    public TestableConcurrencyObjectImpl() {
        reset();
    }

    public static void before() {
        //prevent any tests running in parallel with each other, since global state is used.
        lock.lock();
        TransactionContext.create();
        TransactionContext.set(transactionIdPrefix+TransactionContext.get());
        TransactionContext.putMetadata(metadataKey, metadataValue);
    }

    public static void after() {
        TransactionContext.clear();
        lock.unlock();
    }

    @Override
    public void reset() {
        concurrencyMethodWasCalled = false;
        executionThreadId = null;
        executionTransactionId = null;
        executionMetadataValue = null;
        thrown = null;
        listener = null;
    }

    @Override
    public void testBeforeInvocation() {
        EventBus.addListener(listener = new TestListener());
        this.beforeThreadId = Thread.currentThread().getId();
        this.beforeTransactionId = TransactionContext.get();
        this.beforeMetadataValue = String.class.cast(TransactionContext.getMetadata(metadataKey));
        Assert.assertFalse(concurrencyMethodWasCalled);
        Assert.assertNull(executionThreadId);
        Assert.assertNull(executionTransactionId);
        Assert.assertNull(executionMetadataValue);
    }

    @Override
    public void testAfterConcurrentInvocation() {
        if (executionThreadId.equals(beforeThreadId)) {
            throw new ConcurrencyCanBeRetriedException();
        }

        listener.testConcurrent();
        if (thrown != null) {
            throw new RuntimeException(thrown);
        }
        Assert.assertTrue(concurrencyMethodWasCalled);
        Assert.assertNotEquals(executionThreadId, beforeThreadId);
        Assert.assertEquals(beforeTransactionId, executionTransactionId);
        Assert.assertEquals(executionMetadataValue, metadataValue);
        EventBus.removeListener(listener);
    }

    @Override
    public void testAfterSingleThreadedInvocation() {
        listener.testSingleThreaded();
        if (thrown != null) {
            throw new RuntimeException(thrown);
        }
        Assert.assertTrue(concurrencyMethodWasCalled);
        Assert.assertEquals(beforeThreadId, executionThreadId);
        Assert.assertEquals(beforeTransactionId, executionTransactionId);
        Assert.assertEquals(executionMetadataValue, metadataValue);
        EventBus.removeListener(listener);
    }

    @Override
    public void perform() {
        concurrencyMethodWasCalled = true;
        //if this is the first time, or if so far a collection of work has been single threaded
        if (executionThreadId == null || executionThreadId.equals(beforeThreadId)) {
            executionTransactionId = TransactionContext.get();
            executionMetadataValue = String.class.cast(TransactionContext.getMetadata(metadataKey));
            executionThreadId = Thread.currentThread().getId();
        }
    }

    class TestListener implements Listener {
        Event threadEnter;
        Event threadExit;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ThreadEnterEvent) {
                threadEnter = e;
            }
            if (e instanceof ThreadExitEvent) {
                threadExit = e;
            }
        }

        void testSingleThreaded() {
            Assert.assertNull(threadEnter);
            Assert.assertNull(threadExit);
        }

        void testConcurrent() {
            Assert.assertNotNull(threadEnter);
            Assert.assertNotNull(threadExit);
            Assert.assertTrue(threadEnter instanceof ThreadEnterEvent);
            Assert.assertTrue(threadExit instanceof ThreadExitEvent);
        }
    }

    public static class WhichThrows extends TestableConcurrencyObjectImpl {
        public class TestableConcurrencyObjectException extends RuntimeException {}

        @Override
        public void perform() {
            super.perform();
            throw new TestableConcurrencyObjectException();
        }
    }
}
