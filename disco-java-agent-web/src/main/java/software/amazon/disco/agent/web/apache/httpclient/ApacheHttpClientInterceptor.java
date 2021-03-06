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

import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.web.apache.event.ApacheEventFactory;
import software.amazon.disco.agent.web.apache.utils.HttpRequestAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpResponseAccessor;
import software.amazon.disco.agent.interception.MethodInterceptionCounter;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.concurrent.Callable;

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
    private static final MethodInterceptionCounter METHOD_INTERCEPTION_COUNTER = new MethodInterceptionCounter();
    static final String APACHE_HTTP_CLIENT_ORIGIN = "ApacheHttpClient";

    /**
     * This method is used to replace the Apache http client execute() method.
     * It will record the interaction with the service being called (request / response)
     * and propagate http headers if there is any
     *
     * @param args ByteBuddy will populate this array with the arguments of the intercepted method.
     * @param origin Identifier of the intercepted method, for debugging/logging
     * @param zuper  ByteBuddy supplies a Callable to the intercepted method, due to the @SuperCall annotation
     * @return The object returned by the http client call
     * @throws Throwable The internal call to 'zuper.call()' may throw any Exception
     */
    @SuppressWarnings("unused")
    @RuntimeType
    public static Object intercept(@AllArguments final Object[] args,
                                   @Origin final String origin,
                                   @SuperCall final Callable<Object> zuper) throws Throwable {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Web) interception of " + origin);
        }

        if (METHOD_INTERCEPTION_COUNTER.hasIntercepted()) {
            return call(zuper);
        }

        HttpRequestAccessor requestAccessor = findRequestObject(args);

        // publish request event
        ServiceDownstreamRequestEvent requestEvent = publishRequestEvent(requestAccessor);

        Throwable throwable = null;
        Object response = null;
        try {
            response = call(zuper);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            // publish response event
            HttpResponseAccessor responseAccessor = null;
            //we currently do not support the flavors of execute() which take a ResponseHandler<T> and return the T.
            if (response instanceof HttpResponseAccessor) {
                responseAccessor = (HttpResponseAccessor)response;
            }
            publishResponseEvent(responseAccessor, requestEvent, throwable);
            if (throwable != null) {
                throw throwable;
            }
            return response;
        }
    }

    /**
     * Find the first (presumably only) HttpRequest object in the args passed to the intercepted method. It is assumed that the DataAccessor interceptor is installed, and that therefore
     * this object will be castable to the HttpRequestAccessor type.
     * @param args all arguments passed to the intercepted method
     * @return the found HttpRequest, converted to its HttpRequestAccessor form, or null if none found
     */
    private static HttpRequestAccessor findRequestObject(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (HttpRequestAccessor.class.isAssignableFrom(args[i].getClass())) {
                return (HttpRequestAccessor)args[i];
            }
        }

        return null;
    }

    /**
     * Do the actual downstream call.
     *
     * @param zuper ByteBuddy supplies a Callable to the intercepted method, due to the @SuperCall annotation
     * @return The object returned by the http client call
     * @throws Throwable The internal call to 'zuper.call()' may throw any Exception
     */
    private static Object call(final Callable<Object> zuper) throws Throwable {
        try {
            METHOD_INTERCEPTION_COUNTER.increment();
            // This is specifically for the re-entrancy situation e.g. in a chained method call
            return zuper.call();
        } finally {
            METHOD_INTERCEPTION_COUNTER.decrement();
        }
    }

    /**
     * Publish a {@link ServiceDownstreamRequestEvent}.
     *
     * @param requestAccessor The {@link HttpRequestAccessor}
     * @return The published ServiceDownstreamRequestEvent, which is needed when publishing ServiceDownstreamResponseEvent later
     */
    private static HttpServiceDownstreamRequestEvent publishRequestEvent(final HttpRequestAccessor requestAccessor) {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(APACHE_HTTP_CLIENT_ORIGIN, requestAccessor);
        EventBus.publish(requestEvent);
        return requestEvent;
    }

    /**
     * Publish a {@link ServiceDownstreamResponseEvent}.
     * @param responseAccessor a HttpResponseAccessor to get status code etc.
     * @param requestEvent Previously published ServiceDownstreamRequestEvent
     * @param throwable The throwable if the request fails
     */
    private static void publishResponseEvent(final HttpResponseAccessor responseAccessor, final ServiceDownstreamRequestEvent requestEvent, final Throwable throwable) {
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(responseAccessor, requestEvent, throwable);
        EventBus.publish(responseEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install (final AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(buildMethodMatcher(typeDescription))
                        .intercept(MethodDelegation.to(this.getClass())));
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
