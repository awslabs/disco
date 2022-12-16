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
package software.amazon.disco.agent.web.apache.source;

import software.amazon.disco.agent.event.DownstreamRequestHeaderRetrievable;
import software.amazon.disco.agent.event.DownstreamResponseHeaderRetrievable;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApacheClientTestUtil {

    public static void verifyServiceRequestEvent(final HttpServiceDownstreamRequestEvent serviceDownstreamRequestEvent) {
        assertEquals(ApacheTestConstants.METHOD, serviceDownstreamRequestEvent.getMethod());
        assertEquals(ApacheTestConstants.URI, serviceDownstreamRequestEvent.getUri());
        assertEquals(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamRequestEvent.getOrigin());
        assertNull(serviceDownstreamRequestEvent.getRequest());
    }

    public static void verifyServiceResponseEvent(final ServiceDownstreamResponseEvent serviceDownstreamResponseEvent) {
        assertEquals(ApacheTestConstants.METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(ApacheTestConstants.URI, serviceDownstreamResponseEvent.getService());
        assertEquals(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamResponseEvent.getOrigin());
    }

    public static void verifyRequestHeaderRetrievable(final DownstreamRequestHeaderRetrievable retrievable) {
        assertEquals(retrievable.getFirstHeader("someheader"), "somedata");
        assertEquals(retrievable.getFirstHeader("someheader2"), "somedata2");
        assertNull(retrievable.getHeaders("nonexistentheader"));
    }

    public static void verifyResponseHeaderRetrievable(final DownstreamResponseHeaderRetrievable retrievable) {
        assertEquals(retrievable.getFirstHeader("someheader"), "somedata");
        assertEquals(retrievable.getFirstHeader("someheader2"), "somedata2");
        assertNull(retrievable.getHeaders("nonexistentheader"));
    }
}
