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

package software.amazon.disco.agent.web.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

public class HttpServletRequestAccessorTests {
    HttpServletRequestAccessor accessor;

    @Before
    public void before() {
        accessor = mock(HttpServletRequestAccessor.class);
        when(accessor.retrieveHeaderMap()).thenCallRealMethod();
    }

    @Test
    public void testGetHeaders() {
        when(accessor.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("headername")));
        when(accessor.getHeader("headername")).thenReturn("headervalue");
        Map<String, String> map = accessor.retrieveHeaderMap();
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("headervalue", map.get("headername"));
    }

    @Test
    public void testGetHeadersWhenNull() {
        when(accessor.getHeaderNames()).thenReturn(null);
        Map<String, String> map = accessor.retrieveHeaderMap();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testGetRemotePort() {
        when(accessor.getRemotePort()).thenReturn(800);
        Assert.assertEquals(800, accessor.getRemotePort());
    }

    @Test
    public void testGetRemoteAddr() {
        when(accessor.getRemoteAddr()).thenReturn("1.2.3.4");
        Assert.assertEquals("1.2.3.4", accessor.getRemoteAddr());
    }

    @Test
    public void testGetLocalPort() {
        when(accessor.getLocalPort()).thenReturn(900);
        Assert.assertEquals(900, accessor.getLocalPort());
    }

    @Test
    public void testGetLocalAddr() {
        when(accessor.getLocalAddr()).thenReturn("4.3.2.1");
        Assert.assertEquals("4.3.2.1", accessor.getLocalAddr());
    }

    @Test
    public void testGetMethod() {
        when(accessor.getMethod()).thenReturn("POST");
        Assert.assertEquals("POST", accessor.getMethod());
    }

    @Test
    public void testGetRequestURL() {
        when(accessor.getRequestUrl()).thenReturn("http://example.com/foo?bar=baz");
        Assert.assertEquals("http://example.com/foo?bar=baz", accessor.getRequestUrl());
    }
}
