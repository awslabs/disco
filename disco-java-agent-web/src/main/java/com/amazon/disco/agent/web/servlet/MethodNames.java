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

package com.amazon.disco.agent.web.servlet;

/**
 * Constants of the method names which will be reflectively called on the servlet request and response objects
 */
public class MethodNames {
    // Common Get Methods from request/response
    static final String GET_HEADER = "getHeader";
    static final String GET_HEADER_NAMES = "getHeaderNames";

    // Request-specific methods
    static final String GET_METHOD = "getMethod";
    static final String GET_REQUEST_URL = "getRequestURL";
    static final String GET_LOCAL_PORT = "getLocalPort";
    static final String GET_LOCAL_ADDR = "getLocalAddr";
    static final String GET_REMOTE_PORT = "getRemotePort";
    static final String GET_REMOTE_ADDR = "getRemoteAddr";

    // Response-specific methods
    static final String GET_STATUS = "getStatus";
}
