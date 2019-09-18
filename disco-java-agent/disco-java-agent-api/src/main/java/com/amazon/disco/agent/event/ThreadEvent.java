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

package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus on Thread entry/exit
 */
public interface ThreadEvent extends Event {
    /**
     * The specific type of ThreadEvent
     */
    enum Operation {
        /**
         * This operation is published when a new worker thread is first entered
         */
        ENTERING,

        /**
         * This operation is published when a worker thread has finished working, right before the Runnable/Callable/etc completes
         */
        EXITING
    }

    /**
     * Get the parent threadId
     * @return the parent threadId
     */
    long getParentId();

    /**
     * Get the child threadId
     * @return the child threadId
     */
    long getChildId();

    /**
     * Get the particular type of ThreadEvent ENTERING or EXITING
     * @return the specific thread Operation
     */
    Operation getOperation();
}
