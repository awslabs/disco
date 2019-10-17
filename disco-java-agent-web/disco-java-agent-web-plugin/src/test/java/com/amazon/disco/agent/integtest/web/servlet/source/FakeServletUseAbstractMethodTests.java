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
public class FakeServletUseAbstractMethodTests {

    @Test
    public void testFakeAbstractMethodTest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FakeServletUseDefaultService servlet = new FakeServletUseDefaultService();

        // Needed for HttpServlet.service() call.
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getProtocol()).thenReturn("HTTP/1.1");

        try {
            servlet.service(request, response);
        } catch (ServletException | IOException e) {
            // Shouldn't have any errors.
            Assert.fail();
        }
    }
}
