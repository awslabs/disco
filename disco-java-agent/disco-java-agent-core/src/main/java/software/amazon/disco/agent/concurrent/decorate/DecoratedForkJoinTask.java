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

package software.amazon.disco.agent.concurrent.decorate;


/**
 * ForkJoinTask is an abstract class with many public methods, so cannot enjoy the more natural decoration
 * treatment afforded to Runnables and Callables. Instead of 'decoration' in the strict sense, we - via instrumentation -
 * augment the ForkJoinTask abstract class itself, with the addition threadId and transactionContext fields
 */
public class DecoratedForkJoinTask extends Decorated {
    public static final String DISCO_DECORATION_FIELD_NAME = "$discoDecoration";

    /**
     * Private constructor, use factory method for creation.
     */
    private DecoratedForkJoinTask() {
    }

    /**
     * Create DiSCo propagation metadata for a ForkJoinTask
     * @return a new instance of the DecoratedForkJoinTask object.
     */
    public static DecoratedForkJoinTask create() {
        return new DecoratedForkJoinTask();
    }

    /**
     * An interface we add to decorated ForkJoinTasks, to have bean get/set semantics on the added field.
     */
    public interface Accessor {
        public static final String GET_DISCO_DECORATION_METHOD_NAME = "getDiscoDecoration";
        public static final String SET_DISCO_DECORATION_METHOD_NAME = "setDiscoDecoration";

        /**
         * Get the added discoDecoration field from an intercepted ForkJoinTask
         * @return the discoDecoration field
         */
        DecoratedForkJoinTask getDiscoDecoration();

        /**
         * Set the added discoDecoration field on an intercepted ForkJoinTask
         * @param decoratedForkJoinTask the new value
         */
        void setDiscoDecoration(DecoratedForkJoinTask decoratedForkJoinTask);
    }
}
