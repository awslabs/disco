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

package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import com.amazon.disco.agent.interception.Installable;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.concurrent.Executor;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Executor is a fundamental concurrency primitive in Java, and is the base class of all ExecutorServices, as
 * well as ForkJoinPool.
 *
 * When any method is submitted to the execute() method, it has the semantics of "will happen in the future, perhaps
 * in a different thread". This interface method then has a strong heuristic relationship with the intent to delegate
 * a task to a different thread. We decorate any passed in Runnable object, on that basis.
 */
class ExecutorInterceptor implements Installable {
    private static Logger log = LogManager.getLogger(ExecutorInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                //As with the similar code in ThreadInterceptor, we handle situations where given Executors
                //may already have been used e.g. we know for sure that AspectJ has use-cases where it instantiates
                //a ThreadPoolExecutor. This gives AlphaOne a responsibility to adopt an interception strategy where
                //we can transform a class that is already loaded. The code below achieves that.
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)

                .type(createTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                    .visit(Advice.to(ExecuteAdvice.class)
                        .on(createMethodMatcher()))
                );

    }

    /**
     * Advice class to decorate the execute() method of any implementation of the Executor interface
     */
    public static class ExecuteAdvice {
        /**
         * ByteBuddy advice method to capture the Runnable before it is used, and decorate it.
         *
         * @param command - the command passed to Executor#execute(). Marked as "readonly=false" so that we can
         *                write to it. The bytecode of this method is inlined as a method prologue of the target
         *                method, which is why assigning a new value to an argument has mutable characteristics.
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable command) {
            try {
                command = methodEnter(command);
            } catch (Throwable t) {
                captureThrowableForDebugging(t);
            }
        }

        /**
         * Trampoline method out of the advice method, to allow debugging which is otherwise not possible with the
         * inlined bytecode produced.
         *
         * @param command the incoming command
         * @return the decorated command
         */
        public static Runnable methodEnter(Runnable command) {
            return DecoratedRunnable.maybeCreate(command);
        }

        /**
         * Under normal circumstances should not be called, but for debugging, we call out to a 'real' method
         * @param t the throwable which was thrown by the advice
         */
        public static void captureThrowableForDebugging (Throwable t) {
            log.error("AlphaOne(Concurrency) failed to decorate Runnable for Executor", t);
        }
    }

    /**
     * Create a type matcher to find all implementations of java.util.concurrent.Executor
     * @return a type matcher as above
     */
    static ElementMatcher.Junction<? super TypeDescription> createTypeMatcher() {
        return isSubTypeOf(Executor.class);
        //TODO should we exclude ForkJoinPool here, due to it being an impl of Executor, but handled elsewhere?
    }

    /**
     * Create a method matcher to match any concrete implementation of the Executor#execute() method
     * @return the method matcher as above
     */
    static ElementMatcher.Junction<? super MethodDescription> createMethodMatcher() {
        return named("execute").and(isOverriddenFrom(Executor.class)).and(not(isAbstract()));
    }
}

