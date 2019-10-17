package com.amazon.disco.agent.integtest.web.servlet.source;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Internal tests for FakeOverriddenServlet
 */
public class FakeOverriddenServletTests {

    @Test
    public void testFakeOverridenServlet() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FakeOverriddenServlet servlet = new FakeOverriddenServlet();
        Assert.assertFalse(servlet.didRunService());

        try {
            servlet.service(request, response);
        } catch (ServletException | IOException e) {
            // Shouldn't have any errors.
            Assert.fail();
        }
        Assert.assertTrue(servlet.didRunService());
    }

}
