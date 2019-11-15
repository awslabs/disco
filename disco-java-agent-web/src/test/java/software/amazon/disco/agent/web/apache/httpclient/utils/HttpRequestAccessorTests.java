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

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpRequestAccessorTests {

    HttpRequest httpRequest;
    RequestLine requestLine;
    HttpRequestAccessor httpRequestAccessor;

    @Before
    public void before() {
        httpRequest = mock(HttpRequest.class);
        requestLine = mock(RequestLine.class);
        httpRequestAccessor = new HttpRequestAccessor(httpRequest);
    }

    @Test
    public void testAddHeader() throws Throwable {
        String headerName = "headerName";
        String headerValue = "headerValue";
        httpRequestAccessor.addHeader(headerName, headerValue);

        ArgumentCaptor<String> headerNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpRequest, times(1)).addHeader(headerNameArgumentCaptor.capture(), headerValueArgumentCaptor.capture());

        assertEquals(headerName, headerNameArgumentCaptor.getValue());
        assertEquals(headerValue, headerValueArgumentCaptor.getValue());
    }

    @Test
    public void testRemoveHeaders() throws Throwable {
        String headerName = "headerName";
        httpRequestAccessor.removeHeaders(headerName);

        ArgumentCaptor<String> headerNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpRequest, times(1)).removeHeaders(headerNameArgumentCaptor.capture());

        assertEquals(headerName, headerNameArgumentCaptor.getValue());
    }

    /**
     * Expect returning the FIRST object that is an instance of HttpRequest or its child classes.
     */
    @Test
    public void testFindRequestObjectFound() {
        HttpUriRequest httpUriRequest = mock(HttpUriRequest.class);

        assertEquals(httpRequest, HttpRequestAccessor.findRequestObject("not me", httpRequest, httpUriRequest));
        assertEquals(httpUriRequest, HttpRequestAccessor.findRequestObject("not me", httpUriRequest, httpRequest));
    }

    @Test
    public void testFindRequestObjectNotFound() {
        assertNull(HttpRequestAccessor.findRequestObject("not me"));
    }

    @Test
    public void testGetMethodSucceeded() {
        String method = "GET";

        when(requestLine.getMethod()).thenReturn(method);
        when(httpRequest.getRequestLine()).thenReturn(requestLine);

        assertEquals(method, httpRequestAccessor.getMethod());
    }

    @Test
    public void testGetUriSucceeded() {
        String uri = "http://amazon.com/explore/something";

        when(requestLine.getUri()).thenReturn(uri);
        when(httpRequest.getRequestLine()).thenReturn(requestLine);

        assertEquals(uri, httpRequestAccessor.getUri());
    }

    @Test
    public void testGetMethodAndGetUriFailed() {
        when(httpRequest.getRequestLine()).thenReturn(null);

        assertNull(httpRequestAccessor.getMethod());
        assertNull(httpRequestAccessor.getUri());
    }
}