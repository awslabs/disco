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

package software.amazon.disco.agent.interception.templates.integtest;


import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import software.amazon.disco.agent.interception.templates.DataAccessor;

/**
 * Install an example DataAccessor for the purposes of an 'interception happened' integ test.
 */
public class ExampleAccessorInstaller {
    /**
     * Must be called before any use of the ExampleOuterClass or ExampleDelegatedClass types
     */
    public static void init() {
        DataAccessor da = DataAccessor.forClassNamed("software.amazon.disco.agent.interception.templates.integtest.source.ExampleOuterClass", ExampleAccessor.class);
        AgentBuilder ab = da.install(new AgentBuilder.Default());
        ab.installOn(ByteBuddyAgent.install());
    }
}
