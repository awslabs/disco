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

package software.amazon.disco.agent.web.apache.utils;

import software.amazon.disco.agent.interception.annotations.DataAccessPath;

/**
 * Data Accessor for subtypes of HttpResponse.
 */
public interface HttpResponseAccessor {
    /**
     * Get the status code from the status line in the response
     * @return the http status code
     */
    @DataAccessPath("getStatusLine()/getStatusCode()")
    int getStatusCode();

    /**
     * Get the content length of the entity in the response
     * @return the entity content length
     */
    @DataAccessPath("getEntity()/getContentLength()")
    long getContentLength();
}
