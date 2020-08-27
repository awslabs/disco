/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.concurrent;

import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnableScheduledFuture;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An interceptor for the {@link ScheduledThreadPoolExecutor} to provide TransactionContext propagation.
 *
 * The {@link ScheduledThreadPoolExecutor} is a special case of Executor that provides additional
 * methods for submitting work (schedule and friends) and wraps Runnables and Callables in a
 * {@link RunnableScheduledFuture}, preventing the standard ExecutorInterceptor from handling this case. The
 * ScheduledThreadPoolExecutor however provides decorateTask methods that can be intercepted for ThreadContext
 * propagation.
 */
class ScheduledThreadPoolExecutorInterceptor implements Installable {
    private static Logger log = LogManager.getLogger(ScheduledThreadPoolExecutorInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return InterceptorUtils.configureRedefinition(agentBuilder)
            .type(createTypeMatcher())
            .transform((builder, typeDescription, classLoader, module) -> builder
                .visit(Advice.to(DecorateTaskAdvice.class)
                    .on(createMethodMatcher()))
            );
    }

    /**
     * Advice class to decorate the decorateTask() method of a ScheduledThreadPoolExecutor
     */
    public static class DecorateTaskAdvice {

        /**
         * Advice method to decorate the RunnableScheduledFuture on entry to decorateTask.
         *
         * @param task the actual task executed by the ScheduledThreadPoolExecutor
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 1, readOnly = false) RunnableScheduledFuture task) {
            try {
                task = decorate(task);
            } catch (Throwable t) {
                captureThrowableForDebugging(t);
            }
        }

        /**
         * A trampoline method to make debugging possible from within an Advice.
         *
         * @param task the RunnableScheduledFuture to decorate
         * @return a decorated RunnableScheduledFuture
         */
        public static RunnableScheduledFuture decorate(RunnableScheduledFuture task) {
            return DecoratedRunnableScheduledFuture.maybeCreate(task);
        }

        /**
         * Under normal circumstances should not be called, but for debugging, we call out to a 'real' method
         * @param t the throwable which was thrown by the advice
         */
        public static void captureThrowableForDebugging (Throwable t) {
            log.error("DiSCo(Concurrency) failed to decorate RunnableScheduledFuture for ScheduledThreadPoolExecutor", t);
        }
    }

    /**
     * Create a type matcher for a ScheduledThreadPoolExecutor or any subclass.
     *
     * @return the type matcher
     */
    static ElementMatcher.Junction<? super TypeDescription> createTypeMatcher() {
        return isSubTypeOf(ScheduledThreadPoolExecutor.class);
    }

    /**
     * Create a method matcher to match the decorateTask method (accepting either Callable or Runnable, we don't care).
     *
     * Note that this method is protected, but exists specifically for interception and inspection of the work being
     * submitted to this Executor.
     *
     * @return a method matcher
     */
    static ElementMatcher.Junction<? super MethodDescription> createMethodMatcher() {
        return named("decorateTask");
    }
}

