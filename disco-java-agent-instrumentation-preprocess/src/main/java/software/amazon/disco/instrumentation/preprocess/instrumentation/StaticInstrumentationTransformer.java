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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.AgentLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationOutcome.Status;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.AgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.ClassFileLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.DirectoryLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JDKModuleLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible to orchestrate the instrumentation process involving agent loading, package loading, instrumentation
 * triggering and exporting the transformed classes.
 * <p>
 * At least one valid {@link AgentLoader} AND one {@link ClassFileLoader} must be provided (either a service package loader
 * or a dependency package loader).
 */
@Builder
@AllArgsConstructor
public class StaticInstrumentationTransformer {
    public static final String INSTRUMENTED_JDK_RELATIVE_PATH = "jdk";
    private static final Logger log = LogManager.getLogger(StaticInstrumentationTransformer.class);

    private final PreprocessConfig config;
    private final List<InstrumentationOutcome> sourcesFailedToBeInstrumented = new ArrayList<>();
    private final List<InstrumentationOutcome> sourcesInstrumented = new ArrayList<>();
    private final List<InstrumentationOutcome> signedJarsInstrumented = new ArrayList<>();
    private final List<InstrumentationOutcome> signedJarsDiscovered = new ArrayList<>();

    private final AgentLoader agentLoader;

    @Singular
    private final Map<Class<? extends ClassFileLoader>, ClassFileLoader> classFileLoaders;

    @Getter
    private final List<InstrumentationOutcome> allOutcomes = new ArrayList<>();

    /**
     * This method initiates the transformation process of all packages found under the provided paths. All Runtime exceptions
     * thrown by the library are handled in this method. A detailed error message along with any available Cause will be logged
     * and trigger the program to exit with status 1
     */
    public void transform() {
        log.info("Initiating build time instrumentation...");
        if (config == null) {
            throw new InvalidConfigEntryException("No configuration provided", null);
        }

        if (agentLoader == null) {
            throw new AgentLoaderNotProvidedException();
        }

        agentLoader.loadAgent(config, new TransformerExtractor(Injector.createInstrumentation()));

        processAllSources();

        logInstrumentationSummary();
    }

    /**
     * Process all sources to be statically instrumented, including the JDK itself if path to java home is supplied.
     */
    protected void processAllSources() {
        final List<InstrumentationTask> tasks = new ArrayList<>();

        // each map entry represents a collection of sources to be processed that share the same relative output path. For example, a collection of Jars
        // that will all end up in 'lib'.
        for (final Map.Entry<String, Set<String>> entry : config.getSourcePaths().entrySet()) {
            for (final String source : entry.getValue()) {
                try {
                    final Path pathToSrc = Paths.get(source);
                    final Path actualPathToSrc = Files.isSymbolicLink(pathToSrc) ? Files.readSymbolicLink(pathToSrc) : pathToSrc;
                    final Class<? extends ClassFileLoader> loaderType = actualPathToSrc.toFile().isFile() ? JarLoader.class : DirectoryLoader.class;

                    if (classFileLoaders.get(loaderType) != null) {
                        tasks.add(new InstrumentationTask(classFileLoaders.get(loaderType), actualPathToSrc, config, entry.getKey()));
                    } else {
                        throw new InstrumentationException("Loader not provided: " + loaderType.getName());
                    }

                } catch (Exception e) {
                    throw new InstrumentationException("Failed to configure loader for source: " + source, e);
                }
            }
        }

        if (config.getJdkPath() != null) {
            tasks.add(new InstrumentationTask(new JDKModuleLoader(), Paths.get(config.getJdkPath()), config, INSTRUMENTED_JDK_RELATIVE_PATH));
        }

        // process all created tasks
        for (InstrumentationTask task : tasks) {
            final InstrumentationOutcome outcome = task.applyInstrumentation();
            allOutcomes.add(outcome);

            if (outcome.getSourceInfo() != null && outcome.getSourceInfo().isJarSigned()) {
                signedJarsDiscovered.add(outcome);
            }

            if (outcome.getStatus().equals(Status.NO_OP)) {
                continue;
            }

            if (outcome.hasFailed()) {
                sourcesFailedToBeInstrumented.add(outcome);
            } else {
                sourcesInstrumented.add(outcome);
                if (outcome.getSourceInfo().isJarSigned()) {
                    signedJarsInstrumented.add(outcome);
                }
            }
        }
    }

    /**
     * Log preprocessor's summary
     */
    protected void logInstrumentationSummary() {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Preprocessor summary:");
        log.info(PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED + allOutcomes.size());
        log.info(PreprocessConstants.MESSAGE_PREFIX +  PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED + sourcesInstrumented.size());

        for (InstrumentationOutcome outcome : sourcesInstrumented) {
            log.debug("\t+ " + PreprocessConstants.MESSAGE_PREFIX + outcome.getSource());
        }

        log.info(PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED + signedJarsDiscovered.size());

        for (InstrumentationOutcome outcome : signedJarsDiscovered) {
            log.debug("\t+ " + PreprocessConstants.MESSAGE_PREFIX + outcome.getSource());
        }

        log.info(PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED + signedJarsInstrumented.size());

        for (InstrumentationOutcome outcome : signedJarsInstrumented) {
            log.debug("\t+ " + PreprocessConstants.MESSAGE_PREFIX + outcome.getSource());
        }

        log.info(PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES + sourcesFailedToBeInstrumented.size());

        for (InstrumentationOutcome outcome : sourcesFailedToBeInstrumented) {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + outcome.getSource());
            outcome.getFailedClasses().forEach(
                    clazz -> log.debug(PreprocessConstants.MESSAGE_PREFIX + "\t+ " + clazz)
            );
        }
    }
}
