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

import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoPathProvidedException;

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
     * Install a monolithic agent by directly invoking the {@link Injector} api.
     */
    @Override
    public void loadAgent() {
        Injector.loadAgent(path, null);
    }
}

