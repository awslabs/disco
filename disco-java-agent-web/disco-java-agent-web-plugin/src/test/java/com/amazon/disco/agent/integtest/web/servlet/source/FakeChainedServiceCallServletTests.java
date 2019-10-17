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
