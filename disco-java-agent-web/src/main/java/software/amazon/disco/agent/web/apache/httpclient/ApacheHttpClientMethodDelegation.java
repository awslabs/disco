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

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.interception.MethodInterceptionCounter;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.web.apache.event.ApacheEventFactory;

import java.util.concurrent.Callable;

/**
 * This class is to support Bytebuddy's method delegation for {@link ApacheHttpClientInterceptor}
 * When the execute() method of ApacheHttpClient or subclass of it is called,
 */
public class ApacheHttpClientMethodDelegation {
    private static final Logger log = LogManager.getLogger(ApacheHttpClientInterceptor.class);
    private static final MethodInterceptionCounter METHOD_INTERCEPTION_COUNTER = new MethodInterceptionCounter();
    static final String APACHE_HTTP_CLIENT_ORIGIN = "ApacheHttpClient";

    /**
     * This method is used to replace the Apache http client execute() method.
     * It will record the interaction with the service being called (request / response)
     * and propagate http headers if there is any
     *
     * @param args   ByteBuddy will populate this array with the arguments of the intercepted method.
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

        HttpRequest httpRequest = findRequestObject(args);
        // publish request event
        ServiceDownstreamRequestEvent requestEvent = publishRequestEvent(httpRequest);
        Throwable throwable = null;
        Object response = null;
        try {
            response = call(zuper);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            // publish response event
            HttpResponse httpResponse = null;
            //we currently do not support the flavors of execute() which take a ResponseHandler<T> and return the T.
            if (response instanceof HttpResponse) {
                httpResponse = (HttpResponse) response;
            }
            publishResponseEvent(httpResponse, requestEvent, throwable);
            if (throwable != null) {
                throw throwable;
            }
            return response;
        }
    }

    /**
     * Find the first (presumably only) HttpRequest object in the args passed to the intercepted method.
     * this object will be castable to the HttpRequest type.
     *
     * @param args all arguments passed to the intercepted method
     * @return the found HttpRequest
     */
    private static HttpRequest findRequestObject(Object[] args) {
        for (Object arg : args) {
            if (HttpRequest.class.isAssignableFrom(arg.getClass())) {
                return (HttpRequest) arg;
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
     * @param request The {@link HttpRequest}
     * @return The published ServiceDownstreamRequestEvent, which is needed when publishing ServiceDownstreamResponseEvent later
     */
    private static HttpServiceDownstreamRequestEvent publishRequestEvent(final HttpRequest request) {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(APACHE_HTTP_CLIENT_ORIGIN, request);
        EventBus.publish(requestEvent);
        return requestEvent;
    }

    /**
     * Publish a {@link ServiceDownstreamResponseEvent}.
     *
     * @param response     a HttpResponse to get status code etc.
     * @param requestEvent Previously published ServiceDownstreamRequestEvent
     * @param throwable    The throwable if the request fails
     */
    private static void publishResponseEvent(final HttpResponse response, final ServiceDownstreamRequestEvent requestEvent, final Throwable throwable) {
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(response, requestEvent, throwable);
        EventBus.publish(responseEvent);
    }
}
