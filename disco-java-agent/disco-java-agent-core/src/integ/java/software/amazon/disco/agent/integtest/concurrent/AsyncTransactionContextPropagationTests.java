package software.amazon.disco.agent.integtest.concurrent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.event.AbstractThreadEvent;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncTransactionContextPropagationTests {
    private static int TIMEOUT_VALUE = 10;
    ExecutorService executorService;
    TestListener listener;

    @Before
    public void before() {
        listener = new TestListener();
        EventBus.addListener(listener);
    }

    @After
    public void after() {
        EventBus.removeListener(listener);

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @AfterClass
    public static void afterAll() {
        TransactionContext.clear();
    }

    /**
     * A simple test to validate the expected behaviour when a particular thread enters the same transaction twice as shown by the
     * diagram below where 't1' denotes the thread in question and 'tx1' represents a particular Disco transaction.
     * t1: ...[tx1....]....[tx1...]....
     * <p>
     * This is achieved by submitting an 'OuterCallable' that in turn submits a subsequent 'InnerCallable' to a pool with only 1 worker thread, thus guarantying
     * that the intended thread will re-enter the same transaction.
     * <p>
     * In addition, this test also validates that a particular thread can re-enter a transaction that it has previously created and destroyed.
     */
    @Test
    public void testSimpleTXPropagationInAsyncWorkflow() throws ExecutionException, InterruptedException, TimeoutException {
        /**
         * A Callable that will be submitted to a pool of 1 worker thread.
         */
        class OuterCallable implements Callable<CompletableFuture<Boolean>> {
            Boolean txPropagated = new Boolean(false);

            @Override
            public CompletableFuture call() {
                // create a TX inside a worker thread so that the TX is owned by the worker thread.
                TransactionContext.create();
                String expectedTXId = TransactionContext.get();
                long txOwningThread = Thread.currentThread().getId();

                CompletableFuture<Boolean> innerFuture = new CompletableFuture<>();

                /**
                 * A Callable to be submitted to the same pool of 1 worker thread. The 'InnerCallable' will only start its execution after
                 * the 'OuterCallable' returns.
                 */
                class InnerCallable implements Callable {
                    @Override
                    public Object call() {
                        // verify that the parked TX has been propagated to its original creator.
                        if (expectedTXId.equals(TransactionContext.get()) && txOwningThread == Thread.currentThread().getId()) {
                            txPropagated = true;
                        }

                        innerFuture.complete(txPropagated);
                        return null;
                    }
                }

                // Create and submit an inner callable to the same pool. The inner callable is created within the outer callable to create
                // a situation where a worker thread re-enters a TX that it has previously created and destroyed.
                executorService.submit(new InnerCallable());

                // destroy the TX which effectively replaces the current TX with an uninitialized once since the counter would reach 0.
                TransactionContext.destroy();
                assertEquals(TransactionContext.getUninitializedTransactionContextValue(), TransactionContext.get());

                return innerFuture;
            }
        }

        prepareThreadPool(1);

        // This will not trigger any thread events published because no transaction has been created.
        Future<CompletableFuture<Boolean>> executorServiceFuture = executorService.submit(new OuterCallable());

        // The first 'get()' waits for the 'OuterCallable' finish. The second 'get()' waits for the 'InnerCallable' to finish
        // where TX propagation is validated.
        Boolean txPropagated = executorServiceFuture
                .get(TIMEOUT_VALUE, TimeUnit.SECONDS)
                .get(TIMEOUT_VALUE, TimeUnit.SECONDS);

        executorService.shutdown();
        executorService.awaitTermination(TIMEOUT_VALUE, TimeUnit.SECONDS);

        // check that TX propagation was successful, in other words, the worker thread was able to re-enter a transaction that it has
        // previously created and destroyed.
        assertTrue(txPropagated);

        // assert the correct number of thread events were published in the expected order.
        assertEquals(2, listener.eventList.size());
        assertTrue(listener.eventList.get(0) instanceof ThreadEnterEvent);
        assertTrue(listener.eventList.get(1) instanceof ThreadExitEvent);
    }

    /**
     * A slightly more complex test case where a particular transaction is being entered by 2 threads multiple times as shown in the diagram below.
     * The test is divided into 3 stages, delimited by the '|' symbol.
     * <p>
     * Note that the blocking tasks will also inherit the same transaction context due to limitations of the Disco API which doesn't support
     * clearing transaction context explicitly.
     * <p>
     * A special type of tasked called 'BlockingCallable' is used to guarantee that the submitted task can be assigned to the intended thread.
     * <p>
     * t1: ...[tx1..].|.[Block].|.[tx1..]..
     * t2: ...[Block].|.[tx1..].|.[Block]..
     */
    @Test
    public void testComplexTXPropagationInAsyncWorkflow() throws InterruptedException, ExecutionException, TimeoutException {
        prepareThreadPool(2);
        TransactionContext.create();

        // A list of expected thread events in order to be gradually added below.
        // These thread pseudo-events have its 'equals' method overridden to compare parent thread id, child thread id and operation
        // of actual thread events published by disco core.
        List<ThreadIdComparableEvent> expectedThreadEventsOrdered = new ArrayList<>();

        // Stage 1.0: submit a callable, that will block 1 of the 2 worker threads, to the thread pool. Whichever thread gets assigned will be labeled as t2.
        ThreadContextAwareCallable.BlockingCallable blockingCallable_t2_first = new ThreadContextAwareCallable.BlockingCallable();
        Future blockingFuture_t2_first = submitBlockingTask(blockingCallable_t2_first);
        long t2_id = blockingCallable_t2_first.threadId;
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t2_id));

        // Stage 1.1: submit a non-blocking task to the pool to be picked up by t1
        ThreadContextAwareCallable.NonBlockingCallable nonBlockingCallable_t1_first = new ThreadContextAwareCallable.NonBlockingCallable();
        executorService.submit(nonBlockingCallable_t1_first).get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        long t1_id = nonBlockingCallable_t1_first.threadId;
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t1_id));
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t1_id));


        // Stage 2.0: t1 should've finished executing the assigned task. t1 will now receive a blocking task
        ThreadContextAwareCallable.BlockingCallable blockingCallable_t1 = new ThreadContextAwareCallable.BlockingCallable();
        Future blockingFuture_t1 = submitBlockingTask(blockingCallable_t1);
        assertEquals(t1_id, blockingCallable_t1.threadId);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t1_id));

        // Stage 2.1: t1 is now blocked. Unblock t2
        blockingCallable_t2_first.unblock();
        blockingFuture_t2_first.get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t2_id));

        // Stage 2.2: assign a non-blocking task to t2 and wait for the execution to finish
        ThreadContextAwareCallable.NonBlockingCallable nonBlockingCallable_t2 = new ThreadContextAwareCallable.NonBlockingCallable();
        executorService.submit(nonBlockingCallable_t2).get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        assertEquals(t2_id, nonBlockingCallable_t2.threadId);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t2_id));
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t2_id));


        // Stage 3.0: Now block t2 so t1 is guaranteed to be entered next.
        ThreadContextAwareCallable.BlockingCallable blockingCallable_t2_second = new ThreadContextAwareCallable.BlockingCallable();
        Future blockingFuture_t2_second = submitBlockingTask(blockingCallable_t2_second);
        assertEquals(t2_id, blockingCallable_t2_second.threadId);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t2_id));

        // Stage 3.1: t2 is blocked, unblock t1
        blockingCallable_t1.unblock();
        blockingFuture_t1.get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t1_id));

        // Stage 3.2: submit a non-blocking task to t1
        ThreadContextAwareCallable.NonBlockingCallable nonBlockingCallable_t1_second = new ThreadContextAwareCallable.NonBlockingCallable();
        executorService.submit(nonBlockingCallable_t1_second).get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        assertEquals(t1_id, nonBlockingCallable_t1_second.threadId);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadEnterEvent(t1_id));
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t1_id));


        // Final stage: all expected transaction entering should have been completed. Wait for all tasks to fully execute.
        blockingCallable_t2_second.unblock();
        blockingFuture_t2_second.get(TIMEOUT_VALUE, TimeUnit.SECONDS);
        expectedThreadEventsOrdered.add(new ThreadIdComparableEvent.ThreadExitEvent(t2_id));

        executorService.shutdown();
        executorService.awaitTermination(TIMEOUT_VALUE, TimeUnit.SECONDS);

        TransactionContext.destroy();

        // assert the correct number of expected events were added
        assertEquals(12, expectedThreadEventsOrdered.size());

        // assert that all received thread events are of the correct type and contains the correct data.
        assertArrayEquals(expectedThreadEventsOrdered.toArray(), listener.eventList.toArray());
    }

    /**
     * Helper method to create and submit a 'BlockingCallable' to the pool.
     */
    private Future submitBlockingTask(ThreadContextAwareCallable.BlockingCallable callable) throws InterruptedException {
        CountDownLatch waitForBlockingTaskToGetAssignedLock = new CountDownLatch(1);
        callable.setWaitForBlockingTaskToGetAssignedLatch(waitForBlockingTaskToGetAssignedLock);

        Future future = executorService.submit(callable);
        waitForBlockingTaskToGetAssignedLock.await(TIMEOUT_VALUE, TimeUnit.SECONDS);

        return future;
    }

    /**
     * Create a new pool with the requested size. Warm up the pool by submitting dummy tasks to trigger worker threads creation so that related side-effects
     * will not impact test results.
     * <p>
     * For instance, a long-running 'ThreadPoolExecutor.Worker' thread responsible for orchestrating tasks, which in turn will result
     * in the creation of a ThreadEnterEvent. This thread event is irrelevant to what's being tested and thus should be ignored.
     *
     * {@link ForkJoinPool} is being used here as the implementation of executor service because {@link ThreadExitEvent} resulted from
     * executing a previously submitted task is guaranteed to be published before the associated future is marked as complete. This is especially
     * important when asserting the order of thread events published and is not always guaranteed for other implementations such as
     * {@link java.util.concurrent.ThreadPoolExecutor}.
     */
    private void prepareThreadPool(int size) throws InterruptedException {
        executorService = new ForkJoinPool(size);
        CountDownLatch latch = new CountDownLatch(size);

        List<Callable<Object>> runnables = new ArrayList();
        for (int i = 0; i < size; i++) {
            runnables.add(() -> {
                latch.countDown();
                latch.await();
                return null;
            });
        }
        executorService.invokeAll(runnables);
    }

    public static class TestListener implements Listener {
        public LinkedList<Event> eventList = new LinkedList();
        public AtomicInteger enterEventsReceived = new AtomicInteger();
        public AtomicInteger exitEventsReceived = new AtomicInteger();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof AbstractThreadEvent) {
                eventList.add(e);
                if (e instanceof ThreadEnterEvent) {
                    enterEventsReceived.getAndIncrement();
                } else if (e instanceof ThreadExitEvent) {
                    exitEventsReceived.getAndIncrement();
                }
            }
        }
    }

    public static abstract class ThreadContextAwareCallable implements Callable<String> {
        public long threadId;

        @Override
        public String call() {
            this.threadId = Thread.currentThread().getId();
            return doCall();
        }

        abstract String doCall();

        public static class BlockingCallable extends ThreadContextAwareCallable {
            private CountDownLatch waitForThreadToBeReadyToReceiveNewWorkLatch = new CountDownLatch(1);
            private CountDownLatch waitForBlockingTaskToGetAssignedLatch;

            public void setWaitForBlockingTaskToGetAssignedLatch(CountDownLatch waitForBlockingTaskToGetAssignedLatch) {
                this.waitForBlockingTaskToGetAssignedLatch = waitForBlockingTaskToGetAssignedLatch;
            }

            @Override
            public String doCall() {
                waitForBlockingTaskToGetAssignedLatch.countDown();
                try {
                    // block until whichever thread this callable was assigned to is ready to receive another task.
                    waitForThreadToBeReadyToReceiveNewWorkLatch.await(TIMEOUT_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            public void unblock() {
                waitForThreadToBeReadyToReceiveNewWorkLatch.countDown();
            }
        }

        public static class NonBlockingCallable extends ThreadContextAwareCallable {
            @Override
            public String doCall() {
                return null;
            }
        }
    }

    /**
     * A pseudo-event capable of asserting that a foreign thread event published by disco core is of the expected type and
     * contains the correct data. This is done by overriding the 'equals()' method of the pseudo-event so that statements such as
     * 'assertEquals(pseudoEnterEvent, actualEnterEvent)' will just work as intended out of the box.
     */
    public static class ThreadIdComparableEvent {
        private long expectedChildThreadId;
        private long expectedParentThreadId;
        private ThreadEvent.Operation operation;

        public ThreadIdComparableEvent(long expectedChildThreadId, ThreadEvent.Operation operation) {
            // The expected parent id is constant since all tasks are submitted from the main thread.
            this.expectedParentThreadId = Thread.currentThread().getId();
            this.expectedChildThreadId = expectedChildThreadId;
            this.operation = operation;
        }

        public static class ThreadEnterEvent extends ThreadIdComparableEvent {
            public ThreadEnterEvent(long expectedThreadId) {
                super(expectedThreadId, ThreadEvent.Operation.ENTERING);
            }
        }

        public static class ThreadExitEvent extends ThreadIdComparableEvent {
            public ThreadExitEvent(long expectedThreadId) {
                super(expectedThreadId, ThreadEvent.Operation.EXITING);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AbstractThreadEvent) {
                AbstractThreadEvent abstractThreadEvent = ((AbstractThreadEvent) obj);
                return this.expectedChildThreadId == abstractThreadEvent.getChildId()
                        && this.expectedParentThreadId == abstractThreadEvent.getParentId()
                        && this.operation.equals(abstractThreadEvent.getOperation());
            }
            return false;
        }
    }
}
