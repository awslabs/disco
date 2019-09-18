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

import com.amazon.disco.agent.concurrent.decorate.DecoratedThread;
import com.amazon.disco.agent.interception.Installable;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Modifier;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A companion to ThreadInterceptor, it is possible to subclass a Thread directly, instead of supplying a Runnable
 * target during construction. Here we intercept subclasses of Thread, to instrument their run() method accordingly.
 */
class ThreadSubclassInterceptor implements Installable {
    public static final String DISCO_DECORATION_FIELD_NAME = "discoDecoration";

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(createThreadSubclassTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .defineField(DISCO_DECORATION_FIELD_NAME, DecoratedThread.class, Modifier.PROTECTED)
                        .method(createStartMethodMatcher())
                        .intercept(Advice.to(StartAdvice.class))
                        .method(createRunMethodMatcher())
                        .intercept(Advice.to(RunAdvice.class))
                );
    }

    /**
     * ByteBuddy Advice class to instrument the start() method of a Thread subclass
     */
    public static class StartAdvice {
        /**
         * Advice OnMethodEnter to populate the DiSCo context metadata
         * @param discoDecoration
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.FieldValue(readOnly=false, value= DISCO_DECORATION_FIELD_NAME) DecoratedThread discoDecoration) {
            discoDecoration = methodEnter();
        }

        /**
         * Trampoline method to allow debugging inside Advice
         * @return a new Decoration object to populate the thread with
         */
        public static DecoratedThread methodEnter() {
            try {
                return new DecoratedThread();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * ByteBuddy Advice class to instrument the run() method of a Thread subclass (which is inherited from Runnable)
     */
    public static class RunAdvice {
        /**
         *
         * @param discoDecoration
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.FieldValue(value= DISCO_DECORATION_FIELD_NAME) DecoratedThread discoDecoration) {
            methodEnter(discoDecoration);
        }

        /**
         * Trampoline method to allow debugging Advice method
         * @param discoDecoration the DiSCo context metadata if it exists
         */
        public static void methodEnter(DecoratedThread discoDecoration) {
            try {
                discoDecoration.before();
            } catch (Exception e) {

            }
        }

        /**
         * Advice OnMethodExit to clear the DiSCo context metadata once run() has finished.
         * @param discoDecoration
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.FieldValue(value= DISCO_DECORATION_FIELD_NAME) DecoratedThread discoDecoration) {
            methodExit(discoDecoration);
        }

        /**
         * Trampoline method to allow debugging Advice method
         * @param discoDecoration the DiSCo context metadata if it exists
         */
        public static void methodExit(DecoratedThread discoDecoration) {
            try {
                discoDecoration.after();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Create a type matcher which will match against any subclass of Thread, but not Thread itself
     * @return a type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createThreadSubclassTypeMatcher() {
        return hasSuperType(named("java.lang.Thread"))
            .and(not(named("java.lang.Thread")));
    }

    /**
     * Create a method matcher which will match the Runnable#run() method which the Thread has by inheritance
     * from Runnable
     * @return a method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createRunMethodMatcher() {
        return named("run").and(takesArguments(0));
    }

    /**
     * Creates a method matcher to match the start() method of a Thread
     * @return method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createStartMethodMatcher() {
        return named("start").and(takesArguments(0));
    }

}
