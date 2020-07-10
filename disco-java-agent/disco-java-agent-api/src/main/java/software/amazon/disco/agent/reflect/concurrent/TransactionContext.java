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

package software.amazon.disco.agent.reflect.concurrent;


import software.amazon.disco.agent.reflect.ReflectiveCall;
import software.amazon.disco.agent.reflect.logging.Logger;

import java.util.Collections;
import java.util.Map;


/**
 * Manipulation/overriding of the current TransactionContext, which identifies the current service interaction i.e.
 * the entire enclosure of operations between the service Request and Response. By default, DiSCo chooses a GUID for
 * this, but services consuming DiSCo may elect a different strategy for calculating this value, perhaps to give it
 * semantics more nuanced than just 'random'. The idea is that at the earliest opportunity during the service's Activity
 * this TransactionContext should be set. This must be before ANY downstream dependency interactions have occurred.
 */
public class TransactionContext {
    static final String DISCO_PREFIX = "$amazon.disco";
    static final String TRANSACTIONCONTEXT_CLASS = ".concurrent.TransactionContext";

    /**
     * Create a default UUID transaction ID
     * @return the tx stack depth.
     */
    public static int create() {
        return ReflectiveCall.returning(int.class)
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("create")
                .withDefaultValue(0)
                .call();
    }

    /**
     * Destroys the Transaction Context for this thread. If multiple create methods
     * were called, the same number of destroy methods should be called to clear
     * the Transaction Context. If there are more destroy() calls than create(),
     * this will do nothing.
     */
    public static void destroy() {
        ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("destroy")
                .call();
    }
    /**
     * Set the DiSCo Transaction ID to a specific value.
     * @param value - the value to set
     */
    public static void set(String value) {
        ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("set")
                .withArgTypes(String.class)
                .call(value);
    }

    /**
     * Get the DiSCo Transaction ID, or null if the agent is not loaded
     * @return - the TransactionContext value
     */
    public static String get() {
        return ReflectiveCall.returning(String.class)
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("get")
                .call();
    }

    /**
     * Set a value in the DiSCo metadata map, or do nothing if the agent is not loaded
     * @param key the key of the metadata
     * @param value the metadata value
     */
    public static void putMetadata(String key, Object value) {
        ReflectiveCall call = ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("putMetadata")
                .withArgTypes(String.class, Object.class);

        checkMetadataKey(call, key);

        call.call(key, value);
    }

    /**
     * Remove a value in the DiSCo metadata map, or do nothing if the agent is not loaded
     * @param key the key of the metadata
     */
    public static void removeMetadata(String key) {
        ReflectiveCall call = ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("removeMetadata")
                .withArgTypes(String.class);

        checkMetadataKey(call, key);

        call.call(key);
    }

    /**
     * Get a value from the DiSCo metadata map, or null if the agent is not loaded
     * @param key the key of the metadata
     * @return the metadata value
     */
    public static Object getMetadata(String key) {
        ReflectiveCall call = ReflectiveCall.returning(Object.class)
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("getMetadata")
                .withArgTypes(String.class);

        checkMetadataKey(call, key);

        return call.call(key);
    }

    /**
     * Set a tag on data from the metadata map with a tag, or do nothing if the agent is not loaded
     * @param key a String to identify the data.
     * @param tag a String that will be added to label/tag the data.
     */
    public static void setMetadataTag(String key, String tag) {
        ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("setMetadataTag")
                .withArgTypes(String.class, String.class)
                .call(key, tag);
    }

    /**
     * Get data from the metadata map which contains the specified tag, or return null if the agent is not loaded
     * @param tag a String to identify the metadata objects to be returned.
     * @return a map of metadata objects that contained the tag
     */
    public static Map<String, Object> getMetadataWithTag(String tag) {
        return ReflectiveCall.returning(Map.class)
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("getMetadataWithTag")
                .withArgTypes(String.class)
                .withDefaultValue(Collections.emptyMap())
                .call(tag);
    }

    /**
     * Clear the DiSCo TransactionContext to revert to its default value, or a no-op if Agent not loaded
     */
    public static void clear() {
        ReflectiveCall.returningVoid()
                .ofClass(TRANSACTIONCONTEXT_CLASS)
                .ofMethod("clear")
                .call();
    }

    /**
     * Get the default uninitialized Transaction Context value
     * @return - "disco_null_id"
     */
    public static String getUninitializedTransactionContextValue() {
        return ReflectiveCall.returning(String.class)
            .ofClass(TRANSACTIONCONTEXT_CLASS)
            .ofMethod("getUninitializedTransactionContextValue")
            .call();
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
        Boolean result = ReflectiveCall.returning(Boolean.class)
            .ofClass(TRANSACTIONCONTEXT_CLASS)
            .ofMethod("isWithinCreatedContext")
            .call();
        return result == null ? false : result;
    }

    /**
     * Helper method to check for an invalid/reserved Key when interacting with TransactionContext metadata.
     * @param call the call taking place when the check was performed
     * @param key the offending key
     */
    private static void checkMetadataKey(ReflectiveCall call, String key) {
        if (key.startsWith(DISCO_PREFIX)) {
            String message = key + " may not be used as a metadata key as the prefix " + DISCO_PREFIX + " is reserved for internal use";
            Logger.warn(message);
            call.dispatchException(new IllegalArgumentException(message));
        }
    }
}
