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

/**
 * Concrete event published when a worker thread enters a Disco transaction. The same worker thread can enter a particular
 * transaction multiple times. For instance, consider the following scenario where 2 pairs of enter/exit events will be published.
 * - t0: Thread 't_1' enters transaction 'tx_1':  ThreadEnterEvent published
 * - t1: The same thread t1 exits 'tx_1':         ThreadExitEvent published
 * - t2: Thread sitting idle for some time
 * - t3: Thread 't1' re-enters 'tx_1':            ThreadEnterEvent published
 * - t4: 't1' exits 'tx_1':                       ThreadExitEvent published
 */
public class ThreadEnterEvent extends AbstractThreadEvent {
    /**
     * Construct a new ThreadEnterEvent
     * @param origin origin of the event, presumably 'Concurrency'
     * @param parentId the threadId of the parent thread
     * @param childId the threadId of the worker thread
     */
    public ThreadEnterEvent(String origin, Long parentId, Long childId) {
        super(origin, parentId, childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.ENTERING;
    }
}
