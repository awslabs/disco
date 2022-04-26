/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.coroutines;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;
import kotlinx.coroutines.ThreadContextElementKt;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.concurrent.MetadataItem;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.interception.Installable;

import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

/**
 * Interceptor for Kotlin coroutines primitives (async, runBlocking, launch).
 */
public class BuildersKtCoroutineInterceptor implements Installable {

    @Override
    public AgentBuilder install(final AgentBuilder agentBuilder) {
        return agentBuilder
            .type(buildBuilderKtClassTypeMatcher())
            .transform((builder, typeDescription, classLoader, module) ->
                builder
                    .method(createAsyncMethodMatcher())
                    .intercept(Advice.to(AsyncAdvice.class))
                    .method(createRunBlockingMethodMatcher())
                    .intercept(Advice.to(RunBlockingAdvice.class))
                    .method(createLaunchMethodMatcher())
                    .intercept(Advice.to(LaunchAdvice.class))
            );
    }

    /**
     * The Advice to intercept the async method.
     */
    public static class AsyncAdvice {
        /**
         * The async method is intercepted, before it runs on the same or different thread.
         * For each time a new coroutine is created we add the TX metadata as a ThreadContextElement.
         * This way we can propagate the TX (private metadata) between the handoff for coroutines.
         *
         * Note: When the intercepted coroutine inherits the parent TX's ThreadContextElement instance, it'll be overridden
         * when calling the plus on coroutineContext.
         *
         * @param coroutineContext the context.
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 1, readOnly = false) CoroutineContext coroutineContext) {
            ThreadContextElement txContextElement = ThreadContextElementKt
                .asContextElement(TransactionContext.getPrivateMetadataThreadLocal(), TransactionContext.getPrivateMetadata());
            coroutineContext = coroutineContext.plus(txContextElement);
        }
    }

    /**
     * The Advice to intercept the runBlocking method.
     */
    public static class RunBlockingAdvice {

        /**
         * The runBlocking method is intercepted, before it runs on the same or different thread.
         * For each time a new coroutine is created we add the TX metadata as a ThreadContextElement.
         * This way we can propagate the TX (private metadata) between the handoff for coroutines.
         *
         * Note: When the intercepted coroutine inherits the parent TX's ThreadContextElement instance, it'll be overridden
         * when calling the plus on coroutineContext.
         *
         * @param coroutineContext the context.
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) CoroutineContext coroutineContext) {
            ThreadContextElement txContextElement = ThreadContextElementKt
                .asContextElement(TransactionContext.getPrivateMetadataThreadLocal(), TransactionContext.getPrivateMetadata());
            coroutineContext = coroutineContext.plus(txContextElement);
        }
    }

    /**
     * The Advice to intercept the launch method.
     */
    public static class LaunchAdvice {
        /**
         * The launch method is intercepted, before it runs on the same or different thread.
         * For each time a new coroutine is created we add the TX metadata as a ThreadContextElement.
         * This way we can propagate the TX (private metadata) between the handoff for coroutines.
         *
         * Note: When the intercepted coroutine inherits the parent TX's ThreadContextElement instance, it'll be overridden
         * when calling the plus on coroutineContext.
         *
         * @param coroutineContext the context.
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 1, readOnly = false) CoroutineContext coroutineContext) {
            ThreadContextElement txContextElement = ThreadContextElementKt
                .asContextElement(TransactionContext.getPrivateMetadataThreadLocal(), TransactionContext.getPrivateMetadata());
            coroutineContext = coroutineContext.plus(txContextElement);
        }
    }

    static ElementMatcher<TypeDescription> buildBuilderKtClassTypeMatcher() {
        return named("kotlinx.coroutines.BuildersKt");
    }

    public ElementMatcher.Junction<? super MethodDescription> createAsyncMethodMatcher() {
        return ElementMatchers.named("async")
            .or(named("async$default"))
            .and(takesArgument(1, named("kotlin.coroutines.CoroutineContext")));
    }

    public ElementMatcher.Junction<? super MethodDescription> createRunBlockingMethodMatcher() {
        return ElementMatchers.named("runBlocking")
            .or(named("runBlocking$default"))
            .and(takesArgument(0, named("kotlin.coroutines.CoroutineContext")));
    }

    public ElementMatcher.Junction<? super MethodDescription> createLaunchMethodMatcher() {
        return ElementMatchers.named("launch")
            .or(named("launch$default"))
            .and(takesArgument(1, named("kotlin.coroutines.CoroutineContext")));
    }
}
