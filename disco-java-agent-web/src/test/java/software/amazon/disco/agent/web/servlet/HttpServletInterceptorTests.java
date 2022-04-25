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

package software.amazon.disco.agent.web.servlet;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.event.AbstractTransactionEvent;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServletNetworkRequestEvent;
import software.amazon.disco.agent.event.HttpServletNetworkResponseEvent;
import software.amazon.disco.agent.event.Listener;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class HttpServletInterceptorTests {
    private TestListener testListener;
    private HttpServlet testServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;

    /**
     * Implemented Servlet class to test that only non-abstract classes are instrumented
     */
    public class ImplementedServlet extends HttpServlet {
        public ImplementedServlet() {
            super();
        }

        @Override
        public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            super.service(req, res);
        }
    }

    @Before
    public void before() {
        EventBus.removeListener(testListener);
        EventBus.addListener(testListener = new TestListener());
        testServlet = new ImplementedServlet();
        request = Mockito.mock(HttpServletRequest.class, Mockito.withSettings());
        response = Mockito.mock(HttpServletResponse.class, Mockito.withSettings());
        Mockito.when((response).getHeaderNames()).thenReturn(Collections.EMPTY_LIST);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("URI"));
        // Custom header
        List<String> headerNames = new ArrayList<>();
        headerNames.add("someheader");
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));
        Mockito.when(request.getHeader("someheader")).thenReturn("somedata");

        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getLocalAddr()).thenReturn("0.0.0.0");
        Mockito.when(request.getRemoteAddr()).thenReturn("1.1.1.1");
        Mockito.when(request.getLocalPort()).thenReturn(80);
        Mockito.when(request.getRemotePort()).thenReturn(100);

        List<String> responseHeaderNames = new ArrayList<>();
        responseHeaderNames.add("someresponseheader");
        Mockito.when(response.getHeaderNames()).thenReturn(responseHeaderNames);
        Mockito.when(response.getHeader("someresponseheader")).thenReturn("somedata");
        Mockito.when(response.getStatus()).thenReturn(200);
    }

    @After
    public void after() {
        EventBus.removeListener(testListener);
    }

//    /**
//     * Implemented Servlet class to test that only non-abstract classes are instrumented
//     */
//    public class ImplementedServlet extends HttpServlet {
//        public ImplementedServlet() {
//            super();
//        }
//
//        @Override

