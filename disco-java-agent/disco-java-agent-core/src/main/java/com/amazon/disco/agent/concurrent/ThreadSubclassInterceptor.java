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
    public static final String ALPHA_ONE_DECORATION_FIELD_NAME = "alphaOneDecoration";

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(createThreadSubclassTypeMatcher())
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .defineField(ALPHA_ONE_DECORATION_FIELD_NAME, DecoratedThread.class, Modifier.PROTECTED)
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
         * Advice OnMethodEnter to populate the AlphaOne context metadata
         * @param alphaOneDecoration
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.FieldValue(readOnly=false, value=ALPHA_ONE_DECORATION_FIELD_NAME) DecoratedThread alphaOneDecoration) {
            alphaOneDecoration = methodEnter();
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
         * @param alphaOneDecoration
         */
        @Advice.OnMethodEnter
        public static void onMethodEnter(@Advice.FieldValue(value=ALPHA_ONE_DECORATION_FIELD_NAME) DecoratedThread alphaOneDecoration) {
            methodEnter(alphaOneDecoration);
        }

        /**
         * Trampoline method to allow debugging Advice method
         * @param alphaOneDecoration the AlphaOne context metadata if it exists
         */
        public static void methodEnter(DecoratedThread alphaOneDecoration) {
            try {
                alphaOneDecoration.before();
            } catch (Exception e) {

            }
        }

        /**
         * Advice OnMethodExit to clear the AlphaOne context metadata once run() has finished.
         * @param alphaOneDecoration
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.FieldValue(value=ALPHA_ONE_DECORATION_FIELD_NAME) DecoratedThread alphaOneDecoration) {
            methodExit(alphaOneDecoration);
        }

        /**
         * Trampoline method to allow debugging Advice method
         * @param alphaOneDecoration the AlphaOne context metadata if it exists
         */
        public static void methodExit(DecoratedThread alphaOneDecoration) {
            try {
                alphaOneDecoration.after();
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
