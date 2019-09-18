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

package com.amazon.disco.agent.concurrent.decorate;


import java.lang.reflect.Field;

/**
 * ForkJoinTask is an abstract class with many public methods, so cannot enjoy the more natural decoration
 * treatment afforded to Runnables and Callables. Instead of 'decoration' in the strict sense, we - via instrumentation -
 * augment the ForkJoinTask abstract class itself, with the addition threadId and transactionContext fields
 */
public class DecoratedForkJoinTask extends Decorated {
    public static final String DISCO_DECORATION_FIELD_NAME = "discoDecoration";

    /**
     * Create DiSCo propagation metadata on the supplied object, assumed to be a ForkJoinTask
     * @param task the task to decorate
     * @throws Exception reflection exceptions may be thrown
     */
    public static void create(Object task) throws Exception {
        lookup().set(task, new DecoratedForkJoinTask());
    }

    /**
     * Retreive DiSCo propagation metadata from the supplied object, assumed to be a ForkJoinTask
     * @param task the task from which to obtain the DiSCo propagation data
     * @return an instance of a 'Decorated'
     * @throws Exception relection exceptions may be thrown
     */
    public static DecoratedForkJoinTask get(Object task) throws Exception {
        return DecoratedForkJoinTask.class.cast(lookup().get(task));
    }

    /**
     * Helper method to lookup the DiSCo decoration field, which was added to ForkJoinTask during interception
     * by the treatment in ForkJoinTaskInterceptor
     * @return a read/write Field representing the added DiSCo decoration field
     * @throws Exception reflection exceptions may be thrown
     */
    static Field lookup() throws Exception {
        //have to reflectively lookup the Decorated which exists inside the ForkJoinTask.
        Class fjtClass = Class.forName("java.util.concurrent.ForkJoinTask", true, ClassLoader.getSystemClassLoader());
        Field decoratedField = fjtClass.getDeclaredField(DISCO_DECORATION_FIELD_NAME);
        decoratedField.setAccessible(true);
        return decoratedField;
    }
}
