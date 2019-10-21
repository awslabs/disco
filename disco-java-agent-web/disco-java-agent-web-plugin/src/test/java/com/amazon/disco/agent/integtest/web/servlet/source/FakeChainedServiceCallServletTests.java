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

package com.amazon.disco.agent.integtest.web.servlet.source;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal tests for FakeOverriddenServlet
 */
public class FakeChainedServiceCallServletTests {

    @Test
    public void testFakeOverridenServlet() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        List<String> didCallIndicator = new ArrayList<>();

        // Needed for HttpServlet.service() call.
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getProtocol()).thenReturn("HTTP/1.1");

        FakeChainedServiceCallServlet servlet = new FakeChainedServiceCallServlet();
        try {
            servlet.service(request, response, 1, 2, 3, 4, didCallIndicator);
        } catch (Throwable e) {
            // Shouldn't have any errors.
            Assert.fail();
        }

        // the List gets populated with an item from the bottom-level service() call.
        // It ensures that the chain of "service()" methods were all called.
        Assert.assertEquals(1, didCallIndicator.size());
    }

}
