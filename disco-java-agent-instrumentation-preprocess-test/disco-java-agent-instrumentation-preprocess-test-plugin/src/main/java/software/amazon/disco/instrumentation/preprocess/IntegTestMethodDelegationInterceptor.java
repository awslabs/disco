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
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;

public class IntegTestMethodDelegationInterceptor implements Installable {
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(ElementMatchers.nameStartsWith("software.amazon.disco.instrumentation.preprocess.source.IntegTestMethodDelegationTarget"))
            .transform((builder, typeDescription, classLoader, module) -> {
                ResourcesClassInjector.injectAllClasses(
                    classLoader,
                    IntegTestMethodDelegationInterceptor.class.getClassLoader(),
                    "software.amazon.disco.instrumentation.preprocess.IntegTestDelegation",
                    "software.amazon.disco.instrumentation.preprocess.IntegTestDelegationVoid"
                );

                try {
                    Class<?> methodDelegation = Class.forName("software.amazon.disco.instrumentation.preprocess.IntegTestDelegation", true, classLoader);
                    Class<?> methodDelegationVoid = Class.forName("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationVoid", true, classLoader);

                    return builder
                        // this method delegation returns a String
                        .method(ElementMatchers.named("invokeDelegation"))
                        .intercept(MethodDelegation.to(methodDelegation))

                        // this method delegation returns void.
                        .method(ElementMatchers.named("invokeDelegationVoid"))
                        .intercept(MethodDelegation.to(methodDelegationVoid));
                } catch (Exception e) {
                    return builder;
                }
            });
    }
}
