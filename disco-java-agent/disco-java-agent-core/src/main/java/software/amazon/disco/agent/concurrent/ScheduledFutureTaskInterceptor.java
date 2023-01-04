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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.concurrent.decorate.DecoratedScheduledFutureTask;
import software.amazon.disco.agent.interception.InstallationError;
import software.amazon.disco.agent.interception.OneShotInstallable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static software.amazon.disco.agent.concurrent.decorate.DecoratedScheduledFutureTask.DISCO_DECORATION_FIELD_NAME;

/**
 * Intercept ScheduledFutureTask, a private class that's part of the implementation of Java's ScheduledThreadPoolExecutor,
 * in order to implement Context Propagation for its scheduled tasks.
 */
class ScheduledFutureTaskInterceptor implements OneShotInstallable {
    /**
     * Fully qualified name of the class we're instrumenting
     */
    private static final String TARGET_CLASS_FQN = "java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask";

    private static final Logger log = LogManager.getLogger(ScheduledFutureTaskInterceptor.class);

    /**
     * Get the target class object. We opt to use reflection here as the class is private. The alternative to using
     * reflection, creating a dummy ScheduledThreadPoolExecutor and scheduling a dummy task, is less explicit.
     *
     * @return ScheduledFutureTask class object
     * @throws ReflectiveOperationException
     */
    static Class<?> getTargetClass() throws ReflectiveOperationException {
        return Class.forName(TARGET_CLASS_FQN);
    }

    /**
     * Creates a type matcher which matches against ScheduledFutureTask exclusively
     * Note that this class is private, but we have no choice but to instrument it,
     * other than making a big change in how we propagate transaction context in Java concurrency APIs.
     *
     * @return the type matcher per the above
     */
    static ElementMatcher.Junction<? super TypeDescription> createScheduledFutureTaskTypeMatcher() {
        return named(TARGET_CLASS_FQN);
    }

    /**
     * Creates a method matcher to match the run() method of ScheduledFutureTask
     *
     * @return the method matcher per the above
     */
    static ElementMatcher.Junction<? super MethodDescription> createRunMethodMatcher() {
        return named("run").and(takesArguments(0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(createScheduledFutureTaskTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        // Add a protected field to hold Disco transaction context data
                        .implement(DecoratedScheduledFutureTask.Accessor.class)
                        .defineField(DISCO_DECORATION_FIELD_NAME, DecoratedScheduledFutureTask.class, Modifier.PROTECTED)
                        // Add a public getter method for this field
                        .defineMethod(DecoratedScheduledFutureTask.Accessor.GET_DISCO_DECORATION_METHOD_NAME, DecoratedScheduledFutureTask.class, Visibility.PUBLIC)
                        .intercept(FieldAccessor.ofField(DISCO_DECORATION_FIELD_NAME))
                        // Add a public setter method for this field
                        .defineMethod(DecoratedScheduledFutureTask.Accessor.SET_DISCO_DECORATION_METHOD_NAME, void.class, Visibility.PUBLIC)
                        .withParameter(DecoratedScheduledFutureTask.class)
                        .intercept(FieldAccessor.ofField(DISCO_DECORATION_FIELD_NAME))
                        // Add advice to all constructors, to capture the transaction context. In general, the creation of
                        // an object would not necessarily be a signal of the intent to delegate work to another thread.
                        // In this case, it is, because a ScheduledFutureTask is only created on calls to schedule and friends.
                        .visit(Advice.to(ConstructorAdvice.class).on(isConstructor()))
                        // Add advice to the run() method, to set and potentially clear the transaction context
                        .visit(Advice.to(RunAdvice.class).on(createRunMethodMatcher()))
                );
    }

    /**
     * Force the ScheduledFutureTask class to be loaded and transformed.
     */
    @Override
    public void beforeDisposal() {
        Class<?> clazz = null;
        try {
            clazz = getTargetClass();
        } catch (ReflectiveOperationException e) {
            log.error("DiSCo(Concurrency) failed to resolve ScheduledFutureTask class");
        }
        if (clazz != null) {
            OneShotInstallable.forceClassLoad(clazz);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InstallationError> verifyEffect() {
        LinkedList<InstallationError> errors = new LinkedList<>();
        try {
            getTargetClass().getDeclaredField(DISCO_DECORATION_FIELD_NAME);
        } catch (ReflectiveOperationException e) {
            errors.add(new InstallationError(
                    "DiSCo(Concurrency) failed to instrument class java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask"));
        }
        return errors;
    }

    /**
     * ByteBuddy advice to the constructors of ScheduledFutureTask
     */
    public static class ConstructorAdvice {
        @Advice.OnMethodExit
        public static void onMethodExit(@Advice.This Object thiz) {
            methodExit(thiz);
        }

        /**
         * Trampoline method to ease debugging of advice on exit from ScheduledFutureTask constructors
         *
         * @param task the ScheduledFutureTask
         */
        public static void methodExit(Object task) {
            try {
                DecoratedScheduledFutureTask.Accessor accessor = (DecoratedScheduledFutureTask.Accessor) task;
                accessor.setDiscoDecoration(DecoratedScheduledFutureTask.create());
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to capture context in ScheduledFutureTask", e);
            }
        }
    }

    /**
     * ByteBuddy advice to the run() method of a ScheduledFutureTask
     */
    public static class RunAdvice {

        /**
         * Advice OnMethodEnter for the run() method
         *
         * @param thiz the 'this' pointer of the ScheduledFutureTask
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.This Object thiz) {
            methodEnter(thiz);
        }

        @Advice.OnMethodExit
        public static void onMethodExit(@Advice.This Object thiz) {
            methodExit(thiz);
        }

        /**
         * Trampoline method to ease debugging of advice on entry to run() method
         *
         * @param task the ScheduledFutureTask
         */
        public static void methodEnter(Object task) {
            try {
                DecoratedScheduledFutureTask.Accessor accessor = (DecoratedScheduledFutureTask.Accessor) task;
                accessor.getDiscoDecoration().before();
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to propagate context in ScheduledFutureTask", e);
            }
        }

        /**
         * Trampoline method to ease debugging of advice on exit from run() method
         *
         * @param task the ScheduledFutureTask
         */
        public static void methodExit(Object task) {
            try {
                DecoratedScheduledFutureTask.Accessor accessor = (DecoratedScheduledFutureTask.Accessor) task;
                accessor.getDiscoDecoration().after();
            } catch (Exception e) {
                log.error("DiSCo(Concurrency) unable to propagate context in ScheduledFutureTask", e);
            }
        }
    }
}
