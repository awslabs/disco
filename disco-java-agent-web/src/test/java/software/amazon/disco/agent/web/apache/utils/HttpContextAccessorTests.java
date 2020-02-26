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

import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.web.apache.event.ApacheEventFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class HttpContextAccessorTests {

    private HttpContext context;
    private HttpContextAccessor accessor;

    @Before
    public void before() {
        context = mock(HttpContext.class);
        accessor = new HttpContextAccessor(context);
    }

    @Test
    public void testSetAttribute() {
        String id = "someId";
        String value = "someValue";
        accessor.setAttribute(id, value);

        ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(1)).setAttribute(idArgumentCaptor.capture(), valueArgumentCaptor.capture());

        assertEquals(id, idArgumentCaptor.getValue());
        assertEquals(value, valueArgumentCaptor.getValue());
    }

    @Test
    public void testGetAttribute() {
        String id = "someId";
        String value = "someValue";
        when(context.getAttribute(id)).thenReturn(value);

        assertEquals(value, accessor.getAttribute(id));
    }

    @Test
    public void testRemoveAttribute() {
        String id = "someId";
        String value = "someValue";
        when(context.removeAttribute(id)).thenReturn(value);

        assertEquals(value, accessor.removeAttribute(id));
    }

    @Test
    public void testSetRequestEvent() {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(null, null, null, null);
        accessor.setRequestEvent(requestEvent);

        ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(1)).setAttribute(idArgumentCaptor.capture(), valueArgumentCaptor.capture());

        assertEquals(HttpContextAccessor.DISCO_REQUEST_EVENT_ATTR_NAME, idArgumentCaptor.getValue());
        assertEquals(requestEvent, valueArgumentCaptor.getValue());
    }

    @Test
    public void testRemoveRequestEvent() {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(null, null, null, null);
        when(context.removeAttribute(HttpContextAccessor.DISCO_REQUEST_EVENT_ATTR_NAME)).thenReturn(requestEvent);

        assertEquals(requestEvent, accessor.removeRequestEvent());
    }

    @Test
    public void testGetResponseAccessorSucceeded() {
        when(context.getAttribute("http.response")).thenReturn(mock(HttpResponse.class));
        assertTrue(accessor.getResponseAccessor() instanceof HttpResponseAccessor);
    }

    @Test
    public void testGetResponseAccessorFailed() {
        when(context.getAttribute("http.response")).thenReturn("hello");
        assertNull(accessor.getResponseAccessor());
    }

}