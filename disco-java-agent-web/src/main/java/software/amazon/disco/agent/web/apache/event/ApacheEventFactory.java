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

package software.amazon.disco.agent.web.apache.event;

import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.web.apache.utils.HttpRequestAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpRequestBaseAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpResponseAccessor;

/**
 * Create our private events, so that listeners do not have public access to them
 */
public class ApacheEventFactory {
    /**
     * Create our private events, so that listeners do not have public access to them
     * @param origin the origin of the downstream call e.g. 'Web'
     * @param accessor a HttpRequestAccessor to get uri and HTTP method
     * @return a {@link ApacheHttpServiceDownstreamRequestEvent}
     */
    public static HttpServiceDownstreamRequestEvent createDownstreamRequestEvent(String origin, HttpRequestAccessor accessor) {
        String uri;
        String method;
        if (accessor instanceof HttpRequestBaseAccessor) {
            //we can retrieve the data in a streamlined way, avoiding internal production of the RequestLine
            HttpRequestBaseAccessor baseAccessor = (HttpRequestBaseAccessor)accessor;
            uri = baseAccessor.getUri();
            method = baseAccessor.getMethod();
        } else {
            uri = accessor.getUriFromRequestLine();
            method = accessor.getMethodFromRequestLine();
        }
        //TODO - using uri and method as service and operation name is unsatisfactory.
        ApacheHttpServiceDownstreamRequestEvent requestEvent = new ApacheHttpServiceDownstreamRequestEvent(origin, uri, method, accessor);;
        requestEvent.withMethod(method);
        requestEvent.withUri(uri);
        return requestEvent;
    }

    /**
     * Create response event with HttpResponse for apache client downstream call
     * @param responseAccessor a HttpResponseAccessor to get status code etc.
     * @param requestEvent Previously published ServiceDownstreamRequestEvent
     * @param throwable The throwable if the request fails
     * @return  a {@link HttpServiceDownstreamResponseEvent}.
     */
    public static ServiceDownstreamResponseEvent createServiceResponseEvent(final HttpResponseAccessor responseAccessor, final ServiceDownstreamRequestEvent requestEvent, final Throwable throwable) {
        HttpServiceDownstreamResponseEvent responseEvent = new HttpServiceDownstreamResponseEvent(requestEvent.getOrigin(), requestEvent.getService(), requestEvent.getOperation(), requestEvent);
        if (throwable != null) {
            responseEvent.withThrown(throwable);
        }
        if (responseAccessor != null) {
            responseEvent.withStatusCode(responseAccessor.getStatusCode());
            responseEvent.withContentLength(responseAccessor.getContentLength());
        }
        return responseEvent;
    }
}
