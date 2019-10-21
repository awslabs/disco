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

package software.amazon.disco.agent.integtest.web.servlet.source;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This call to service
 */
public class FakeChainedServiceCallServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */

    public void service(HttpServletRequest request, HttpServletResponse response, int param1, List<String> didCallIndicator) throws ServletException, IOException {
        // didCallIndicator used to make sure that this call is made.
        didCallIndicator.add("called");
        super.service(request, response);
    }

    public void service(HttpServletRequest request, HttpServletResponse response, int param1, int param2, List<String> didCallIndicator) throws ServletException, IOException {
        service(request, response, param1, didCallIndicator);
    }

    public void service(HttpServletRequest request, HttpServletResponse response, int param1, int param2, int param3, List<String> didCallIndicator) throws ServletException, IOException {
        service(request, response, param1, param2, didCallIndicator);
    }

    public void service(HttpServletRequest request, HttpServletResponse response, int param1, int param2, int param3, int param4, List<String> didCallIndicator) throws ServletException, IOException {
        service(request, response, param1, param2, param3, didCallIndicator);
    }
}