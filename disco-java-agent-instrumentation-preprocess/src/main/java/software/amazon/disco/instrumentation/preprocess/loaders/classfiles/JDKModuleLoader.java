/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.export.JDKExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An implementation of {@link JarLoader} which is responsible for loading classes from the JDK base module. This loader will explore sub-directories of the provided
 * 'java.home' path to retrieve the JDK base module file depending on the JDK version.
 *
 * For JDK 8 and lower, the file would be [path_to_java_home]/lib/rt.jar, whereas [path_to_java_home]/jmods/java.base.jmod is the file for JDK9+.
 */
public class JDKModuleLoader extends JarLoader {
    private static final Logger log = LogManager.getLogger(JDKModuleLoader.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceInfo load(final Path path, final PreprocessConfig config) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "JDK support option enabled. Attempting to instrument provided JDK runtime");

        if (config.getOutputDir() == null) {
            throw new InstrumentationException("No output dir provided to store instrumented JDK runtime, unable to perform JDK instrumentation", null);
        }

        final File jdkModuleFile = getJDKBaseModule(path.toString());

        // check JVM compatibility
        if (isJDK9Compatible()) {
            // jvm 9 or higher
            if (jdkModuleFile.getAbsolutePath().endsWith(".jar")) {
                throw new InstrumentationException("Unable to instrument jdk 8 or lower using a JVM that uses the module system.", null);
            }
        } else {
            // jvm 8 or lower
            if (jdkModuleFile.getAbsolutePath().endsWith(".jmod")) {
                throw new InstrumentationException("Unable to instrument jdk 9 or higher using a JVM that doesn't support the module system.", null);
            }
        }

        // the signed Jar handling strategy configured for handling non-JDK library Jars is independent/irrelevant to how the JDK should be loaded and later instrumented,
        // and thus the default strategy will be explicitly set here.
        final SourceInfo info = loadJar(jdkModuleFile, new JDKExportStrategy(), new InstrumentSignedJarHandlingStrategy());

        return info;
    }

    /**
     * Retrieves the Java base module file from the java home directory.
     *
     * @param javaHome path to the Java home.
     * @return file object of the Java base module
     */
    public File getJDKBaseModule(final String javaHome) {
        final File javaHomeFile =  new File(javaHome);
        if (javaHomeFile.exists()) {
            final File jdk9Plus = Paths.get(javaHomeFile.getAbsolutePath(), "jmods", "java.base.jmod").toFile();
            final File jdk8Lower = Paths.get(javaHomeFile.getAbsolutePath(), "lib", "rt.jar").toFile();

            if (jdk9Plus.exists()) {
                log.debug(PreprocessConstants.MESSAGE_PREFIX + "Java base module for JDK9+ found.");
                return jdk9Plus;
            } else if (jdk8Lower.exists()) {
                log.debug(PreprocessConstants.MESSAGE_PREFIX + "Java base module for JDK8 and lower found.");
                return jdk8Lower;
            }
        }

        throw new InstrumentationException("Unable to retrieve JDK base module from provided path: " + javaHome, null);
    }

    /**
     * Checks whether the JVM is JDK9 compatible by reading the 'java.version' system property. Versions that start with '1.x'
     * have not yet implemented the module system and therefore not JDK9 compatible.
     *
     * @return true if compatible, false otherwise
     */
    protected boolean isJDK9Compatible() {
        return !System.getProperty("java.version").startsWith("1.");
    }
}
