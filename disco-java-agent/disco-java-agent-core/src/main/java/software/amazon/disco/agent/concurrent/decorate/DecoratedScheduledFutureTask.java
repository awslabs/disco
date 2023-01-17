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

/**
 * ScheduledThreadPoolExecutor$DelayedWorkQueue.indexOf is doing an explicit type check for ScheduledFutureTask.
 * Wrapping instances of this type with a Disco type would cause a performance regression by failing this type check
 * and forcing the code into doing a linear scan of the delayed work item queue rather than a constant-time lookup.
 * In order to keep the work items' type ScheduledFutureTask while doing context propagation across scheduled work
 * items, we're forced to decorate the ScheduledFutureTask class itself, despite it being private.
 */
public class DecoratedScheduledFutureTask extends Decorated {
    public static final String DISCO_DECORATION_FIELD_NAME = "$discoDecoration";

    /**
     * Private constructor, use factory method for creation.
     */
    private DecoratedScheduledFutureTask() {
    }

    /**
     * Create DiSCo propagation metadata for a ScheduledFutureTask
     * @return a new instance of DecoratedScheduledFutureTask.
     */
    public static DecoratedScheduledFutureTask create() {
        return new DecoratedScheduledFutureTask();
    }

    /**
     * An interface we add to decorated ScheduledFutureTasks, to have get/set operations on the added field.
     */
    public interface Accessor {
        public static final String GET_DISCO_DECORATION_METHOD_NAME = "getDiscoDecoration";
        public static final String SET_DISCO_DECORATION_METHOD_NAME = "setDiscoDecoration";

        /**
         * Get the added discoDecoration field from an intercepted ScheduledFutureTask
         * @return the discoDecoration field
         */
        DecoratedScheduledFutureTask getDiscoDecoration();

        /**
         * Set the added discoDecoration field on an intercepted ScheduledFutureTask
         * @param decoratedScheduledFutureTask the new value
         */
        void setDiscoDecoration(DecoratedScheduledFutureTask decoratedScheduledFutureTask);
    }
}