//    }

    /**
     * These methods should never be matched.
     */
    public class RecursiveServlet extends HttpServlet {
        public RecursiveServlet() {
            super();
        }

        public void service(HttpServletRequest req, HttpServletResponse res, int someparam) throws ServletException, IOException {
            super.service(req, res);
        }

        public void service(HttpServletRequest req, HttpServletResponse res, int someparam, int someparam2) throws ServletException, IOException {
            this.service(req, res, someparam);
        }

        public void service(HttpServletRequest req, HttpServletResponse res, int someparam, int someparam2, int someparam3) throws ServletException, IOException {
            this.service(req, res, someparam, someparam2);
        }

        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            return;
        }

    }

    @Test
    public void testMethodMatcherSucceeds() throws Exception {
        Assert.assertTrue(methodMatches("service", ImplementedServlet.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailsOnMethod() throws Exception {
        methodMatches("notAMethod", ImplementedServlet.class);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailsOnClass() throws Exception {
        Assert.assertFalse(methodMatches("service", String.class));
    }

    @Test
    public void testMethodMatcherMultipleService() throws Exception {
        Assert.assertEquals(0, countMatches("service", RecursiveServlet.class));
    }

    @Test
    public void testClassMatcherSucceeds() throws ClassNotFoundException {
        Assert.assertTrue(classMatches(ImplementedServlet.class));
    }

    @Test
    public void testClassMatcherFails() throws ClassNotFoundException {
        Assert.assertFalse(classMatches(String.class));
    }

    @Test
    public void testClassMatcherSuccessOnAbstractType() throws ClassNotFoundException {
        Assert.assertTrue(classMatches(HttpServlet.class));
    }

    /**
     * Helper function to test the method matcher against an input class
     *
     * @param methodName name of method
     * @param paramType class we are verifying contains the method
     * @return true if matches, else false
     * @throws NoSuchMethodException
     */
    private boolean methodMatches(String methodName, Class paramType) throws NoSuchMethodException {
        return countMatches(methodName, paramType) == 1;
    }

    /**
     * Helper function to test how many methods match the matcher.
     *
     * @param methodName
     * @param paramType
     * @return
     * @throws NoSuchMethodException
     */
    private int countMatches(String methodName, Class paramType) throws NoSuchMethodException {
        int count = 0;
        List<Method> methods = new ArrayList<>();
        for (Method m : paramType.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                methods.add(m);
            }
        }


        if (methods.size() == 0) throw new NoSuchMethodException();

        HttpServletServiceInterceptor interceptor = new HttpServletServiceInterceptor();
        for (Method m : methods) {
            if (interceptor.buildMethodMatcher().matches(new MethodDescription.ForLoadedMethod(m))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper function to test the class matcher matching
     *
     * @param clazz Class type we are validating
     * @return true if matches else false
     */
    private boolean classMatches(Class clazz) throws ClassNotFoundException {
        HttpServletServiceInterceptor interceptor = new HttpServletServiceInterceptor() {
        };
        return interceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }
    @Test
    public void testHTTPServletServiceInterceptor() throws Throwable {
        Callable<Object> zuper = () -> null;
        Object[] args = new Object[2];
        args[0] = request;
        args[1] = response;
        HttpServletServiceMethodDelegation.service(args, testServlet,
                "FakeMethodName",
                zuper);

        Assert.assertNotNull(testListener.request);
        Assert.assertEquals(request, testListener.request.getRequest());
        Assert.assertEquals(request.getMethod(), testListener.request.getMethod());
        Assert.assertEquals(request.getHeader("someheader"), testListener.request.getHeaderData("someheader"));
        Assert.assertEquals(request.getLocalAddr(), testListener.request.getLocalIPAddress());
        Assert.assertEquals(request.getRemoteAddr(), testListener.request.getRemoteIPAddress());
        Assert.assertEquals(request.getLocalPort(), testListener.request.getDestinationPort());
        Assert.assertEquals(request.getRemotePort(), testListener.request.getSourcePort());

        Assert.assertNotNull(testListener.response);
        Assert.assertEquals(response, testListener.response.getResponse());
        Assert.assertEquals(response.getHeader("someresponseheader"), testListener.response.getHeaderData("someresponseheader"));
        Assert.assertEquals(response.getStatus(), testListener.response.getStatusCode());
        Assert.assertEquals(200, testListener.response.getStatusCode());  // Explicitly check for 200
        Assert.assertEquals(testListener.request, testListener.response.getHttpRequestEvent());
    }

    @Test
    public void testHTTPServletServiceInterceptorExceptionHandling() {
        Callable<Object> zuper = () -> {
            Mockito.when(response.getStatus()).thenReturn(500);  // Mimic servlet throwing 500 status code.
            throw new ClassCastException();
        };

        Object[] args = new Object[2];
        args[0] = request;
        args[1] = response;

        try {
            HttpServletServiceMethodDelegation.service(args, testServlet, "FakeMethodName", zuper);

            //should throw exception and not run this line
            Assert.fail();
        } catch (Throwable t) {
            Assert.assertThat(t, instanceOf(ClassCastException.class));

            Assert.assertNotNull(testListener.request);
            Assert.assertEquals(request, testListener.request.getRequest());
            Assert.assertEquals(request.getMethod(), testListener.request.getMethod());
            Assert.assertEquals(request.getLocalAddr(), testListener.request.getLocalIPAddress());
            Assert.assertEquals(request.getRemoteAddr(), testListener.request.getRemoteIPAddress());
            Assert.assertEquals(request.getLocalPort(), testListener.request.getDestinationPort());
            Assert.assertEquals(request.getRemotePort(), testListener.request.getSourcePort());

            Assert.assertNotNull(testListener.response);
            Assert.assertEquals(response, testListener.response.getResponse());
            Assert.assertEquals(500, testListener.response.getStatusCode()); // Should be 500 and not 200.
            Assert.assertEquals(testListener.request, testListener.response.getHttpRequestEvent());
        }
    }

    private static class TestListener implements Listener {
        HttpServletNetworkRequestEvent request;
        HttpServletNetworkResponseEvent response;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof HttpServletNetworkRequestEvent) {
                request = (HttpServletNetworkRequestEvent) e;
            } else if (e instanceof HttpServletNetworkResponseEvent) {
                response = (HttpServletNetworkResponseEvent) e;
            } else if (e instanceof AbstractTransactionEvent) {
                //ignore
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }
}