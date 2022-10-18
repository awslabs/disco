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
import software.amazon.disco.agent.event.TransactionBeginEvent;
import software.amazon.disco.agent.event.TransactionEndEvent;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.util.FastIdGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A thread local map of arbitrary data.
 * By default contains a Transaction ID that we can use to identify the set of service inputs and downstream interactions that
 * make up the complete 'closure' of a given request to the service-under-test.
 *
 * Optionally, this structure can also be populated with metadata by clients.
 */
public class TransactionContext {
    private static Logger log = LogManager.getLogger(TransactionContext.class);

    static final String TRANSACTION_ID_KEY = "$amazon.discoTransactionId";
    public static final String TRANSACTION_OWNING_THREAD_KEY = "$amazon.discoTransactionOwningThreadId";
    public static final String UNINITIALIZED_TRANSACTION_CONTEXT_VALUE = "disco_null_id";
    private static final String REFERENCE_COUNTER_KEY = "$amazon.discoRefCounterKey";
    private static final TransactionContextFactory TRANSACTION_CONTEXT_FACTORY = new TransactionContextFactory();
    private static final ThreadLocal<ConcurrentMap<String, MetadataItem>> transactionContext = ThreadLocal.withInitial(TRANSACTION_CONTEXT_FACTORY);

    /**
     * This class was created to solve a null pointer exception when deploying a service using a statically instrumented JDK. The TransactionContext
     * along with other dependency classes to enable concurrency support has to be injected to the java.base module for Java 9+ and loaded while the
     * JVM is still bootstrapping itself by initializing primordial classes such as Thread.
     *
     * At this stage of the program execution, the JVM is unable to handle lambda expressions such as the one passed to {@link ThreadLocal#withInitial(Supplier)}.
     * To remedy this shortcoming, a class that explicitly extends {@link Supplier} has been implemented and initialized and used to populate {@link #transactionContext}
     * instead of using an inline lambda expression.
     */
    static class TransactionContextFactory implements Supplier<ConcurrentMap<String, MetadataItem>> {
        /**
         * returns a ConcurrentMap with a default {@link #UNINITIALIZED_TRANSACTION_CONTEXT_VALUE value} for the key {@link #TRANSACTION_ID_KEY}
         *
         * @return ThreadLocal variable which is a {@link ConcurrentMap}
         */
        @Override
        public ConcurrentMap<String, MetadataItem> get() {
            ConcurrentMap<String, MetadataItem> map = new ConcurrentHashMap<>();
            map.put(TRANSACTION_ID_KEY, new MetadataItem(TransactionContext.UNINITIALIZED_TRANSACTION_CONTEXT_VALUE));
            map.put(TRANSACTION_OWNING_THREAD_KEY, new MetadataItem(Long.valueOf(-1)));
            return map;
        }
    }

    /**
     * For internal use, retrieves the internal reference counter.
     * @return The internal reference counter
     */
    static AtomicInteger getReferenceCounter() {
        MetadataItem referenceCounter = transactionContext.get().get(REFERENCE_COUNTER_KEY);
        if (referenceCounter == null) {
            return null;
        }
        return AtomicInteger.class.cast(referenceCounter.get());
    }

    /**
     * Create a new unique Transaction Context for this thread
     * @return the current stack depth of the transaction context caused by create(). Will be 0 if this call
     * to create() actually caused a new transaction context to be created. Higher values indicate that the reference counter
     * was simply incremented.
     */
    public static int create() {
        // Prevent destructive actions by incrementing a reference counter to detect when to truly
        // create a new Transaction Context.
        if (getReferenceCounter() == null || getReferenceCounter().get() <= 0) {
            clear();
            set(FastIdGenerator.generate());
            putMetadata(TRANSACTION_OWNING_THREAD_KEY, Long.valueOf(Thread.currentThread().getId()));
            transactionContext.get().put(REFERENCE_COUNTER_KEY, new MetadataItem(new AtomicInteger(0)));
            EventBus.publish(new TransactionBeginEvent("Core"));
        }
        return getReferenceCounter().getAndIncrement();
    }

    /**
     * Destroys the Transaction Context for this thread. If multiple create methods
     * were called, the same number of destroy methods should be called to clear
     * the Transaction Context. If there are more destroy() calls than create(),
     * this will do nothing.
     */
    public static void destroy() {
        if (getReferenceCounter() == null) {
            clear();
            return;
        }
        // When the counter <= 0, we know that the transaction is fully finished.
        if (getReferenceCounter().decrementAndGet() <= 0) {
            EventBus.publish(new TransactionEndEvent("Core"));
            clear();
        }
    }

    /**
     * Get the current Transaction ID value for the thread
     * @return - the current TransactionContext value
     */
    public static String get() {
        return String.class.cast(transactionContext.get().get(TRANSACTION_ID_KEY).get());
    }

    /**
     * Set the Transaction ID value for the the thread
     * @param value - the new TransactionContext value
     */
    public static void set(String value) {
        transactionContext.get().put(TRANSACTION_ID_KEY, new MetadataItem(value));
    }

    /**
     * Place an arbitrary value into the map
     * @param key a String to identify the data.  May not be "discoTransactionId" which is reserved internally.
     * @param value the metadata value
     */
    public static void putMetadata(String key, Object value) {
        if (TRANSACTION_ID_KEY.equals(key)) {
            throw new IllegalArgumentException(TRANSACTION_ID_KEY + " may not be used as a metadata key");
        }

        transactionContext.get().put(key,  new MetadataItem(value));
    }

