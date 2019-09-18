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

package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.event.EventBus;
import com.amazon.disco.agent.event.ThreadEnterEvent;
import com.amazon.disco.agent.event.ThreadExitEvent;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;

import java.util.concurrent.ConcurrentMap;

/**
 * Utility methods for copying DiSCo propagation metadata, with checks for safety and redundancy.
 */
public class ConcurrentUtils {
    private static Logger log = LogManager.getLogger(ConcurrentUtils.class);

    /**
     * Propagate the transaction context, if running in a different thread than at construction time
     * @param parentThreadId the threadId of the thread which created the object being passed across thread boundary
     * @param discoTransactionContext the parent's TransactionContext map.
     */
    public static void set(long parentThreadId, ConcurrentMap<String, MetadataItem> discoTransactionContext) {
        if (discoTransactionContext == null) {
            log.error("DiSCo(Core) could not propagate null context from thread id " + parentThreadId + " to thread id " + Thread.currentThread().getId());
            return;
        }

        if (Thread.currentThread().getId() != parentThreadId && !isDiscoNullId(discoTransactionContext)) {
            TransactionContext.setPrivateMetadata(discoTransactionContext);
            EventBus.publish(new ThreadEnterEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
        }
    }

    /**
     * Clear the transaction context, if running in a different thread than at construction time
     * @param parentThreadId the threadId of the thread which created the object being passed across thread boundary
     * @param discoTransactionContext the parent's TransactionContext map.
     */
    public static void clear(long parentThreadId, ConcurrentMap<String, MetadataItem> discoTransactionContext) {
        if (discoTransactionContext == null) {
            return;
        }

        if (Thread.currentThread().getId() != parentThreadId && !isDiscoNullId(discoTransactionContext)) {
            EventBus.publish(new ThreadExitEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
            TransactionContext.clear();
        }
    }

    /**
     * Check if the incoming transaction context contains a null id, indicating that it is not parented to an
     * Activity/Request/Transaction - i.e. it might be background state such as a worker. In these situations
     * we don't issue events, or manage TransactionContext content.
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
