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

package software.amazon.disco.agent.web.apache.httpclient.utils;

import software.amazon.disco.agent.web.MethodHandleWrapper;

/**
 * Accessor for data stored in Apache's HttpResponse
 */
public class HttpResponseAccessor {
    private static final String HTTP_RESPONSE_CLASS_NAME = "org.apache.http.HttpResponse";

    private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    private static MethodHandleWrapper getStatusLine;
    private static MethodHandleWrapper getStatusCode;

    private static MethodHandleWrapper getEntity;
    private static MethodHandleWrapper getContentLength;

    private final Object responseObject;

    {
        try {
            getStatusLine = new MethodHandleWrapper(HTTP_RESPONSE_CLASS_NAME, classLoader, "getStatusLine", "org.apache.http.StatusLine");
            getStatusCode = new MethodHandleWrapper(getStatusLine.getRtype().getName(), classLoader, "getStatusCode", int.class);

            getEntity = new MethodHandleWrapper(HTTP_RESPONSE_CLASS_NAME, classLoader, "getEntity", "org.apache.http.HttpEntity");
            getContentLength = new MethodHandleWrapper(getEntity.getRtype().getName(), classLoader, "getContentLength", long.class);
        } catch (Exception e) {
            //do nothing
        }
    }


    /**
     * Create a new accessor of an HttpResponse object
     * @param response the HttpResponseObject
     */
    public HttpResponseAccessor(Object response) {
        this.responseObject = response;
    }

    /**
     * Get the status code stored in the response
     * @return status code
     */
    public int getStatusCode() {
        if (responseObject == null) {
            return -1;
        }
        return (int)getStatusCode.invoke(getStatusLine.invoke(responseObject));
    }

    /**
     * Get the content length stored in the response
     * @return content length
     */
    public long getContentLength() {
        if (responseObject == null) {
            return -1L;
        }

        Object entity = getEntity.invoke(responseObject);
        if (entity == null) {
            return 0L;
        }
        return (long)getContentLength.invoke(entity);
    }
}
