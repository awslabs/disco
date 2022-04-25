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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.util.concurrent.ThreadPoolExecutor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An interceptor for the {@link ThreadPoolExecutor} to ensure that Disco is unobtrusive to user-specific extensions.
 *
 * Users may override {@link ThreadPoolExecutor} and perform their own decoration in the execute() method; therefore,
 * Disco must preserve the type of the Runnable in the lifecycle hooks provided by {@link ThreadPoolExecutor} in particular,
 * beforeExecute() and afterExecute(). We do this by un-decorating the DecoratedRunnable on entrance to these methods.
 */
public class ThreadPoolInterceptor implements Installable {
    private static Logger log = LogManager.getLogger(ThreadPoolInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return InterceptorUtils.configureRedefinition(agentBuilder)
            .type(createTypeMatcher())
            .transform((builder, typeDescription, classLoader, module) -> builder
                .visit(Advice.to(BeforeExecuteAdvice.class)
                    .on(createBeforeExecuteMethodMatcher()))
                .visit(Advice.to(AfterExecuteAdvice.class)
                    .on(createAfterExecuteMethodMatcher()))
            );
    }

    /**
     * Create a type matcher for a ThreadPoolExecutor or any subclass.
     *
     * @return the type matcher
     */
    static ElementMatcher.Junction<? super TypeDescription> createTypeMatcher() {
        return isSubTypeOf(ThreadPoolExecutor.class);
    }

    /**
     * Create a method matcher to match the beforeExecute method.
     *
     * @return a method matcher
     */
    static ElementMatcher.Junction<? super MethodDescription> createBeforeExecuteMethodMatcher() {
        return named("beforeExecute")
            .and(isOverriddenFrom(ThreadPoolExecutor.class))
            .and(not(isAbstract()));
    }

    /**
     * Create a method matcher to match the afterExecute method.
     *
     * @return a method matcher
     */
    static ElementMatcher.Junction<? super MethodDescription> createAfterExecuteMethodMatcher() {
        return named("afterExecute")
            .and(isOverriddenFrom(ThreadPoolExecutor.class))
            .and(not(isAbstract()));
    }

    public static class BeforeExecuteAdvice {
        /**
         * Advice method un-decorate any DecoratedRunnable on entry to BeforeExecute.
         *
         * @param r the runnable that is about to be executed by the ThreadPoolExecutor.
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 1, readOnly = false) Runnable r) {
            r = unDecorate(r);
        }
    }

    public static class AfterExecuteAdvice {
        /**
         * Advice method un-decorate any DecoratedRunnable on entry to AfterExecute.
         *
         * @param r the runnable that was executed by the ThreadPoolExecutor.
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable r) {
            r = unDecorate(r);
        }
    }

    /**
     * Trampoline method to allow debugging of the Advice methods.
     *
     * @param r the runnable passed to beforeExecute or afterExecute
     * @return the underlying Runnable if r is a DecoratedRunnable, else r
     */
    public static Runnable unDecorate(Runnable r) {
        if (r instanceof DecoratedRunnable) {
            return ((DecoratedRunnable) r).getTarget();
        }
        return r;
    }
}
