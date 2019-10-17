package com.amazon.disco.agent.integtest.web.servlet.source;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Test to ensure that if the service throws an exception, the event is still generated
 */
public class FakeOverrideThrowExceptionServlet extends HttpServlet {

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        throw new ServletException();
    }
}
