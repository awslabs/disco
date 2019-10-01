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

package com.amazon.disco.agent.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.*;

public class HttpServletResponseAccessorTests {
    HttpServletResponse req;
    HttpServletResponseAccessor accessor;

    @Before
    public void before() {
        req = mock(HttpServletResponse.class);
        accessor = new HttpServletResponseAccessor(req);
    }

    @Test
    public void testGetHeaders() {
        when(req.getHeaderNames()).thenReturn(Arrays.asList("headername"));
        when(req.getHeader("headername")).thenReturn("headervalue");
        Map<String, String> map = accessor.retrieveHeaderMap();
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("headervalue", map.get("headername"));
    }

    @Test
    public void testGetHeadersWhenNull() {
        when(req.getHeaderNames()).thenReturn(null);
        Map<String, String> map = accessor.retrieveHeaderMap();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testGetClassOf() {
        Assert.assertEquals(HttpServletResponse.class, accessor.getClassOf());
    }

    @Test
    public void testGetObject() {
        Assert.assertEquals(req, accessor.getObject());
    }

    @Test
    public void testGetStatus() {
        when(req.getStatus()).thenReturn(202);
        Assert.assertEquals(202, accessor.getStatus());
    }
}
