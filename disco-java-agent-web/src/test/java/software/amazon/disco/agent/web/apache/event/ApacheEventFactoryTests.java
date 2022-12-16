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

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.DownstreamRequestHeaderRetrievable;
import software.amazon.disco.agent.event.DownstreamResponseHeaderRetrievable;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HeaderReplaceable;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.web.apache.source.ApacheClientTestUtil;
import software.amazon.disco.agent.web.apache.source.ApacheTestConstants;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpRequest;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpResponse;
import software.amazon.disco.agent.web.apache.source.InterceptedHttpRequestBase;
import software.amazon.disco.agent.web.apache.source.MockEventBusListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ApacheEventFactoryTests {

    private InterceptedBasicHttpRequest accessor;
    private InterceptedHttpRequestBase httpRequestBaseAccessor;
    private MockEventBusListener mockEventBusListener;

    @Before
    public void before() {
        accessor = new InterceptedBasicHttpRequest();
        accessor.addHeader("someheader", "somedata");
        accessor.addHeader("someheader2", "somedata2");
        mockEventBusListener = new MockEventBusListener();
        httpRequestBaseAccessor = new InterceptedHttpRequestBase();
        httpRequestBaseAccessor.addHeader("someheader", "somedata");
        httpRequestBaseAccessor.addHeader("someheader2", "somedata2");
        TransactionContext.create();
        EventBus.addListener(mockEventBusListener);
    }

    @After
    public void after() {
        EventBus.removeListener(mockEventBusListener);
        TransactionContext.destroy();
    }

    @Test
    public void testForRequestEventCreationForRequest() {
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, accessor);
        ApacheClientTestUtil.verifyServiceRequestEvent(event);
        assertNull(accessor.getFirstHeader("TEST").getValue());
        HeaderReplaceable replaceable = (HeaderReplaceable) event;
        replaceable.replaceHeader("TEST", "TEST");
        assertTrue(accessor.getFirstHeader("TEST").getValue().equalsIgnoreCase("TEST"));
        ApacheClientTestUtil.verifyRequestHeaderRetrievable((DownstreamRequestHeaderRetrievable) event);
    }

    @Test
    public void testForRequestEventCreationForRequestBase() {
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, httpRequestBaseAccessor);
        ApacheClientTestUtil.verifyServiceRequestEvent(event);
        ApacheClientTestUtil.verifyRequestHeaderRetrievable((DownstreamRequestHeaderRetrievable) event);
    }

    @Test
    public void testForResponseEventCreationForSuccessfulResponse() {
        HttpResponse expectedResponse = new InterceptedBasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");
        expectedResponse.addHeader("someheader", "somedata");
        expectedResponse.addHeader("someheader2", "somedata2");
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, accessor);
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(expectedResponse, event, null);
        ApacheClientTestUtil.verifyServiceResponseEvent(responseEvent);
        assertNull(responseEvent.getThrown());
        ApacheClientTestUtil.verifyResponseHeaderRetrievable((DownstreamResponseHeaderRetrievable) responseEvent);
    }

    @Test
    public void testForRequestEventCreationWithNoRequestLine() {
        InterceptedBasicHttpRequest request = Mockito.mock(InterceptedBasicHttpRequest.class);
        Mockito.when(request.getRequestLine()).thenReturn(null);
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, request);
        assertNotNull(event);
        assertNull(event.getUri());
    }

    @Test
    public void testForResponseEventCreationForFailureResponse() {
        Exception error = new Exception("CUSTOM EXCEPTION");
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, accessor);
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(null, event, error);
        ApacheClientTestUtil.verifyServiceResponseEvent(responseEvent);
        assertEquals(responseEvent.getThrown(), error);
    }
}