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
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.AgentLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.exceptions.ClassFileLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.AgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarInfo;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.ClassFileLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;

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
    private static final Logger log = LogManager.getLogger(StaticInstrumentationTransformer.class);

    private final ClassFileLoader jarLoader;
    private final AgentLoader agentLoader;
    private final PreprocessConfig config;

    /**
     * This method initiates the transformation process of all packages found under the provided paths. All Runtime exceptions
     * thrown by the library are handled in this method. A detailed error message along with any available Cause will be logged
     * and trigger the program to exit with status 1
     */
    public void transform() {
        if (config == null) {
            throw new InvalidConfigEntryException("No configuration provided", null);
        }
        // setting the level again here in case the lib is imported as a dependency and the arg parser is never used
        Configurator.setRootLevel(config.getLogLevel());

        if (agentLoader == null) {
            throw new AgentLoaderNotProvidedException();
        }
        if (jarLoader == null) {
            throw new ClassFileLoaderNotProvidedException();
        }

        agentLoader.loadAgent(config, new TransformerExtractor(Injector.createInstrumentation()));

        log.info(PreprocessConstants.MESSAGE_PREFIX + TransformerExtractor.getTransformers().size() + " Installables loaded.");

        // Apply instrumentation on all loaded jars
        for (final JarInfo info : jarLoader.load(config)) {
            applyInstrumentation(info);
        }
    }

    /**
     * Triggers instrumentation of classes by invoking {@link ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])} of
     * all ClassFileTransformers extracted via a {@link TransformerExtractor} and saves the transformed byte code according to the provided {@link ExportStrategy export strategy}
     * on a local file.
     *
     * @param jarInfo information of a loaded jar file such as all the class files it contains.
     */
    protected void applyInstrumentation(JarInfo jarInfo) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Applying transformation on: " + jarInfo.getFile().getAbsolutePath());
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Classes found: " + jarInfo.getClassByteCodeMap().size());

        for (Map.Entry<String, byte[]> entry : jarInfo.getClassByteCodeMap().entrySet()) {
            try {
                for (ClassFileTransformer transformer : TransformerExtractor.getTransformers()) {
                    final String internalName = entry.getKey().replace('.','/');
                    final byte[] bytecodeToTransform = getInstrumentedClasses().containsKey(internalName) ?
                            getInstrumentedClasses().get(internalName).getClassBytes() : entry.getValue();

                    transformer.transform(ClassLoader.getSystemClassLoader(), entry.getKey(), null, null, bytecodeToTransform);
                }
            } catch (IllegalClassFormatException e) {
                throw new InstrumentationException("Failed to instrument : " + entry.getKey(), e);
            }
        }

        log.debug(PreprocessConstants.MESSAGE_PREFIX + getInstrumentedClasses().size() + " classes transformed");
        jarInfo.getExportStrategy().export(jarInfo, getInstrumentedClasses(), config);

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
