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
import software.amazon.disco.agent.concurrent.decorate.Decorated;
import software.amazon.disco.agent.concurrent.decorate.DecoratedForkJoinTask;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * All ForkJoinTasks must implement exec(), which is the method called to actually perform the work, and the method which
 * may therefore be in a different thread than that in which fork() was invoked. We hook the exec() method to populate the
 * DecoratedForkJoinTask instance.
 */
class ForkJoinTaskSubclassInterceptor implements Installable {
    public static Logger log = LogManager.getLogger(ForkJoinTaskSubclassInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(createForkJoinTaskSubclassTypeMatcher())
            .transform((builder, typeDescription, classLoader, module) -> builder
                    .method(createExecMethodMatcher())
                    .intercept(Advice.to(ForkJoinTaskSubclassInterceptor.ExecAdvice.class))
            );
    }

    /**
     * Creates a type matcher which matches against any subclass of ForkJoinTask
     * @return the type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createForkJoinTaskSubclassTypeMatcher() {
        return hasSuperType(named("java.util.concurrent.ForkJoinTask"));
    }

    /**
     * Creates a method matcher to match the exec() method of any concrete implementation of it, overridden from ForkJoinTask
     * @return the method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createExecMethodMatcher() {
        return named("exec").and(isOverriddenFrom(named("java.util.concurrent.ForkJoinTask"))).and(not(isAbstract()));
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

                //in the case of a FJT which was originally a Runnable submitted to a ForkJoinPool, becoming an AdaptedRunnable,
                //the fork() method has not been called, and this is null. TX propagation will be handled by the FJP, not the FJT in this case.
                Decorated decorated = accessor.getDiscoDecoration();
                if (decorated != null) {
                    decorated.before();
                }
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to propagate context in ForkJoinTask", e);
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

                Decorated decorated = accessor.getDiscoDecoration();
                if (decorated != null) {
                    decorated.after();
                }
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to propagate context in ForkJoinTask", e);
            }
        }
    }
}
