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

package software.amazon.disco.instrumentation.preprocess;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

public class IntegTestImplementInterfaceInterceptor implements Installable {
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(nameStartsWith("software.amazon.disco.instrumentation.preprocess.source.IntegTestImplementInterfaceTarget"))
            .transform((builder, typeDescription, classLoader, module) -> {
                    ResourcesClassInjector.injectAllClasses(
                        classLoader,
                        IntegTestImplementInterfaceInterceptor.class.getClassLoader(),
                        "software.amazon.disco.instrumentation.preprocess.IntegTestDelegation",
                        "software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall"
                    );

                    try {
                        Class<?> methodDelegation = Class.forName("software.amazon.disco.instrumentation.preprocess.IntegTestDelegation", true, classLoader);
                        Class<?> methodDelegationNoSuperCall = Class.forName("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall", true, classLoader);

                        return builder
                            .implement(IntegTestInterface.class)

                            // define the method in target class that overrides the default method from the interface implemented.
                            .defineMethod("getTargetTypeName", String.class, Visibility.PUBLIC)
                            .intercept(MethodDelegation.to(methodDelegation))

                            // define the method in target class that overrides the abstract method from the interface implemented.
                            .defineMethod("abstractMethod", String.class, Visibility.PUBLIC)
                            .withParameters(String.class)
                            .intercept(MethodDelegation.to(methodDelegationNoSuperCall));
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            );
    }

    public interface IntegTestInterface {
        // to be overridden by any class that implements this interface.
        default String getTargetTypeName() {
            return null;
        }

        // return the name of the interface as it is.
        default String getInterfaceName() {
            return IntegTestInterface.class.getName();
        }

        // to be overridden by any class that implements this interface.
        String abstractMethod(String param);
    }
}
