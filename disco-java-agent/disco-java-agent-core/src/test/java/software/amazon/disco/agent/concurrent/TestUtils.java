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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.mockito.Mockito;
import software.amazon.disco.agent.interception.Installable;

public final class TestUtils {
    private TestUtils() {

    }

    public static void testInstallableCanBeInstalled(Installable installable) {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.RedefinitionListenable.WithoutBatchStrategy withoutBatchStrategy = Mockito.mock(AgentBuilder.RedefinitionListenable.WithoutBatchStrategy.class);
        AgentBuilder.Identified.Extendable extendable = Mockito.mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        AgentBuilder.Ignored ignored = Mockito.mock(AgentBuilder.Ignored.class);

        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.InitializationStrategy.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.RedefinitionStrategy.class))).thenReturn(withoutBatchStrategy);
        Mockito.when(withoutBatchStrategy.with(Mockito.any(AgentBuilder.TypeStrategy.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        Mockito.when(narrowable.transform(Mockito.any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        Mockito.when(agentBuilder.ignore(Mockito.any(ElementMatcher.class))).thenReturn(ignored);
        Mockito.when(ignored.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);

        AgentBuilder result = installable.install(agentBuilder);
        Assert.assertEquals(extendable, result);
    }
}
