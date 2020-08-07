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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.AgentLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.exceptions.ModuleLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.exceptions.UnableToInstrumentException;
import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.AgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Map;

/**
 * Class responsible to orchestrate the instrumentation process involving agent loading, package loading, instrumentation
 * triggering and exporting the transformed classes.
 * <p>
 * At least one valid {@link AgentLoader} AND one {@link ModuleLoader} must be provided (either a service package loader
 * or a dependency package loader).
 */
@Builder
@AllArgsConstructor
public class ModuleTransformer {
    private static final Logger log = LogManager.getLogger(ModuleTransformer.class);

    private final ModuleLoader jarLoader;
    private final AgentLoader agentLoader;
    private final PreprocessConfig config;

    /**
     * This method initiates the transformation process of all packages found under the provided paths. All Runtime exceptions
     * thrown by the library are handled in this method. A detailed error message along with any available Cause will be logged
     * and trigger the program to exit with status 1
     */
    public void transform() {
        if (config == null || config.getLogLevel() == null) {
            Configurator.setRootLevel(Level.INFO);
        } else {
            Configurator.setRootLevel(config.getLogLevel());
        }

        try {
            if (config == null) {
                throw new InvalidConfigEntryException("No configuration provided", null);
            }
            if (agentLoader == null) {
                throw new AgentLoaderNotProvidedException();
            }
            if (jarLoader == null) {
                throw new ModuleLoaderNotProvidedException();
            }

            agentLoader.loadAgent(config, new TransformerExtractor());

            // Apply instrumentation on all loaded jars
            for (final ModuleInfo info : jarLoader.loadPackages(config)) {
                applyInstrumentation(info);
            }

        } catch (RuntimeException e) {
            log.error(e);
            System.exit(1);
        }
    }

    /**
     * Triggers instrumentation of classes using Reflection and applies and saves the changes according to the provided
     * {@link ModuleExportStrategy export strategy} on a local file
     *
     * @param moduleInfo meta-data of a loaded jar file
     */
    protected void applyInstrumentation(ModuleInfo moduleInfo) {
        log.info("applying transformation on: " + moduleInfo.getFile().getAbsolutePath());
        for (Map.Entry<String, byte[]> entry : moduleInfo.getClassByteCodeMap().entrySet()) {
            try {
                for (ClassFileTransformer transformer : TransformerExtractor.getTransformers()) {
                    transformer.transform(getClass().getClassLoader(), entry.getKey(), null, null, entry.getValue());
                }
            } catch (IllegalClassFormatException e) {
                throw new UnableToInstrumentException("Failed to instrument : " + entry.getKey(), e);
            }
        }

        log.debug(PreprocessConstants.MESSAGE_PREFIX + getInstrumentedClasses().size() + " classes transformed");
        moduleInfo.getExportStrategy().export(moduleInfo, getInstrumentedClasses(), config);

        // empty the map in preparation for transforming another package
        getInstrumentedClasses().clear();
    }

    /**
     * Fetches instrumented classes from the listener attached to all {@link software.amazon.disco.agent.interception.Installable installables}.
     *
     * @return a Map of class name as key and {@link InstrumentedClassState} as value
     */
    protected Map<String, InstrumentedClassState> getInstrumentedClasses() {
        return TransformationListener.getInstrumentedTypes();
    }
}
