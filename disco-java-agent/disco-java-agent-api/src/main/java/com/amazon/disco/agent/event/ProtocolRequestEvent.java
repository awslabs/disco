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
 * Specialization of ProtocolEvent for protocol requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs. At the protocol-level,
 * the requester can be seen as the invoker, and the response is the result of the invocation.
 */
public interface ProtocolRequestEvent extends ProtocolEvent {
    /**
     * Get the protocol-level request object
     * @return the request object
     */
    Object getRequest();

    /**
     * Get the source address; i.e. the origin that invoked this protocol request.
     * @return The source address
     */
    String getSourceAddress();

    /**
     * Get the destination address; i.e. the receiving end for this request.
     * @return The destination address
     */
    String getDestinationAddress();
}
