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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.pool.TypePool;
import software.amazon.disco.agent.DiscoAgentTemplate;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.config.AgentConfigParser;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.interception.EffectVerificationStrategy;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoAgentToLoadException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.TransformationListener;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * Agent loader used to dynamically load a Java Agent at runtime by calling the
 * {@link Injector} api.
 */
public class DiscoAgentLoader implements AgentLoader {
    private final static Logger log = LogManager.getLogger(DiscoAgentLoader.class);

    /**
     * {@inheritDoc}
     * <p>
     * Install an agent by directly invoking the {@link Injector} api.
     */
    @Override
    public void loadAgent(final PreprocessConfig config, Instrumentation instrumentation) {
        if (config == null || config.getAgentPath() == null) {
            throw new NoAgentToLoadException();
        }

        final ClassFileVersion version = parseClassFileVersionFromConfig(config);

        DiscoAgentTemplate.setAgentConfigFactory(() -> {
            final AgentConfig coreConfig = new AgentConfigParser().parseCommandLine(config.getAgentArg());
            coreConfig.setAgentBuilderTransformer(getAgentBuilderTransformer(version));

            return coreConfig;
        });

        // We're loading the agent with an Instrumentation that doesn't actually instrument anything, thus any
        // attempts to verify effects of installation will fail.
        DiscoAgentTemplate.setEffectVerificationStrategy(EffectVerificationStrategy.Standard.NO_VERIFICATION);

        // AgentConfig passed here as String will be ignored by Disco core if AgentConfigFactory is set
        Injector.loadAgent(
            instrumentation,
            config.getAgentPath(),
            null
        );

        log.info(PreprocessConstants.MESSAGE_PREFIX + TransformerExtractor.getTransformers().size() + " Installables loaded.");
    }

    /**
     * Generate a uuid to identify the {@link Installable} being passed in.
     *
     * @param installable an Installable that will have a TransformationListener installed on.
     * @return a uuid that identifies the Installable passed in.
     */
    private static String uuidGenerate(Installable installable) {
        return "mock uuid"; //TODO
    }

    /**
     * Parses a java version supplied by the {@link PreprocessConfig} file. Default is java 8 if not specified.
     *
     * @param config a PreprocessConfig containing information to perform module instrumentation
     * @return a parsed instance of ClassFileVersion
     * @throws InvalidConfigEntryException if supplied config value is invalid
     */
    protected static ClassFileVersion parseClassFileVersionFromConfig(PreprocessConfig config) {
        try {
            if (config.getJavaVersion() == null) {
                log.info(PreprocessConstants.MESSAGE_PREFIX + "Java version to compile transformed classes not specified, set to Java 8 by default");
                return ClassFileVersion.ofJavaVersion(8);
            } else {
                return ClassFileVersion.ofJavaVersion(Integer.parseInt(config.getJavaVersion()));
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigEntryException("java version: " + config.getJavaVersion(), e);
        }
    }

    /**
     * Returns an AgentBuilder transformer that DiscoAgentTemplate will use to transform an AgentBuilder.
     *
     * @param version java version used to compile the transformed classes
     * @return an AgentBuilder transformer suitable for the code InterceptionInstaller.
     */
    private BiFunction<AgentBuilder, Installable, AgentBuilder> getAgentBuilderTransformer(ClassFileVersion version) {
        final ConcurrentMap<ClassLoader, TypePool.CacheProvider> typePoolCache = new ConcurrentHashMap<>();
        typePoolCache.put(getClass().getClassLoader(), new TypePool.CacheProvider.Simple());

        return (agentBuilder, installable) -> agentBuilder
            .with(new ByteBuddy(version))
            .with(new TransformationListener(uuidGenerate(installable)))
            .with(new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(typePoolCache))
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE);
    }
}

