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


package software.amazon.disco.agent.config;

import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.interception.Installable;

import java.util.function.BiFunction;

public class AgentConfigTest {
    static AgentConfig config = new AgentConfig(null);

    @Test
    public void testDefaultAgentBuilderTransformer() {
        Assert.assertNotNull(config.getAgentBuilderTransformer());
    }

    @Test
    public void testSetNonDefaultAgentBuilderTransformer() {
        BiFunction<AgentBuilder, Installable, AgentBuilder> defaultTransformer = config.getAgentBuilderTransformer();
        BiFunction<AgentBuilder, Installable, AgentBuilder> mockTransformer = Mockito.mock(BiFunction.class);

        config.setAgentBuilderTransformer(mockTransformer);

        Assert.assertNotEquals(defaultTransformer, config.getAgentBuilderTransformer());
    }

    @Test
    public void testSetNullAgentBuilderTransformerResetsToDefaultTransformer(){
        config.setAgentBuilderTransformer(null);

        Assert.assertNotNull(config.getAgentBuilderTransformer());
    }
}
