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
 * Abstract Event to encapsulate information when execution crosses a thread boundary, within an AlphaOne transaction
 */
public abstract class AbstractThreadEvent extends AbstractEvent implements ThreadEvent {
    /**
     * The data items contained in thread events
     */
    enum DataKey {
        /**
         * The threadId of the parent thread
         */
        PARENT_ID,

        /**
         * The threadId of the child thread
         */
        CHILD_ID
    }

    /**
     * Create a new ThreadEvent
     * @param origin the origin of the Event, presumably 'Concurrency'
     * @param parentId threadId of parent thread
     * @param childId threadId of worker thread
     */
    public AbstractThreadEvent(String origin, Long parentId, Long childId) {
        super(origin);
        withData(DataKey.PARENT_ID.name(), parentId);
        withData(DataKey.CHILD_ID.name(), childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractThreadEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getParentId() {
        return Long.class.cast(getData(DataKey.PARENT_ID.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getChildId() {
        return Long.class.cast(getData(DataKey.CHILD_ID.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Operation getOperation();
}
