package com.amazon.disco.agent.integtest.web.servlet.source;

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