/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.matchers.TrieNameMatcher;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Class to control installation of interceptions/advice on target methods.
 */
public class InterceptionInstaller {
    private static final InterceptionInstaller INSTANCE = new InterceptionInstaller(new DefaultAgentBuilderFactory());
    private static final Logger log = LogManager.getLogger(InterceptionInstaller.class);
    private final Supplier<AgentBuilder> agentBuilderFactory;
    /**
     * Common low-level and otherwise problematic namespaces to ignore.
     */
    private static final String[] IGNORE_PREFIXES = new String[] {"sun.", "com.sun.", "java.lang.ClassLoader$", "jdk.", "org.jacoco.", "org.junit.",
            "org.aspectj.", "software.amazon.disco.agent."};
    private static final ElementMatcher.Junction<? super TypeDescription> TRIE_BASED_IGNORE_MATCHER_INSTANCE = new TrieNameMatcher<>(IGNORE_PREFIXES);

    /**
     * Non-public constructor for singleton semantics. Package-private for tests
     */
    InterceptionInstaller(Supplier<AgentBuilder> agentBuilderFactory) {
        this.agentBuilderFactory = agentBuilderFactory;
    }

    /**
     * Singleton access
     * @return the InterceptionInstaller singleton
     */
    public static InterceptionInstaller getInstance() {
        return INSTANCE;
    }

    /**
     * Sets up the interceptions as configured by the Agent, passing in a list of Installables.
     * @param instrumentation - the Instrumentation instance, as passed to 'premain'
     * @param installables - the collection of Installable hooks passed in from the Agent
     * @param config - the command line config passed into the agent.
     * @param customIgnoreMatcher extra ignore rules to be OR'd with the default
     */
    public void install(Instrumentation instrumentation, Set<Installable> installables, AgentConfig config,
                        ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher) {
        final ElementMatcher<? super TypeDescription> ignoreMatcher = createIgnoreMatcher(customIgnoreMatcher);

        List<ClassFileTransformer> oneShotTransformers = new ArrayList<>();
        List<OneShotInstallable> oneShotInstallables = new ArrayList<>();
        for (Installable installable: installables) {
            //We create a new Agent for each Installable, otherwise their matching rules can
            //compete with each other.
            AgentBuilder agentBuilder = agentBuilderFactory.get().ignore(ignoreMatcher);

            //The Interception listener is expensive during class loading, and limited value most of the time
            if (config.isExtraverbose()) {
                agentBuilder = agentBuilder.with(InterceptionListener.create(installable));
            }

            agentBuilder = config.getAgentBuilderTransformer().apply(agentBuilder, installable);

            log.info("DiSCo(Core) attempting to install " + installable.getClass().getName());
            agentBuilder = installable.install(agentBuilder);

            if (agentBuilder != null) {
                ClassFileTransformer transformer = agentBuilder.installOn(instrumentation);
                if (installable instanceof OneShotInstallable) {
                    oneShotInstallables.add((OneShotInstallable) installable);
                    oneShotTransformers.add(transformer);
                }
            }
        }

        // Give all one-shot installers heads up before we remove their class transformers. To avoid potential
        // non-obvious interactions between different one-shot installables' beforeDisposal(), we start removing
        // class transformers only after all beforeDisposal() have been called.
        for (OneShotInstallable oneShotInstallable: oneShotInstallables) {
            oneShotInstallable.beforeDisposal();
        }
        for (ClassFileTransformer transformer: oneShotTransformers) {
            instrumentation.removeTransformer(transformer);
        }
    }

    /**
     * Create a matcher to ignore low-level and otherwise problematic namespaces.
     *
     * @param customIgnoreMatcher an extra ignore rule to be OR'd with the default
     * @return - a matcher suitable for passing to AgentBuilder#ignore
     */
    public static ElementMatcher.Junction<? super TypeDescription> createIgnoreMatcher(ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher) {
        //TrieNameMatcher - Ignore low-level pieces of the JDK and runtime, 3rd party libraries
        //disco itself and its internals - not to ignore "software.amazon.disco.agent.integtest" to test the interceptors.
        //Required to be added in non-test code, as test code does not have an opportunity to inject or
        //modify these entries as they happen at class loading.
        return (TRIE_BASED_IGNORE_MATCHER_INSTANCE
                    .and(not(nameStartsWith("software.amazon.disco.agent.integtest."))))
                .or(customIgnoreMatcher);
    }

    /**
     * A default Factory for creation of AgentBuilder instances
     */
    private static class DefaultAgentBuilderFactory implements Supplier<AgentBuilder> {
        /**
         * Factory method to produce a real AgentBuilder
         * @return an AgentBuilder in the default case
         */
        @Override
        public AgentBuilder get() {
            return new AgentBuilder.Default();
        }
    }
}
