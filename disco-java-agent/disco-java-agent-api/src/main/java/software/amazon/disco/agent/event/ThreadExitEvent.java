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

package software.amazon.disco.agent.event;

import java.util.concurrent.Future;

/**
 * Concrete ThreadEvent published when a worker thread exits a Disco transaction, and the inner FunctionalInterface is about to return.
 * Under normal circumstances, a 'ThreadEnterEvent' should always be paired with a 'ThreadExitEvent'.
 *
 * Please note that when an application submits a task, e.g. {@link Runnable} to an executor service, e.g. {@link java.util.concurrent.ThreadPoolExecutor},
 * the returned {@link java.util.concurrent.Future} object that tracks the completion of the submitted task may be marked as completed
 * before the associated {@link ThreadExitEvent} is published. In other words, a call to {@link Future#get()} may unblock prior to
 * the publication of the related {@link ThreadExitEvent}.
 */
public class ThreadExitEvent extends AbstractThreadEvent {
    /**
     * Construct a new ThreadExitEvent
     * @param origin origin of the event, presumably 'Concurrency'
     * @param parentId the threadId of the parent thread
     * @param childId the threadId of the worker thread
     */
    public ThreadExitEvent(String origin, Long parentId, Long childId) {
        super(origin, parentId, childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.EXITING;
    }
}
