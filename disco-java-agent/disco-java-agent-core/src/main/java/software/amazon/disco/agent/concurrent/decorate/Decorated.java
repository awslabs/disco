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

import software.amazon.disco.agent.concurrent.ConcurrentUtils;
import software.amazon.disco.agent.concurrent.MetadataItem;
import software.amazon.disco.agent.concurrent.TransactionContext;

import java.util.concurrent.ConcurrentMap;

/**
 * To propagate context, we decorate or otherwise adorn classes such as Runnable, Callable and ForkJoinTask
 * with extra metadata regarding thread provenance. This metadata is encapsulated in this abstraction.
 */
public abstract class Decorated {
    protected Long ancestralThreadId;
    protected long parentThreadId;
    ConcurrentMap<String, MetadataItem> parentTransactionContext;

    /**
     * Construct a new object to hold thread provenance information.
     */
    protected Decorated() {
        this.parentTransactionContext = TransactionContext.getPrivateMetadata();
        MetadataItem data = parentTransactionContext.get(TransactionContext.TRANSACTION_OWNING_THREAD_KEY);
        this.ancestralThreadId = (Long)data.get();
        this.parentThreadId = Thread.currentThread().getId();
    }

    /**
     * Convenience method to call before the execution of the dispatched object method eg. run() or call()
     */
    public void before() {
        ConcurrentUtils.set(ancestralThreadId, parentThreadId, parentTransactionContext);
    }

    /**
     * Convenience method to call after the execution of the dispatched object method eg. run() or call()
     */
    public void after() {
        ConcurrentUtils.clear(ancestralThreadId, parentThreadId, parentTransactionContext);
    }
}
