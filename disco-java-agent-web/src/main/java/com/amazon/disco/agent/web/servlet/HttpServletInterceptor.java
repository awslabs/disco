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

import com.amazon.disco.agent.interception.Installable;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Base class for the HTTP Servlet Interceptor for inbound requests and outbound responses.
 */
public abstract class HttpServletInterceptor implements Installable {

    /**
     * Build a ElementMatcher which defines the kind of class which will be intercepted. Package-private for tests.
     *
     * @return A ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    ElementMatcher<? super TypeDescription> buildClassMatcher() {
        return ElementMatchers.hasSuperType(ElementMatchers.named("javax.servlet.http.HttpServlet"));
    }

    /**
     * Build an ElementMatcher which will match against the service() method of an HttpServlet.
     * Package-private for tests
     *
     * @return An ElementMatcher suitable for passing to the method() method of a DynamicType.Builder
     */
    ElementMatcher<? super MethodDescription> buildMethodMatcher() {
        ElementMatcher<? super TypeDescription> requestTypeName = ElementMatchers.named("javax.servlet.http.HttpServletRequest");
        ElementMatcher<? super TypeDescription> responseTypeName = ElementMatchers.named("javax.servlet.http.HttpServletResponse");
        ElementMatcher.Junction<? super MethodDescription> hasTwoArgs = ElementMatchers.takesArguments(2);
        ElementMatcher.Junction<? super MethodDescription> firstArgMatches = ElementMatchers.takesArgument(0, requestTypeName);
        ElementMatcher.Junction<? super MethodDescription> secondArgMatches = ElementMatchers.takesArgument(1, responseTypeName);
        ElementMatcher.Junction<? super MethodDescription> methodMatches = ElementMatchers.named("service").and(hasTwoArgs.and(firstArgMatches.and(secondArgMatches)));
        return methodMatches.and(ElementMatchers.not(ElementMatchers.isAbstract()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(buildMethodMatcher())
                        .intercept(MethodDelegation.to(this.getClass())));
    }
}
