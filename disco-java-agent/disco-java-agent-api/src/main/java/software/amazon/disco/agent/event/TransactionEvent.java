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
 * An event issued to the event bus at the beginning and end of logical 'transactions'
 */
public interface TransactionEvent extends Event {
    /**
     * The kind of specific operation of this transaction event
     */
    enum Operation {
        /**
         * When the transaction logically begins
         */
        BEGIN,

        /**
         * When the transaction logically ends
         */
        END
    }

    /**
     * Get the type of TransactionEvent, BEGIN or END
     * @return the type of this TransactionEvent
     */
    Operation getOperation();
}
