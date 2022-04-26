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


package software.amazon.disco.agent.plugin.source;

import net.bytebuddy.agent.builder.AgentBuilder;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.Package;

import java.util.Arrays;
import java.util.Collection;

public class PluginPackage implements Package {
    @Override
    public Collection<Installable> get() {
        return Arrays.asList(
                new PluginInstallable(),
                new OtherInstallable()
        );
    }

    public static class OtherInstallable implements Installable {
        @Override
        public AgentBuilder install(AgentBuilder agentBuilder) {
            return null;
        }
    }
}
