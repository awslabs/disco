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

package software.amazon.disco.agent.concurrent;

import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.util.concurrent.ConcurrentMap;

/**
 * Utility methods for copying DiSCo propagation metadata, with checks for safety and redundancy.
 */
public class ConcurrentUtils {
    private static Logger log = LogManager.getLogger(ConcurrentUtils.class);

    /**
     * Signals that a worker thread is about to perform some assigned work. Transaction context should therefore be propagated to the
     * thread in question when appropriate.
     *
     * @param parentThreadId          the threadId of the thread which created the object being passed across thread boundary
     * @param discoTransactionContext the parent's TransactionContext map.
     */
    public static void enter(long parentThreadId, ConcurrentMap<String, MetadataItem> discoTransactionContext) {
        if (discoTransactionContext == null) {
            log.error("DiSCo(Core) could not propagate null context from parent thread id " + parentThreadId + " to thread id " + Thread.currentThread().getId());
            return;
        }

        if (!isDiscoNullId(discoTransactionContext)) {
            TransactionContext.setPrivateMetadata(discoTransactionContext);
            EventBus.publish(new ThreadEnterEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
        }
    }

    /**
     * Signals that a sub-thread is exiting by publishing a ThreadExitEvent as a pair with the ThreadEnterEvent emitted from enter().
     *
     * To avoid unnecessary work, the TransactionContext itself is not cleared or removed unless explicitly requested. Since the thread's
     * business logic is exiting at this time, the TX can be safely left in place, as nothing can reach it and inspect it.
     *
     * If this is the real end of the lifetime of a Thread, the Thread and Thread subclass interceptors will request removal of the ThreadLocal
     * TransactionContext for garbage collection. If a reusable 'pooled' thread the TransactionContext will be refreshed to the correct state the
     * next time this thread is subject to a call to enter() instead.
     *
     * @param parentThreadId           the threadId of the thread which created the object being passed across thread boundary
     * @param discoTransactionContext  the parent's TransactionContext map.
     * @param removeTransactionContext true if the TransactionContext ThreadLocal data should be removed for garbage collection
     */
    public static void exit(long parentThreadId, ConcurrentMap<String, MetadataItem> discoTransactionContext, boolean removeTransactionContext) {
        if (discoTransactionContext == null) {
            return;
        }

        if (!isDiscoNullId(discoTransactionContext)) {
            EventBus.publish(new ThreadExitEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
        }

        if (removeTransactionContext) {
            TransactionContext.remove();
        }
    }

    /**
     * Check if the incoming transaction context contains a null id, indicating that it is not parented to an
     * Activity/Request/Transaction - i.e. it might be background state such as a worker. In these situations
     * we don't issue events, or manage TransactionContext content.
     *
     * @param discoTransactionContext the context to check
     * @return true if it is the sentinel value for a null ID, indicating a non-parented thread handoff.
     */
    private static boolean isDiscoNullId(ConcurrentMap<String, MetadataItem> discoTransactionContext) {
        MetadataItem item = discoTransactionContext.get(TransactionContext.TRANSACTION_ID_KEY);
        if (item == null) {
            return false;
        }
        return TransactionContext.getUninitializedTransactionContextValue().equals(item.get());
    }
}