    /**
     * Remove a value from the map
     *
     * @param key a String to identify the data.  May not be "discoTransactionId" which is reserved internally.
     */
    public static void removeMetadata (String key) {
        if (TRANSACTION_ID_KEY.equals(key)) {
            throw new IllegalArgumentException(TRANSACTION_ID_KEY + " may not be used as a metadata key");
        }
        transactionContext.get().remove(key);
    }

    /**
     * Get data from the metadata map
     * @param key a String to identify the data. May not be "discoTransactionId" which is reserved internally.
     * @return the metadata value
     */
    public static Object getMetadata(String key) {
        if (TRANSACTION_ID_KEY.equals(key)) {
            throw new IllegalArgumentException(TRANSACTION_ID_KEY + " may not be used as a metadata key");
        }

        MetadataItem metadataItem = transactionContext.get().get(key);
        if (metadataItem == null) {
            return null;
        }

        return metadataItem.get();
    }

    /**
     * Get data from the metadata map which contains the specified tag
     * @param tag a String to identify the metadata objects to be returned.
     * @return a map of metadata objects that contained the tag
     */
    public static Map<String, Object> getMetadataWithTag(String tag) {
        Map<String, Object> result = transactionContext
                                    .get().entrySet().stream().filter(map -> map.getValue().hasTag(tag))
                                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue().get()));
        return result;
    }

    /**
     * Set a tag on data from the metadata map with a tag
     * @param key a String to identify the data.
     * @param tag a String that will be added to label/tag the data.
     */
    public static void setMetadataTag(String key, String tag) {
        MetadataItem metadataItem = transactionContext.get().get(key);
        if (metadataItem == null) {
            throw new IllegalArgumentException(key + " no metadata object exists for this key");
        } else {
            metadataItem.setTag(tag);
        }
    }

    /**
     * Removes a tag from a metadata entry
     * @param key a String to identify the data.
     * @param tag a String representing the label/tag that will be cleared.
     */
    public static void clearMetadataTag(String key, String tag) {
        MetadataItem metadataItem = transactionContext.get().get(key);
        if (metadataItem == null) {
            throw new IllegalArgumentException(key + " no metadata object exists for this key");
        } else {
            metadataItem.clearTag(tag);
        }
    }

    /**
     * Queries if a given metadata key has the specified tag. The metadata must exist, which can be checked via a prior
     * call to getMetadata(), checking that null is not returned.
     * @param key a String to identify the data.
     * @param tag a String representing the label/tag
     * @return true if this metadata has the given tag.
     * @throws IllegalArgumentException if no such metadata exists
     */
    public static boolean hasMetadataTag(String key, String tag) {
        MetadataItem metadataItem = transactionContext.get().get(key);
        if (metadataItem == null) {
            throw new IllegalArgumentException(key + " no metadata object exists for this key");
        } else {
           return metadataItem.hasTag(tag);
        }
    }

    /**
     * Clears the value of the TransactionContext for this thread, and restores it to an empty state
     */
    public static void clear() {
        transactionContext.set(TRANSACTION_CONTEXT_FACTORY.get());
    }

    /**
     * Actually remove the TransactionContext for this thread, causing the ThreadLocal variable to be expunged by the
     * JVM which can be expensive.
     */
    public static void remove() {
        transactionContext.remove();
    }

    /**
     * Get the default uninitialized Transaction Context value
     * @return - "disco_null_id"
     */
    public static String getUninitializedTransactionContextValue() {
        return UNINITIALIZED_TRANSACTION_CONTEXT_VALUE;
    }

    /**
     * Test if we are currently considered to be inside a created Transaction Context or not.
     * This is a fairly coarse test just against an uninitialized Transaction Context value.
     * Mainly this is designed to test for so-called 'steady-state', which means program behaviors which
     * fall outside of a service activity e.g. bean initialization, and background workers which poll for
     * rarely-changing cacheable state. This is only a heuristic though, and requires two conditions to be satisfied:
     * 1) The service framework thread pool is not reused for non-activity tasks. The interceptors do take care to
     *    clear the transactionId after processing, but if that logic were to fail, the thread lingers with the
     *    identity previously assigned to it.
     * 2) Service activities do not lazily initialize background workers. We assume that these are created during
     *    service startup or bean initialization. If a service activity lazily begins a background task, the identity
     *    will be propagated into the new thread, and will then resemble the activity.
     *
     *    TODO - design a better mechanism than this. We may wish to track a pool of 'active' transaction ids
     *           for example, and extend this check. We may want to make the create()/clear() pair of operations
     *           around every activity more reliable, instead of Support package authors having to remember to do it
     * @return true if we think we're currently inside a created Transaction Context, else false
     */
    public static boolean isWithinCreatedContext() {
        return !UNINITIALIZED_TRANSACTION_CONTEXT_VALUE.equals(get());
    }

    /**
     * For internal use. Get the underlying map of metadata. Needs to be public for accessibility from Advice methods.
     * @return the Map of metadata
     */
    public static ConcurrentMap<String, MetadataItem> getPrivateMetadata() {
        return transactionContext.get();
    }
    /**
     * For internal use. Get the ThreadLocal that holds the private metadata.
     * Needs to be public for accessibility from Advice methods.
     *
     * @return the threadlocal object.
     */
    public static ThreadLocal<ConcurrentMap<String, MetadataItem>> getPrivateMetadataThreadLocal() {
        return transactionContext;
    }

    /**
     * For internal use, replace the entire underlying map of metadata.
     * Needs to be public for accessibility from Advice methods.
     * @param metadata a map of new metadata to create in the current Thread Local storage
     */
    public static void setPrivateMetadata(ConcurrentMap<String, MetadataItem> metadata) {
        transactionContext.set(metadata);
    }
}
