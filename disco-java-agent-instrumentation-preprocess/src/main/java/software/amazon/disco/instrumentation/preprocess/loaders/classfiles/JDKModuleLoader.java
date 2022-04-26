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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.export.JDKExportStrategy;
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

        final File jdkModuleFile = getJDKBaseModule(config);

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

        final SourceInfo info = loadJar(jdkModuleFile, new JDKExportStrategy());

        return info;
    }

    /**
     * Retrieves the Java base module file from the java home directory.
     *
     * @param config a PreprocessConfig containing information to perform Build-Time Instrumentation
     * @return file object of the Java base module
     */
    protected File getJDKBaseModule(final PreprocessConfig config) {
        final File javaHome = new File(config.getJdkPath());

        if (javaHome.exists()) {
            final File jdk9Plus = Paths.get(javaHome.getAbsolutePath(), "jmods", "java.base.jmod").toFile();
            final File jdk8Lower = Paths.get(javaHome.getAbsolutePath(), "lib", "rt.jar").toFile();

            if (jdk9Plus.exists()) {
                log.info(PreprocessConstants.MESSAGE_PREFIX + "Java base module for JDK9+ found.");
                return jdk9Plus;
            } else if (jdk8Lower.exists()) {
                log.info(PreprocessConstants.MESSAGE_PREFIX + "Java base module for JDK8 and lower found.");
                return jdk8Lower;
            }
        }

        throw new InstrumentationException("Enable to retrieve JDK base module from provided path: " + config.getJdkPath(), null);
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
