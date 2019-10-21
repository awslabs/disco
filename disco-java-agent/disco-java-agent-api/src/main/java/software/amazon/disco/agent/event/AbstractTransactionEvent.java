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
 * A class of event to encapsulate the begin or end of a logical Transaction. For a request-response service, this envelopes
 * the lifetime of the service activities. For other types of service-under-test this might be a receipt of a unit of work
 * by some other non-SAAS means e.g. draining a queue of work items
 */
public abstract class AbstractTransactionEvent extends AbstractEvent implements TransactionEvent {
    /**
     * Create a new TransactionEvent
     * @param origin the origin e.g. the name of a service framework plugin
     */
    public AbstractTransactionEvent(String origin) {
        super(origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractTransactionEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Operation getOperation();
}
