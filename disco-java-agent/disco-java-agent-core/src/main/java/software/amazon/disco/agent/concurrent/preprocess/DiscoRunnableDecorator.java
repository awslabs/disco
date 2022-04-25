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

package software.amazon.disco.agent.concurrent.preprocess;

import java.util.function.BiFunction;

/**
 * Class implemented to create an indirection when decorating the target field(a Runnable) of a given Thread. If the 'maybeDecorateFunction' field
 * is NOT set, the 'maybeDecorate()' method will return the original passed in Runnable, otherwise, the 'maybeDecorateFunction' will be invoked to decorate
 * the passed in Runnable.
 *
 * The reason for this indirection is due to the fact that some dependencies are required upfront during the JVM bootstrap phase of a Build-Time instrumented service, more specifically,
 * when loading/initializing the Thread class. At this stage in time, the Disco agent, which contains dependencies such as Disco core, ByteBuddy and ASM, has yet been installed, making all those
 * dependencies unresolvable.
 *
 * This indirection therefore guarantees that dependencies such as 'DecoratedRunnable' will only be required after the Disco agent's premain has been called by the JVM, and that the
 * 'maybeDecorateFunction' has been set by 'DiscoAgentTemplate#install()'.
 */
public class DiscoRunnableDecorator {
    // First argument is the Runnable to be decorated, second argument is whether removeTransactionContext should be called
    protected static BiFunction<Runnable, Boolean, Runnable> maybeDecorateFunction;

    /**
     * Method to be invoked when {@link Thread#start()} from an instrumented Thread is called. The removal of the TransactionContext of the decorated Runnable is delegated
     * to the 'DecoratedRunnable' class itself to so that 'DecoratedRunnable' is no longer a dependency of the instrumented Thread.
     *
     * @param target Runnable to be decorated when applicable
     * @return a DecoratedRunnable when applicable or the original Runnable
     */
    public static Runnable maybeDecorate(Runnable target) {
        if (maybeDecorateFunction != null) {
            try {
                return maybeDecorateFunction.apply(target, true);
            } catch (Throwable t) {
                return target;
            }
        }
        return target;
    }

    /**
     * Sets the function responsible for decorating Runnable. This will be called by the DiscoJavaAgent inside DiscoAgentTemplate#install().
     *
     * @param function function to be set by 'DiscoAgentTemplate'
     */
    public static void setDecorateFunction(BiFunction function) {
        maybeDecorateFunction = function;
    }
}
