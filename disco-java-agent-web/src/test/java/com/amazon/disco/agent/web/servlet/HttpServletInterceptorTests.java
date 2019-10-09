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

package com.amazon.disco.agent.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HttpServletInterceptorTests {

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
        HttpServletInterceptor interceptor = new HttpServletInterceptor() {
        };
        return interceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }
}