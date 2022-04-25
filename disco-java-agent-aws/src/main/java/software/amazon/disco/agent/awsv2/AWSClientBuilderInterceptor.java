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

package software.amazon.disco.agent.awsv2;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class AWSClientBuilderInterceptor implements Installable {
    /**
     * Disco logger. Must be public for use in Advice methods.
     */
    public final static Logger log = LogManager.getLogger(AWSClientBuilderInterceptor.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(buildClassMatcher())
            .transform((builder, typeDescription, classLoader, module) -> {
                ResourcesClassInjector.injectAllClasses(
                        classLoader,
                        AWSClientBuilderInterceptor.class.getClassLoader(),
                        "software.amazon.disco.agent.awsv2.DiscoExecutionInterceptor",
                        "software.amazon.disco.agent.event.AwsServiceDownstreamRequestEventImpl",
                        "software.amazon.disco.agent.event.AwsServiceDownstreamResponseEventImpl"
                );

                return new AgentBuilder.Transformer.ForAdvice()
                    .include(this.getClass().getClassLoader())
                    .advice(buildMethodMatcher(), this.getClass().getClasses()[0].getName())
                    .transform(builder, typeDescription, classLoader, module);
                }
            );
    }

    /**
     * Nested delegation class that handles any methods intercepted by the installable. Separate class is necessary
     * so as to not load any AWS SDK V2 classes referenced within eagerly, which would cause ClassNotFoundException
     * for Disco customers using this installable without using the AWS SDK V2.
     */
    public static class AWSClientBuilderInterceptorAdvice {

        /**
         * The SdkClientBuilder#build method is intercepted, and this method is inlined in front of it.
         * This interception occurs once per client created, e.g. it does not need any re-interception
         * for each request.
         *
         * Once intercepted, the original Client Builder is modified to include the DiscoExecutionInterceptor in its
         * execution chain. The DiscoExecutionInterceptor obtains relevant metadata during a
         * client request invocation and publishes it into the AwsServiceDownstreamEvents through the EventBus.
         *
         * @param invoker the object that invoked the build method originally
         * @param origin  identifier of the intercepted method, for debugging/logging
         */
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void enter(@Advice.This final SdkClientBuilder invoker,
                                 @Advice.Origin final String origin)
        {
            if (LogManager.isDebugEnabled()) {
                log.debug("DiSCo(AWSv2) method interception of " + origin);
            }

            ClientOverrideConfiguration configuration = ClientOverrideConfiguration
                    .builder()
                    .addExecutionInterceptor(new DiscoExecutionInterceptor())
                    .build();
            invoker.overrideConfiguration(configuration);
        }
    }

    /**
     * Build a ElementMatcher which defines the kind of class which will be intercepted. Package-private for tests.
     *
     * @return A ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    static ElementMatcher<? super TypeDescription> buildClassMatcher() {
        return hasSuperType(named("software.amazon.awssdk.core.client.builder.SdkClientBuilder"))
                .and(not(isInterface()));
    }

    /**
     * Build an ElementMatcher which will match against the build() method of an Sdk Client Builder.
     * Package-private for tests
     *
     * @return An ElementMatcher suitable for passing to the method() method of a DynamicType.Builder
     */
    static ElementMatcher<? super MethodDescription> buildMethodMatcher() {
        return named("build")
                .and(takesArguments(0))
                .and(isFinal())
                .and(not(isAbstract()));
    }
}
