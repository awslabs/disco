/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import kotlinx.coroutines.ThreadContextElementKt;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.interception.Installable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

/**
 * Interceptor for Kotlin coroutine creation using FutureKt.
 */
public class FutureKtCoroutineInterceptor implements Installable {

    @Override
    public AgentBuilder install(final AgentBuilder agentBuilder) {
        return agentBuilder
            // Disabled the TypeValidation since there are some functions in that class that have invalid method name
            // in java rules. Otherwise, bytebuddy will throw IllegalStateException when transforming this class.
            .with(new ByteBuddy().with(TypeValidation.DISABLED))
            .type(buildFutureKtClassTypeMatcher())
            .transform((builder, typeDescription, classLoader, module) -> builder
                .method(createFutureMethodMatcher())
                .intercept(Advice.to(FutureAdvice.class))
            );
    }

    /**
     * The Advice to intercept the launch method.
     */
    public static class FutureAdvice {
        /**
         * The future method is intercepted, before it runs on the same or different thread.
         * For each time a new coroutine is created we add the TX metadata as a ThreadContextElement.
         * This way we can propagate the TX (private metadata) between the handoff for coroutines.
         *
         * @param coroutineContext the context.
         */
        @Advice.OnMethodEnter
        public static void enter(
            @Advice.Argument(value = 1, readOnly = false) CoroutineContext coroutineContext) {
            coroutineContext = coroutineContext.plus(ThreadContextElementKt.asContextElement(TransactionContext.getPrivateMetadataThreadLocal(), TransactionContext.getPrivateMetadata()));
        }
    }

    static ElementMatcher<TypeDescription> buildFutureKtClassTypeMatcher() {
        return named("kotlinx.coroutines.future.FutureKt");
    }

    public ElementMatcher.Junction<? super MethodDescription> createFutureMethodMatcher() {
        return ElementMatchers.named("future")
            .or(named("future$default"))
            .and(takesArgument(1, named("kotlin.coroutines.CoroutineContext")));
    }
}
