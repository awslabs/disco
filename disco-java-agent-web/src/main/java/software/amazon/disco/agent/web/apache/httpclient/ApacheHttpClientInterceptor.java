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

import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.web.apache.httpclient.event.ApacheEventFactory;
import software.amazon.disco.agent.web.apache.httpclient.utils.HttpRequestAccessor;
import software.amazon.disco.agent.web.apache.httpclient.utils.HttpResponseAccessor;
import software.amazon.disco.agent.web.apache.httpclient.utils.MethodInterceptionCounter;
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

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * When making a HTTP call using ApacheHttpClient the org.apache.http.client#execute method
 * is intercepted, to allow recording of the call and header propagation.
 *
 * IMPORTANT NOTE:
 *
 * This interceptor has been tested on Apache-HttpComponents-HttpClient 4.5.x only.
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

        HttpRequestAccessor requestAccessor = new HttpRequestAccessor(args);

        // header propagation
        propagateHeaders(requestAccessor);

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
            HttpResponseAccessor responseAccessor = new HttpResponseAccessor(response);
            publishResponseEvent(responseAccessor, requestEvent, throwable);
            if (throwable != null) {
                throw throwable;
            }
            return response;
        }
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
     * Add headers to the http request if there is any.
     *
     * @param requestAccessor The {@link HttpRequestAccessor}
     */
    private static void propagateHeaders(final HttpRequestAccessor requestAccessor) throws Throwable {
        try {
            // Gather the metadata that must be propagated
            final Map<String, Object> headerAttributes = TransactionContext.getMetadataWithTag(TransactionContext.PROPAGATE_IN_REQUEST_TAG);

            // Add the metadata as HTTP headers in the request
            for (final Map.Entry<String, Object> entry : headerAttributes.entrySet()) {
                if (LogManager.isDebugEnabled()) {
                    log.debug(String.format("DiSCo(Web) propagates header [Key]: %s, [Value]: %s",
                            entry.getKey(), entry.getValue()));
                }

                propagateHeader(requestAccessor, entry.getKey(), entry.getValue());
            }
        }
        catch (final Exception e) {
            // Exceptions caused by the interceptor must be shadowed so that the original flow is not affected
            log.error("DiSCo(Web) Failed to propagate headers for Apache http client: " + e.getMessage(), e);
        }
    }

    /**
     * Add a header to the http request.
     *
     * @param requestAccessor The {@link HttpRequestAccessor}
     * @param name The name of the header
     * @param value The value of the header
     */
    private static void propagateHeader(final HttpRequestAccessor requestAccessor, final String name, final Object value) throws Throwable {
        String stringValue;
        if (value == null) {
            stringValue = null;
        } else if (value instanceof String) {
            stringValue = (String) value;
        } else {
            // TODO: better Object to String handling?
            stringValue = value.toString();
        }

        // This will aggressively overwrite the header if it's already there. It's necessary so that we don't end up
        // with duplicate header values by mistake
        requestAccessor.removeHeaders(name);
        requestAccessor.addHeader(name, stringValue);
    }

    /**
     * Publish a {@link ServiceDownstreamRequestEvent}.
     *
     * @param requestAccessor The {@link HttpRequestAccessor}
     * @return The published ServiceDownstreamRequestEvent, which is needed when publishing ServiceDownstreamResponseEvent later
     */
    private static HttpServiceDownstreamRequestEvent publishRequestEvent(final HttpRequestAccessor requestAccessor) throws Throwable {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(requestAccessor.getUri(), requestAccessor.getMethod(), requestAccessor);
        requestEvent.withMethod(requestAccessor.getMethod());
        requestEvent.withUri(requestAccessor.getUri());
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
        HttpServiceDownstreamResponseEvent responseEvent = new HttpServiceDownstreamResponseEvent(APACHE_HTTP_CLIENT_ORIGIN, requestEvent.getService(), requestEvent.getOperation(), requestEvent);
        if(throwable != null) {
            responseEvent.withThrown(throwable);
        }
        responseEvent.withStatusCode(responseAccessor.getStatusCode());
        responseEvent.withContentLength(responseAccessor.getContentLength());
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
