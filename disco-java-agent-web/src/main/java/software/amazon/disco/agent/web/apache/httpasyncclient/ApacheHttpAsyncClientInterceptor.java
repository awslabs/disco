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

package software.amazon.disco.agent.web.apache.httpasyncclient;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.web.apache.event.ApacheEventFactory;
import software.amazon.disco.agent.web.apache.utils.HttpContextAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpRequestAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpResponseAccessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * When making a HTTP call using ApacheHttpAsyncClient
 * the org.apache.http.nio.client.HttpAsyncClient#execute(
 *          org.apache.http.nio.protocol.HttpAsyncRequestProducer       requestProducer
 *          org.apache.http.nio.protocol.HttpAsyncResponseConsumer      responseConsumer
 *          org.apache.http.protocol.HttpContext                        context
 *          org.apache.http.concurrent.FutureCallback                   callback
 *      ) method is intercepted, to allow recording of the call and header propagation.
 *
 * IMPORTANT NOTE:
 *
 * This interceptor has been tested on org.apache.httpcomponents:httpasyncclient 4.+ only.
 */
public class ApacheHttpAsyncClientInterceptor implements Installable {

    public static Logger log = LogManager.getLogger(ApacheHttpAsyncClientInterceptor.class);
    static final String APACHE_HTTP_ASYNC_CLIENT_ORIGIN = "ApacheHttpAsyncClient";

    /**
     * Advice around detected Apache Http Async Client 'execute' method.
     */
    public static class RequestProducerAdvice {

        /**
         * At the start of the `execute` method, decorate the incoming arguments to
         * publish request/response event when async calls happen.
         *
         * @param interceptedMethod     The method which was intercepted
         * @param requestProducer       The HttpAsyncRequestProducer object
         * @param context               The HttpContext object
         * @param futureCallback        The FutureCallback object
         */
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void methodEnter(
                @Advice.Origin Method interceptedMethod,
                @Advice.Argument(value = 0, readOnly = false, typing = Assigner.Typing.DYNAMIC) Object requestProducer,
                @Advice.Argument(2) final Object context,
                @Advice.Argument(value = 3, readOnly = false, typing = Assigner.Typing.DYNAMIC) Object futureCallback) {

            if (LogManager.isDebugEnabled()) {
                log.debug(String.format("DiSCo(Web) interception of (%s#%s).",
                        interceptedMethod.getDeclaringClass(), interceptedMethod.getName()));
            }
            Object[] decoratedArgs = enter(requestProducer, context, futureCallback);
            requestProducer = decoratedArgs[0];
            futureCallback = decoratedArgs[1];
        }

        /**
         * Internal method for dispatching the Advice.OnMethodEnter functionality.
         * called from Advice methods
         *
         * @param requestProducer       The HttpAsyncRequestProducer object
         * @param context               The HttpContext object
         * @param futureCallback        The FutureCallback object
         *
         * @return An array of decorated args
         */
        public static Object[] enter(
                Object requestProducer,
                final Object context,
                Object futureCallback) {

            HttpContextAccessor contextAccessor = new HttpContextAccessor(context);
            Object decoratedRequestProducer = createDecoratedRequestProducer(requestProducer, contextAccessor);
            Object decoratedFutureCallback = createDecoratedFutureCallback(futureCallback, contextAccessor);

            return new Object[] {
                    decoratedRequestProducer,
                    decoratedFutureCallback
            };
        }

        /**
         * At the end of the `execute` method, publish a Response event if there is any throwable.
         *
         * @param throwable     the thrown Throwable if the method is not completing normally
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Thrown final Throwable throwable) {
            exit(throwable);
        }

        /**
         * Internal method for dispatching the Advice.OnMethodExit functionality.
         * called from Advice methods
         *
         * @param throwable     the thrown Throwable if the method is not completing normally
         */
        public static void exit(final Throwable throwable) {
            if (throwable != null) {
                publishResponseEvent(null, null, throwable);
            }
        }

        /**
         * Helper method to create decorated HttpAsyncRequestProducer
         *
         * @return The decorated HttpAsyncRequestProducer object or the original object if someone has already decorated it
         */
        private static Object createDecoratedRequestProducer(Object originalRequestProducer, HttpContextAccessor contextAccessor) {
            if (Proxy.isProxyClass(originalRequestProducer.getClass())) {
                // skip if already decorated
                // catch: there is no way to inject our business logic if the victim app has already decorated this object
                return originalRequestProducer;
            }

            return Proxy.newProxyInstance(
                    originalRequestProducer.getClass().getClassLoader(),
                    new Class[]{ originalRequestProducer.getClass().getSuperclass().getInterfaces()[0] },
                    Handlers.decorateRequestProducer(originalRequestProducer, contextAccessor));
        }

