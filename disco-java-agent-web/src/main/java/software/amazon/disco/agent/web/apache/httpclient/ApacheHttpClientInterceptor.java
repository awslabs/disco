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
package software.amazon.disco.agent.web.apache.httpclient;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;


/**
 * When making a HTTP call using ApacheHttpClient the org.apache.http.client.HttpClient#execute method
 * is intercepted, to allow recording of the call and header propagation.
 *
 * IMPORTANT NOTE:
 *
 * This interceptor has been tested on org.apache.httpcomponents:httpclient 4.5.10 only.
 */
public class ApacheHttpClientInterceptor implements Installable {
    private static final Logger log = LogManager.getLogger(ApacheHttpClientInterceptor.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install (final AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform((builder, typeDescription, classLoader, module) -> {
                    ResourcesClassInjector.injectAllClasses(
                            classLoader,
                            ApacheHttpClientInterceptor.class.getClassLoader(),
                            "software.amazon.disco.agent.web.apache.httpclient.ApacheHttpClientMethodDelegation",
                            "software.amazon.disco.agent.web.apache.event.ApacheEventFactory",
                            "software.amazon.disco.agent.web.apache.event.ApacheHttpServiceDownstreamRequestEvent"
                    );

                    try {
                        Class<?> methodDelegation = Class.forName("software.amazon.disco.agent.web.apache.httpclient.ApacheHttpClientMethodDelegation", true, classLoader);
                        return builder
                                .method(buildMethodMatcher(typeDescription))
                                .intercept(MethodDelegation.to(methodDelegation));
                    } catch (Exception e) {
                        log.error("Disco(Web) could not install ApacheHttpClientMethodDelegation");
                        return builder;
                    }
                });
    }

    /**
     * Build an ElementMatcher which defines the kind of class which will be intercepted. Package-private for tests.
     *
     * @return An ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    static ElementMatcher<? super TypeDescription> buildClassMatcher() {
        ElementMatcher.Junction<TypeDescription> classMatches = ElementMatchers.hasSuperType(ElementMatchers.named("org.apache.http.client.HttpClient"));
        ElementMatcher.Junction<TypeDescription> notInterfaceMatches = ElementMatchers.not(ElementMatchers.isInterface());
        return classMatches.and(notInterfaceMatches);
    }

    /**
     * Build an ElementMatcher which will match against the execute() method
     * with at least one argument having a super type of HttpRequest in the HttpClient class.
     * Package-private for tests.
     * @param typeDescription a description of the class which has been matched for interception, passed in to
     *                        prevent bytebuddy from aggressively matching superclass methods
     * @return An ElementMatcher suitable for passing to the method() method of a DynamicType.Builder
     */
    static ElementMatcher<? super MethodDescription> buildMethodMatcher(TypeDescription typeDescription) {
        ElementMatcher.Junction<TypeDescription> superTypeIsHttpRequestMatches = ElementMatchers.hasSuperType(ElementMatchers.named("org.apache.http.HttpRequest"));
        ElementMatcher.Junction<MethodDescription> anyArgHasSuperTypeIsHttpRequestMatches = ElementMatchers.hasParameters(ElementMatchers.whereAny(ElementMatchers.hasType(superTypeIsHttpRequestMatches)));
        ElementMatcher.Junction<MethodDescription> methodMatches = ElementMatchers.named("execute").and(anyArgHasSuperTypeIsHttpRequestMatches);
        ElementMatcher.Junction<MethodDescription> declaredByClass = ElementMatchers.isDeclaredBy(typeDescription);
        return methodMatches.and(declaredByClass).and(ElementMatchers.not(ElementMatchers.isAbstract()));
    }

}
