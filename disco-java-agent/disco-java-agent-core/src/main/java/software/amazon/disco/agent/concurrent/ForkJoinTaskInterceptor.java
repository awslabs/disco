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

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import software.amazon.disco.agent.concurrent.decorate.DecoratedForkJoinTask;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;


import java.lang.reflect.Modifier;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static software.amazon.disco.agent.concurrent.decorate.DecoratedForkJoinTask.DISCO_DECORATION_FIELD_NAME;

/**
 * ForkJoinPool and ForkJoinTask are a related pair of Java features for dispatching work to pools of threads
 *
 * For ForkJoinTask, although it has an API surface consisting of many public methods which request thread
 * delegation, the fork() method is the common denominator. All ForkJoinTasks must implement exec(), which is the
 * method called to actually perform the work, and the method which may therefore be in a different thread than that
 * in which fork() was invoked.
 *
 * We hook the fork() method, to populate the DiSCo metadata fields, and the exec() method to consider them
 * for propagation into the new thread context.
 */
class ForkJoinTaskInterceptor implements Installable {
    public static Logger log = LogManager.getLogger(ForkJoinTaskInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                //add extra metadata fields to the ForkJoinTask abstract base, and propagate them
                //when thread handoff occurs. To populate them in the first place, we capture method like
                //ForkJoinTask.fork() and also ForkJoinPool.submit(ForkJoinTask) et al.
                //this diversity of ways to interact with ForkJoinTasks is a nuisance, so the
                //interception treatment in this Installable is incomplete, dealing only with the 'in the
                //new thread' portion. Populating the metadata at hand-off time is achieved in this installable
                //for methods on ForkJoinTask, and elsewhere for methods on ForkJoinPool.
                .type(createForkJoinTaskTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .implement(DecoratedForkJoinTask.Accessor.class)
                        .defineField(DISCO_DECORATION_FIELD_NAME, DecoratedForkJoinTask.class, Modifier.PROTECTED)

                        .defineMethod(DecoratedForkJoinTask.Accessor.GET_DISCO_DECORATION_METHOD_NAME, DecoratedForkJoinTask.class, Visibility.PUBLIC)
                        .intercept(FieldAccessor.ofField(DISCO_DECORATION_FIELD_NAME))

                        .defineMethod(DecoratedForkJoinTask.Accessor.SET_DISCO_DECORATION_METHOD_NAME, void.class, Visibility.PUBLIC).
                            withParameter(DecoratedForkJoinTask.class)
                        .intercept(FieldAccessor.ofField(DISCO_DECORATION_FIELD_NAME))

                        .method(createForkMethodMatcher())
                        .intercept(Advice.to(ForkAdvice.class))
                )

                .type(createForkJoinTaskSubclassTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(createExecMethodMatcher())
                            .intercept(Advice.to(ExecAdvice.class))
                );
    }

    /**
     * A ByteBuddy Advice class to hook the fork() method of a ForkJoinTask
     */
    public static class ForkAdvice {
        /**
         * Advice OnMethodEnter for the fork() method
         * @param thiz the 'this' pointer of the ForkJoinTask
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Object thiz) {
            methodEnter(thiz);
        }

        /**
         * A trampoline method to make debugging possible from Advice
         * @param task the ForkJoinTask
         */
        public static void methodEnter(Object task) {
            try {
                DecoratedForkJoinTask.Accessor accessor = (DecoratedForkJoinTask.Accessor)task;
                accessor.setDiscoDecoration(DecoratedForkJoinTask.create());
            } catch (Exception e) {
                //swallow
            }
        }
    }

    /**
     * A ByteBuddy Advice class to hook the exec() method of any subclass of ForkJoinTask
     */
    public static class ExecAdvice {
        /**
         * Advice OnMethodEnter for the exec() method
         * @param thiz the 'this' pointer of the ForkJoinTask
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Object thiz) {
            methodEnter(thiz);
        }

        /**
         * A trampoline method to make debugging possible from Advice. Contains the impl of copying parent
         * thread context into the new thread.
         * @param task the ForkJoinTask
         */
        public static void methodEnter(Object task) {
            try {
                DecoratedForkJoinTask.Accessor accessor = (DecoratedForkJoinTask.Accessor)task;
                accessor.getDiscoDecoration().before();
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to propagate context in ForkJoinTask");
            }
        }

        /**
         * Advice OnMethodEnter for the exec() method
         * @param thiz the 'this' pointer of the ForkJoinTask
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.This Object thiz) {
                methodExit(thiz);
        }

        /**
         * A trampoline method to make debugging possible from Advice. Clears the thread context from this thread
         * such that it can be reused and given back to the pool
         * @param task the ForkJoinTask
         */
        public static void methodExit(Object task) {
            try {
                DecoratedForkJoinTask.Accessor accessor = (DecoratedForkJoinTask.Accessor)task;
                accessor.getDiscoDecoration().after();
            } catch (Exception e) {
                //swallow
            }
        }
    }

    /**
     * Creates a type matcher which matches against ForkJoinTask exclusively
     * @return the type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createForkJoinTaskTypeMatcher() {
        return named("java.util.concurrent.ForkJoinTask");
    }

    /**
     * Creates a type matcher which matches against any subclass of ForkJoinTask
     * @return the type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createForkJoinTaskSubclassTypeMatcher() {
        return hasSuperType(named("java.util.concurrent.ForkJoinTask"));
    }

    /**
     * Creates a method matcher to match the fork() method of ForkJoinTask
     * @return the method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createForkMethodMatcher() {
        return named("fork").and(takesArguments(0));
    }

    /**
     * Creates a method matcher to match the exec() method of any concrete implementation of it, overridden from ForkJoinTask
     * @return the method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createExecMethodMatcher() {
        return named("exec").and(isOverriddenFrom(named("java.util.concurrent.ForkJoinTask"))).and(not(isAbstract()));
    }

}