        /**
         * Helper method to create decorated FutureCallback accordingly:
         *      1. if request is sent w/ FutureCallback specified, then decorate to that FutureCallback object
         *      2. otherwise, try to create a new FutureCallback object
         *
         * @return The decorated FutureCallback object or the original object if someone has already decorated it.
         * It could be null if neither FutureCallback object is provided nor the FutureCallback class can be found.
         */
        private static Object createDecoratedFutureCallback(Object originalFutureCallback, HttpContextAccessor contextAccessor) {
            if (originalFutureCallback != null) {
                if (Proxy.isProxyClass(originalFutureCallback.getClass())) {
                    // skip if already decorated
                    // catch: there is no way to inject our business logic if the victim app has already decorated this object
                    return originalFutureCallback;
                }

                // inject response events publishing logic to the originalFutureCallback object
                return Proxy.newProxyInstance(
                        originalFutureCallback.getClass().getClassLoader(),
                        new Class[]{ originalFutureCallback.getClass().getInterfaces()[0] },
                        Handlers.decorateFutureCallback(originalFutureCallback, contextAccessor));
            }
            // create one to inject response events publishing logic when originalFutureCallback is null
            Class<?> futureCallbackClass = maybeFindClass("org.apache.http.concurrent.FutureCallback");
            if (futureCallbackClass != null) {
                return Proxy.newProxyInstance(
                        futureCallbackClass.getClassLoader(),
                        new Class[]{ futureCallbackClass },
                        Handlers.decorateFutureCallback(originalFutureCallback, contextAccessor));
            }
            return null;
        }

        /**
         * Lookup a Class by name if encountering for the first time.
         * @param accessClassName The class name which this accessor accesses
         * @return The Class which this accessor accesses
         */
        private static Class maybeFindClass(final String accessClassName) {
            try {
                return Class.forName(accessClassName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                // do nothing
            }
            return null;
        }
    }

    public static class Handlers {

        /**
         *  Helper method to create a Handler for HttpAsyncRequestProducer.
         */
        static InvocationHandler decorateRequestProducer(final Object requestProducer, final HttpContextAccessor contextAccessor) {
            return new RequestProducerHandler(requestProducer, contextAccessor);
        }

        /**
         * Helper method to create a Handler for FutureCallback.
         */
        static InvocationHandler decorateFutureCallback(final Object futureCallback, final HttpContextAccessor contextAccessor) {
            return new FutureCallbackHandler(futureCallback, contextAccessor);
        }

        /**
         * Helper method that invokes the reflected method and suppresses exceptions
         * from {@link Method#invoke(Object, Object...)} itself e.g. {@link IllegalAccessException}.
         *
         * @param target the object the underlying method is invoked from
         * @param method the {@link Method} instance corresponding to the interface method invoked on the proxy instance.
         * @param args an array of objects containing the values of the arguments passed in the method invocation
         *             on the proxy instance, or null if interface method takes no arguments.
         * @return the value to return from the method invocation on the proxy instance.
         * @throws Throwable the cause of {@link InvocationTargetException}
         */
        private static Object invokeQuietly (final Object target, final Method method, final Object[] args) throws Throwable {
            Object result = null;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } catch (IllegalAccessException e) {
                // do nothing
            }
            return result;
        }

        /**
         * A Dynamic Proxy which deals with request event publishing for any generated HttpRequest.
         */
        private static class RequestProducerHandler implements InvocationHandler {
            private final Object requestProducer;
            private final HttpContextAccessor contextAccessor;

            /**
             * Constructs a new Handler to publish request events on HttpRequest object generated.
             */
            public RequestProducerHandler(final Object requestProducer, final HttpContextAccessor contextAccessor) {
                this.requestProducer = requestProducer;
                this.contextAccessor = contextAccessor;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Object result = invokeQuietly(requestProducer, method, args);
                if ("generateRequest".equals(method.getName())) {
                    HttpRequestAccessor requestAccessor = new HttpRequestAccessor(result);
                    HttpServiceDownstreamRequestEvent requestEvent = publishRequestEvent(requestAccessor);
                    contextAccessor.setRequestEvent(requestEvent);
                }
                return result;
            }
        }

