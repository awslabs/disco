package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.event.EventBus;
import com.amazon.disco.agent.event.ThreadEnterEvent;
import com.amazon.disco.agent.event.ThreadExitEvent;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;

import java.util.concurrent.ConcurrentMap;

/**
 * Utility methods for copying AlphaOne propagation metadata, with checks for safety and redundancy.
 */
public class ConcurrentUtils {
    private static Logger log = LogManager.getLogger(ConcurrentUtils.class);

    /**
     * Propagate the transaction context, if running in a different thread than at construction time
     * @param parentThreadId the threadId of the thread which created the object being passed across thread boundary
     * @param alphaOneTransactionContext the parent's TransactionContext map.
     */
    public static void set(long parentThreadId, ConcurrentMap<String, MetadataItem> alphaOneTransactionContext) {
        if (alphaOneTransactionContext == null) {
            log.error("AlphaOne(Core) could not propagate null context from thread id " + parentThreadId + " to thread id " + Thread.currentThread().getId());
            return;
        }

        if (Thread.currentThread().getId() != parentThreadId && !isAlphaOneNullId(alphaOneTransactionContext)) {
            TransactionContext.setPrivateMetadata(alphaOneTransactionContext);
            EventBus.publish(new ThreadEnterEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
        }
    }

    /**
     * Clear the transaction context, if running in a different thread than at construction time
     * @param parentThreadId the threadId of the thread which created the object being passed across thread boundary
     * @param alphaOneTransactionContext the parent's TransactionContext map.
     */
    public static void clear(long parentThreadId, ConcurrentMap<String, MetadataItem> alphaOneTransactionContext) {
        if (alphaOneTransactionContext == null) {
            return;
        }

        if (Thread.currentThread().getId() != parentThreadId && !isAlphaOneNullId(alphaOneTransactionContext)) {
            EventBus.publish(new ThreadExitEvent("Concurrency", parentThreadId, Thread.currentThread().getId()));
            TransactionContext.clear();
        }
    }

    /**
     * Check if the incoming transaction context contains a null id, indicating that it is not parented to an
     * Activity/Request/Transaction - i.e. it might be background state such as a worker. In these situations
     * we don't issue events, or manage TransactionContext content.
     * @param alphaOneTransactionContext the context to check
     * @return true if it is the sentinel value for a null ID, indicating a non-parented thread handoff.
     */
    private static boolean isAlphaOneNullId(ConcurrentMap<String, MetadataItem> alphaOneTransactionContext) {
        MetadataItem item = alphaOneTransactionContext.get(TransactionContext.TRANSACTION_ID_KEY);
        if (item == null) {
            return false;
        }
        return TransactionContext.getUninitializedTransactionContextValue().equals(item.get());
    }
}