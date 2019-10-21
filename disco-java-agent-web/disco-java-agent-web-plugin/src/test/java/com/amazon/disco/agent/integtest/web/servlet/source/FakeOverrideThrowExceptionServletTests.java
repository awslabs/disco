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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Internal tests for FakeOverrideThrowExceptionServlet;
 * Its service() method call should throw an exception.
 */
public class FakeOverrideThrowExceptionServletTests {

    @Test
    public void testFakeOverrideThrowExceptionServlet() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FakeOverrideThrowExceptionServlet servlet = new FakeOverrideThrowExceptionServlet();
        try {
            servlet.service(request, response);
            // If we execute the next line, then no exception was caught.
            Assert.fail();
        } catch (ServletException | IOException e) {
            // We should reach here.
        }
    }

}
