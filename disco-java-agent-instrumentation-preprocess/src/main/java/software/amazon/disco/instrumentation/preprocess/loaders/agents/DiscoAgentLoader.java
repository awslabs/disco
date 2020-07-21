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

import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoPathProvidedException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.TransformationListener;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * Agent loader used to dynamically load a Java Agent at runtime by calling the
 * {@link Injector} api.
 */
public class DiscoAgentLoader implements AgentLoader {
    protected String path;

    /**
     * Constructor
     *
     * @param path path of the agent to be loaded
     */
    public DiscoAgentLoader(final String path) {
        if (path == null) {
            throw new NoPathProvidedException();
        }
        this.path = path;
    }

    /**
     * {@inheritDoc}
     * Install an agent by directly invoking the {@link Injector} api.
     */
    @Override
    public void loadAgent() {
        final Instrumentation instrumentation = Injector.createInstrumentation();
        Injector.addToBootstrapClasspath(instrumentation, new File(path));

        AgentConfig.setAgentBuilderTransformer(
                (agentBuilder, installable) -> agentBuilder.with(new TransformationListener(uuidGenerate(installable))));

        Injector.loadAgent(instrumentation, path, null);
    }

    /**
     * Generate a uuid to identify the {@link Installable} being passed in.
     *
     * @param installable an Installable that will have a TransformationListener installed on.
     * @return a uuid that identifies the Installable passed in.
     */
    private String uuidGenerate(Installable installable) {
        return "mock uuid"; //TODO
    }
}

