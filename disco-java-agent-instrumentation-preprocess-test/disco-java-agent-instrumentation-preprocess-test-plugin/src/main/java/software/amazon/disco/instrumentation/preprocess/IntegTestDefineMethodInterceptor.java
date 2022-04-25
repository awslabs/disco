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
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;

public class IntegTestDefineMethodInterceptor implements Installable {
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(ElementMatchers.nameStartsWith("software.amazon.disco.instrumentation.preprocess.source.IntegTestDefineMethodTarget"))
            .transform((builder, typeDescription, classLoader, module) ->
                {
                    ResourcesClassInjector.injectAllClasses(
                        classLoader,
                        IntegTestImplementInterfaceInterceptor.class.getClassLoader(),
                        "software.amazon.disco.instrumentation.preprocess.IntegTestDelegation",
                        "software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall"
                    );

                    try {
                        Class<?> methodDelegationNoSuperCall = Class.forName("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall", true, classLoader);

                        return builder
                            .defineMethod("getPrivateField", String.class, Visibility.PUBLIC)
                            .intercept(FieldAccessor.ofField("field"))

                            .defineMethod("setPrivateField", void.class, Visibility.PUBLIC)
                            .withParameter(String.class)
                            .intercept(FieldAccessor.ofField("field"))

                            // intercept method via a method delegation which has no reference to the original method.
                            .defineMethod("invokeMethodDelegation", String.class, Visibility.PUBLIC)
                            .withParameter(Object.class)
                            .intercept(MethodDelegation.to(methodDelegationNoSuperCall));
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            );
    }
}
