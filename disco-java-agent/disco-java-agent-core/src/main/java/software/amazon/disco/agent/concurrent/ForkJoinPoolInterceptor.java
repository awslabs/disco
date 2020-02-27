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

package software.amazon.disco.agent.concurrent;

import software.amazon.disco.agent.concurrent.decorate.DecoratedCallable;
import software.amazon.disco.agent.concurrent.decorate.DecoratedForkJoinTask;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * ForkJoinPool and ForkJoinTask are a related pair of Java features for dispatching work to pools of threads
 *
 * For ForkJoinPool, several methods are used to dispatch concurrency objects - submit, execute, invoke, invokeAll,
 * which take a variety of input types - Runnables, Callables, ForkJoinTasks and Collections of Callables.
 *
 * The call of such a method indicates an intention to dispatch work, potentially to a supplementary thread, or
 * (at the discretion of the runtime) to the same thread as the call site.
 */
class ForkJoinPoolInterceptor implements Installable {
    public static Logger log = LogManager.getLogger(ForkJoinPoolInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .ignore(not(named("java.util.concurrent.ForkJoinPool")))
                .type(createTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(createRunnableMethodsMatcher())
                            .intercept(Advice.to(RunnableMethodsAdvice.class))
                        .method(createCallableMethodsMatcher())
                            .intercept(Advice.to(CallableMethodsAdvice.class))
                        .method(createCallableCollectionMethodsMatcher())
                            .intercept(Advice.to(CallableCollectionMethodsAdvice.class))
                        .method(createForkJoinTaskMethodsMatcher())
                            .intercept(Advice.to(ForkJoinTaskMethodsAdvice.class))
                );
    }

    /**
     * Create a type matcher matching - exactly - the JDK ForkJoinPool class
     * @return a type matcher as per above
     */
    static ElementMatcher.Junction<? super TypeDescription> createTypeMatcher() {
        return named("java.util.concurrent.ForkJoinPool");
    }

    /**
     * Create a method matcher, matching any of the methods which take a Runnable as an argument, being the
     * execute() and submit() methods
     * @return a method matcher as per above
     */
    static ElementMatcher.Junction<? super MethodDescription> createRunnableMethodsMatcher() {
        return isPublic()
                .and(named(("execute")).or(named("submit")))
                .and(takesArgument(0, Runnable.class)
        );
    }

    /**
     * Create a method matcher, matching any of the methods which take a Callable as an argument, being the
     * submit() method
     * @return a method matcher as per above
     */
    static ElementMatcher.Junction<? super MethodDescription> createCallableMethodsMatcher() {
        return isPublic()
                .and(named("submit"))
                .and(takesArgument(0, Callable.class)
        );
    }

    /**
     * Create a method matcher, matching any of the methods which take a Collection of Callables as an argument, being the
     * invokeAll() method
     * @return a method matcher as per above
     */
    static ElementMatcher.Junction<? super MethodDescription> createCallableCollectionMethodsMatcher() {
        return isPublic()
                .and(named("invokeAll"))
                .and(takesArgument(0, Collection.class)
            );
    }

    /**
     * Create a method matcher, matching any of the methods which take a ForkJoinTask as an argument, being the
     * submit(), invoke() and execute() methods
     * @return a method matcher as per above
     */
    static ElementMatcher.Junction<? super MethodDescription> createForkJoinTaskMethodsMatcher() {
        return isPublic()
                .and(named("invoke").or(named("execute")).or(named("submit")))
                .and(takesArgument(0, named("java.util.concurrent.ForkJoinTask"))
            );
    }

    /**
     * A ByteBuddy Advice class to decorate the incoming Runnable on the methods matched by createRunnableMethodsMatcher
     */
    public static class RunnableMethodsAdvice {
        /**
         * Advice OnMethodEnter to inspect the supplied Runnable and decorate it accordingly
         * @param task the Runnable given
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Runnable task) {
            task = methodEnter(task);
        }

        /**
         * A trampoline method to make debugging possible from within an Advice
         * @param task the Runnable as passed to the Advice OnMethodEnter method
         * @return the decorated Runnable, or the same Runnable if it was already decorated
         */
        public static Runnable methodEnter(Runnable task) {
            return DecoratedRunnable.maybeCreate(task);
        }
    }

    /**
     * A ByteBuddy Advice class to decorate the incoming Runnable on the methods matched by createCallableMethodsMatcher
     */
    public static class CallableMethodsAdvice {
        /**
         * Advice OnMethodEnter to inspect the supplied Callable and decorate it accordingly
         * @param task the Callable given
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Callable task) {
            task = methodEnter(task);
        }

        /**
         * A trampoline method to make debugging possible from within an Advice
         * @param task the Callable as passed to the Advice OnMethodEnter method
         * @return the decorated Callable, or the same Callable if it was already decorated
         */
        public static Callable methodEnter(Callable task) {
            return DecoratedCallable.maybeCreate(task);
        }
    }

    /**
     * A ByteBuddy Advice class to decorate the incoming Runnable on the methods matched by createCallableCollectionMethodsMatcher
     */
    public static class CallableCollectionMethodsAdvice {
        /**
         * Advice OnMethodEnter to inspect the supplied Collection of Callables and decorate each entry within it accordingly
         * @param tasks the Collection of Callables given
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, readOnly = false) Collection<Callable> tasks) {
            tasks = methodEnter(tasks);
        }

        /**
         * A trampoline method to make debugging possible from within an Advice
         * @param tasks the Collection of Callables as passed to the Advice OnMethodEnter method
         * @return the Collection of decorated Callables
         */
        public static Collection<Callable> methodEnter(Collection<Callable> tasks) {
            int size = tasks.size();
            if (size > 0) {
                Collection decorated = new ArrayList(size);
                for (Callable c: tasks) {
                    decorated.add((DecoratedCallable.maybeCreate(c)));
                }
                return decorated;
            }
            //fallback to returning input, if no processing could be performed
            return tasks;
        }
    }

    /**
     * A ByteBuddy Advice class to decorate the incoming ForkJoinTask on the methods matched by createForkJoinTaskMethodsMatcher
     */
    public static class ForkJoinTaskMethodsAdvice {
        /**
         * Advice OnMethodEnter to inspect the supplied ForkJoinTask and decorate it accordingly
         *
         * For ForkJoinTask this is not strictly the Decorator pattern, but rather populating extra metadata fields
         * which were programmatically added to ForkJoinTask itself. Since a ForkJoinTask is not a simple FunctionalInterface
         * it is not convenient to wrap as we do with Runnable and Callable, since we would have to ensure every public
         * method is overloaded with a call to its super. If a future JDK release adds a new public or protected method which
         * our Decoration misses, this will cause perplexing failures. Instrumenting the class with additional fields directly
         * solves for this.
         *
         * @param task the ForkJoinTask given
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.Argument(value = 0, typing = Assigner.Typing.DYNAMIC) Object task) {
            methodEnter(task);
        }

        /**
         * A trampoline method to make debugging possible from within an Advice
         * @param task the ForkJoinTask as passed to the Advice OnMethodEnter method
         * @return the ForkJoinTask, with its added fields populated with the current thread's contextual data.
         */
        public static void methodEnter(Object task) {
            try {
                DecoratedForkJoinTask.Accessor accessor = (DecoratedForkJoinTask.Accessor)task;
                accessor.setDiscoDecoration(DecoratedForkJoinTask.create());
            } catch (Exception e ) {
                log.error("DiSCo(Concurrency) could not propagate context into " + task);
            }
        }
    }
}
