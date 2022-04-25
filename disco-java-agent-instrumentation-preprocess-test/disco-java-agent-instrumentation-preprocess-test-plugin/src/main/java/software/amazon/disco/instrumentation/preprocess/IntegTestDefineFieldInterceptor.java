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
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.interception.Installable;

import java.lang.reflect.Modifier;
import java.util.Map;

public class IntegTestDefineFieldInterceptor implements Installable {
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
            .type(ElementMatchers.nameStartsWith("software.amazon.disco.instrumentation.preprocess.source" +
                ".IntegTestDefineFieldTarget"))
            .transform((builder, typeDescription, classLoader, module) -> builder
                .defineField("privateField", Object.class, Modifier.PRIVATE)
                .defineField("protectedField", String.class, Modifier.PROTECTED)
                .defineField("publicField", Map.class, Modifier.PUBLIC)
            );
    }
}
