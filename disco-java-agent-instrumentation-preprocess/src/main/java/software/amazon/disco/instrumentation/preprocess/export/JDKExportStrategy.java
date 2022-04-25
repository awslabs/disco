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

package software.amazon.disco.instrumentation.preprocess.export;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.agent.concurrent.preprocess.DiscoRunnableDecorator;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Strategy used to export instrumented JDK classes. This differs slightly from {@link JarExportStrategy} in the sense that
 * <p>
 * 1) Only the transformed classes need to be packaged into the output jar since --patch-module and -Xbootclasspath/p only requires files to be patched to be included in the provided Jar.
 * 2) A class named {@link DiscoRunnableDecorator} will be inserted into the output Jar to allow the JVM to bootstrap normally. For more info, please see {@link DiscoRunnableDecorator}.
 * 3) If no output dir is specified, the output file will be created under the current directory where the library is being executed. This is to prevent pollution to the java lib directory by default.
 * <p>
 * For instrumenting JDK 9+ using --patch-module, since classes appended to java.base are module-private by default and cannot be accessed by other modules and
 * the newly introduced requirement for JDK9+ where named modules have restricted access to unnamed modules, the following JVM args must be provided when
 * starting up the service application.
 * <p>
 * example for launching a JDK9+ Java application:
     java --patch-module java.base='path_to_InstrumentedJDK.jar
          --add-reads java.base=ALL-UNNAMED
          --add-exports=java.base/software.amazon.disco.agent.concurrent.preprocess=ALL-UNNAMED
          Driver
 */
public class JDKExportStrategy extends ExportStrategy {
    public static final String INSTRUMENTED_JDK_OUTPUT_NAME = "InstrumentedJDK.jar";
    public static final String PACKAGE_TO_INSERT = "software/amazon/disco/agent/concurrent/preprocess";

    private static final Logger log = LogManager.getLogger(JDKExportStrategy.class);

    /**
     * Exports all transformed JDK classes to a Jar file named instrumentedJDKRT.jar. A temporary Jar File will be created to store all
     * the transformed classes and then be renamed to replace the original Jar.
     *
     * {@inheritDoc}
     */
    @Override
    public void export(SourceInfo info, Map<String, InstrumentationArtifact> artifacts, PreprocessConfig config, final String relativeOutputPath) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Attempting to export artifacts JDK classes");

        try {
            final File tempFile = createOutputFile(config.getOutputDir(), relativeOutputPath, INSTRUMENTED_JDK_OUTPUT_NAME);

            try (JarOutputStream os = new JarOutputStream(new FileOutputStream(tempFile))) {
                saveInstrumentationArtifactsToJar(os, artifacts);

                // copy required dependencies such as concurrency support from the agent jar so classes within can be resolved when instantiating the JVM at service runtime
                final JarFile agentJar = new JarFile(new File(config.getAgentPath()));
                for (Map.Entry<String, JarEntry> entry : extractBootstrapDependenciesFromAgent(agentJar).entrySet()) {
                    copyJarEntry(os, agentJar, entry.getValue());
                }
            }

            log.info(PreprocessConstants.MESSAGE_PREFIX + "Instrumented JDK moved to destination");
        } catch (IOException e) {
            throw new ExportException("Failed to create temp Jar file", e);
        } finally {
            artifacts.clear();
        }
    }

    /**
     * Extracts dependencies, from the agent used to perform the static instrumentation, that must be resolved
     * at JVM bootstrap phase since classes such as Thread will need access to ThreadInterceptor which hasn't yet been
     * injected to the classpath. This is due to the fact that this early into the program execution, the disco agent hasn't
     * been installed on the jvm yet.
     *
     * @param agentJar the agent jar
     * @return a set of JarEntry
     */
    protected Map<String, JarEntry> extractBootstrapDependenciesFromAgent(final JarFile agentJar) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Extracting dependencies from agent");
        final Map<String, JarEntry> result = new HashMap<>();

        for (Enumeration entries = agentJar.entries(); entries.hasMoreElements(); ) {
            final JarEntry entry = (JarEntry) entries.nextElement();

            if (entry.getName().startsWith(PACKAGE_TO_INSERT)) {
                result.put(entry.getName(), entry);
            }
        }
        return result;
    }
}
