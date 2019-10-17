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
