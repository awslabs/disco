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
 * Specialization of ServiceEvent for service requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs
 */
public interface ServiceResponseEvent extends ServiceEvent {
    /**
     * Get the associated request event
     * @return the request object
     */
    ServiceRequestEvent getRequest();

    /**
     * Get the response object
     * @return the response object
     */
    Object getResponse();

    /**
     * Get the thrown exception
     * @return the thrown exception
     */
    Throwable getThrown();
}
