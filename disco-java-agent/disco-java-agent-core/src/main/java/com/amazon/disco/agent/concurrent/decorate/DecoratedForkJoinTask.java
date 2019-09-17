package com.amazon.disco.agent.concurrent.decorate;


import java.lang.reflect.Field;

/**
 * ForkJoinTask is an abstract class with many public methods, so cannot enjoy the more natural decoration
 * treatment afforded to Runnables and Callables. Instead of 'decoration' in the strict sense, we - via instrumentation -
 * augment the ForkJoinTask abstract class itself, with the addition threadId and transactionContext fields
 */
public class DecoratedForkJoinTask extends Decorated {
    public static final String ALPHA_ONE_DECORATION_FIELD_NAME = "alphaOneDecoration";

    /**
     * Create AlphaOne propagation metadata on the supplied object, assumed to be a ForkJoinTask
     * @param task the task to decorate
     * @throws Exception reflection exceptions may be thrown
     */
    public static void create(Object task) throws Exception {
        lookup().set(task, new DecoratedForkJoinTask());
    }

    /**
     * Retreive AlphaOne propagation metadata from the supplied object, assumed to be a ForkJoinTask
     * @param task the task from which to obtain the AlphaOne propagation data
     * @return an instance of a 'Decorated'
     * @throws Exception relection exceptions may be thrown
     */
    public static DecoratedForkJoinTask get(Object task) throws Exception {
        return DecoratedForkJoinTask.class.cast(lookup().get(task));
    }

    /**
     * Helper method to lookup the AlphaOne decoration field, which was added to ForkJoinTask during interception
     * by the treatment in ForkJoinTaskInterceptor
     * @return a read/write Field representing the added AlphaOne decoration field
     * @throws Exception reflection exceptions may be thrown
     */
    static Field lookup() throws Exception {
        //have to reflectively lookup the Decorated which exists inside the ForkJoinTask.
        Class fjtClass = Class.forName("java.util.concurrent.ForkJoinTask", true, ClassLoader.getSystemClassLoader());
        Field decoratedField = fjtClass.getDeclaredField(ALPHA_ONE_DECORATION_FIELD_NAME);
        decoratedField.setAccessible(true);
        return decoratedField;
    }
}
