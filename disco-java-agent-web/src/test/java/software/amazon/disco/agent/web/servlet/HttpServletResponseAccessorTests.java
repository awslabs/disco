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

import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.*;

public class HttpServletResponseAccessorTests {
    HttpServletResponseAccessor accessor;

    @Before
    public void before() {
        accessor = mock(HttpServletResponseAccessor.class);
        when(accessor.retrieveHeaderMap()).thenCallRealMethod();
    }

    @Test
    public void testGetHeaders() {
        when(accessor.getHeaderNames()).thenReturn(Arrays.asList("headername"));
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
    public void testGetStatus() {
        when(accessor.getStatus()).thenReturn(202);
        Assert.assertEquals(202, accessor.getStatus());
    }
}
