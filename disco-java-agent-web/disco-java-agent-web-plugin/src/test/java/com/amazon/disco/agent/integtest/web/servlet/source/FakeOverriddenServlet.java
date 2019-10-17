package com.amazon.disco.agent.integtest.web.servlet.source;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Do nothing class.
 */
public class FakeOverriddenServlet extends HttpServlet {
    private boolean ranServiceMethod;

    public FakeOverriddenServlet() {
        this.ranServiceMethod = false;
    }

    public boolean didRunService() {
        return this.ranServiceMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ranServiceMethod = true; // Indicator variable to ensure that this was ran.
    }
}
