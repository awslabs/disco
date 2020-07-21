/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.loaders.agents;

import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoPathProvidedException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.TransformationListener;

public class DiscoAgentLoaderTest {
    @Test(expected = NoPathProvidedException.class)
    public void testConstructorFailOnNullPaths() throws NoPathProvidedException {
        new DiscoAgentLoader(null);
    }

    @Test
    public void testLoadAgentCallsSetAgentBuilderTransformer(){
        DiscoAgentLoader loader = Mockito.spy(new DiscoAgentLoader("a path"));
        AgentBuilder builder = Mockito.mock(AgentBuilder.class);
        Installable installable = Mockito.mock(Installable.class);

        loader.loadAgent();
        new AgentConfig(null).getAgentBuilderTransformer().apply(builder, installable);

        Mockito.verify(builder).with(Mockito.any(TransformationListener.class));
    }
}
