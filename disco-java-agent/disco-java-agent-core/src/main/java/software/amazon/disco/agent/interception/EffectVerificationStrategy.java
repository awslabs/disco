/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.interception;

import software.amazon.disco.agent.plugin.PluginOutcome;

import java.util.Collection;

/**
 * A strategy that determines how the agent will verify the effects of Installables on the Java runtime environment.
 */
public interface EffectVerificationStrategy {
    /**
     * Verify the effects of installables listed in {@code pluginOutcomes}. Optionally list installation errors in
     * {@code pluginOutcomes}.
     *
     * @param pluginOutcomes Collection of {@code PluginOutcome} where installables to verify are listed.
     */
    void verify(Collection<PluginOutcome> pluginOutcomes);

    /**
     * Standard effect verification strategies.
     */
    enum Standard implements EffectVerificationStrategy {
        /**
         * Don't verify any effects.
         */
        NO_VERIFICATION {
            public void verify(Collection<PluginOutcome> pluginOutcomes) {}
        },

        /**
         * Delegate verification of effects to installables listed in {@code pluginOutcomes}. List any resulting installation
         * errors in {@code pluginOutcomes}.
         *
         * @param pluginOutcomes Plugin outcomes that list installables to verify and where we list installation errors.
         */
        DELEGATE_TO_PLUGINS {
            public void verify(Collection<PluginOutcome> pluginOutcomes) {
                for (PluginOutcome pluginOutcome : pluginOutcomes) {
                    for (Installable installable : pluginOutcome.installables) {
                        pluginOutcome.installationErrors.addAll(installable.verifyEffect());
                    }
                }
            }
        }
    }
}