        /**
         * A Dynamic Proxy which deals with response event publishing for any FutureCallback state change.
         */
        private static class FutureCallbackHandler implements InvocationHandler {
            private final Object futureCallback;
            private final HttpContextAccessor contextAccessor;

            /**
             * Constructs a new Handler to publish response events on FutureCallback state change.
             */
            public FutureCallbackHandler(final Object futureCallback, final HttpContextAccessor contextAccessor) {
                this.futureCallback = futureCallback;
                this.contextAccessor = contextAccessor;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (Arrays.asList("completed", "failed", "cancelled").contains(method.getName())) {
                    HttpServiceDownstreamRequestEvent requestEvent = contextAccessor.removeRequestEvent();
                    HttpResponseAccessor responseAccessor = contextAccessor.getResponseAccessor();
                    if ("completed".equals(method.getName())) {
                        publishResponseEvent(responseAccessor, requestEvent, null);
                    } else if ("failed".equals(method.getName())) {
                        publishResponseEvent(responseAccessor, requestEvent, (Exception) args[0]);
                    } else if ("cancelled".equals(method.getName())) {
                        // TODO: better way to describe this behavior?
                        publishResponseEvent(responseAccessor, requestEvent, null);
                    }
                }

                if (futureCallback ==  null) {
                    return null;
                }

                return invokeQuietly(futureCallback, method, args);
            }
        }
    }

    /**
     * Publish a {@link ServiceDownstreamRequestEvent}.
     *
     * @param requestAccessor The {@link HttpRequestAccessor}
     * @return The published ServiceDownstreamRequestEvent, which is needed when publishing ServiceDownstreamResponseEvent later
     */
    private static HttpServiceDownstreamRequestEvent publishRequestEvent(final HttpRequestAccessor requestAccessor) {
        HttpServiceDownstreamRequestEvent requestEvent = ApacheEventFactory.createDownstreamRequestEvent(APACHE_HTTP_ASYNC_CLIENT_ORIGIN, requestAccessor.getUri(), requestAccessor.getMethod(), requestAccessor);
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
        HttpServiceDownstreamResponseEvent responseEvent = new HttpServiceDownstreamResponseEvent(APACHE_HTTP_ASYNC_CLIENT_ORIGIN, null, null, requestEvent);
        if (requestEvent != null) {
            responseEvent = new HttpServiceDownstreamResponseEvent(APACHE_HTTP_ASYNC_CLIENT_ORIGIN, requestEvent.getService(), requestEvent.getOperation(), requestEvent);
        }

        if(throwable != null) {
            responseEvent.withThrown(throwable);
        }

        if (responseAccessor != null) {
            responseEvent.withStatusCode(responseAccessor.getStatusCode());
            responseEvent.withContentLength(responseAccessor.getContentLength());
        }
        EventBus.publish(responseEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install (AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(buildMethodMatcher(typeDescription))
                        .intercept(Advice.to(RequestProducerAdvice.class).withExceptionPrinting()));
    }

    /**
     * Build an ElementMatcher which defines the kind of class which will be intercepted. Package-private for tests.
     *
     * @return An ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    static ElementMatcher<? super TypeDescription> buildClassMatcher() {
        ElementMatcher.Junction<TypeDescription> classMatches = hasSuperType(named("org.apache.http.nio.client.HttpAsyncClient"));
        ElementMatcher.Junction<TypeDescription> notInterfaceMatches = not(isInterface());
        return classMatches.and(notInterfaceMatches);
    }

    /**
     * Build an ElementMatcher which will match against the execute() method. Package-private for tests.
     *
     * @return An ElementMatcher suitable for passing to the method() method of a DynamicType.Builder
     */
    static ElementMatcher<? super MethodDescription> buildMethodMatcher(TypeDescription typeDescription) {
        return isMethod()
                .and(named("execute"))
                .and(isDeclaredBy(typeDescription))
                .and(not(isAbstract()))
                .and(takesArguments(4))
                .and(takesArgument(0, named("org.apache.http.nio.protocol.HttpAsyncRequestProducer")))
                .and(takesArgument(1, named("org.apache.http.nio.protocol.HttpAsyncResponseConsumer")))
                .and(takesArgument(2, named("org.apache.http.protocol.HttpContext")))
                .and(takesArgument(3, named("org.apache.http.concurrent.FutureCallback")));
    }
}
