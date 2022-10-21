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

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;
import software.amazon.disco.instrumentation.preprocess.exceptions.JarEntryCopyException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.util.JarFileUtils;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Abstract base class for the strategy to use when exporting transformed classes
 */
public abstract class ExportStrategy {
    private static final Logger log = LogManager.getLogger(ExportStrategy.class);

    /**
     * Strategy called to export all transformed classes.
     *
     * @param info               Information of the original source file
     * @param artifacts          a map of instrumentation artifacts with their bytecode
     * @param config             configuration file containing instructions to instrument a module
     * @param relativeOutputPath relative output path where the preprocessing artifact will be stored.
     */
    public abstract void export(final SourceInfo info, final Map<String, InstrumentationArtifact> artifacts, final PreprocessConfig config, final String relativeOutputPath);

    /**
     * Creates the output file at the specified location. The resulting artifact will be saved under the root output directory extracted from the config file. The relative path
     * of the parent directory' contents should be identical to their relative path on the deployed host.
     * <p>
     * For instance, 'aws-java-sdk-core' should be saved to 'outputDir/lib/aws-java-sdk-core-1.x.jar' during preprocessing so that its relative path, 'lib/aws-java-sdk-core-1.x.jar'
     * is identical to its corresponding relative path on the deployed host.
     * <p>
     * In the case of individual class files under a parent directory, for instance 'tomcat/software/amazon/toplevelpackage/ClassA.class', the relativeParentPath param would be 'tomcat',
     * whereas 'software/amazon/toplevelpackage/ClassA.class' would be the filePath.
     *
     * @param rootOutputDir      root directory where all the preprocessing artifacts will be stored
     * @param relativeParentPath relative path of the parent directory in relation to the root output directory
     * @param filePath           relative path to the source file in relation to the source's parent directory
     * @return a created file at the specified location
     */
    protected File createOutputFile(final String rootOutputDir, final String relativeParentPath, final String filePath) {
        try {
            final File outputFile = Paths.get(rootOutputDir, relativeParentPath, filePath).toFile();
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();

            return outputFile;
        } catch (IOException e) {
            throw new ExportException("Failed to create output file for: " + String.join("/", filePath), e);
        }
    }

    /**
     * Copies a single {@link JarEntry entry} from the original Jar to the Temp Jar
     *
     * @param jarOS {@link JarOutputStream} used to write entries to the output jar
     * @param file  original {@link File jar} where the entry's binary data will be read
     * @param entry a single {@link JarEntry Jar entry}
     * @throws JarEntryCopyException
     */
    protected void copyJarEntry(final JarOutputStream jarOS, final JarFile file, final JarEntry entry) {
        try {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Copying entry: " + entry.getName());

            jarOS.putNextEntry(new JarEntry(entry.getName()));
            jarOS.write(JarFileUtils.readEntryFromJar(file, entry));
            jarOS.closeEntry();

        } catch (Throwable t) {
            // Some Jars might be mal-formed where multiple entries with the exact same path and extension may co-exist in the same Jar.
            // This is a common behaviour when the 'duplicate' config from Ant tasks such as Jar or JarJar are left undefined.
            // This behaviour is usually not disruptive when the duplicated entries are text files and is allowed in the Zip File specification
            // (used by Jar and JarJar) but not allowed in the Java Jar specification (used by the Preprocessor).
            // Since certain text files don't have file format explicitly defined, e.g. LICENSE, duplicated entries under the entire 'META-INF/' dir is ignored.
            if (t instanceof IOException && entry.getName().startsWith("META-INF/")) {
                log.warn(PreprocessConstants.MESSAGE_PREFIX + "Duplicated entry ignored: " + entry.getName());
            } else {
                throw new JarEntryCopyException(entry.getName(), t);
            }
        }
    }

    /**
     * Inserts all transformed classes into the temporary Jar file
     *
     * @param jarOS     {@link JarOutputStream} used to write entries to the output jar
     * @param artifacts a map of instrumentation artifacts with their bytecode
     */
    protected void saveInstrumentationArtifactsToJar(final JarOutputStream jarOS, final Map<String, InstrumentationArtifact> artifacts) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Transformed " + artifacts.size() + " classes.");
        for (Map.Entry<String, InstrumentationArtifact> mapEntry : artifacts.entrySet()) {
            final String classPath = mapEntry.getKey();
            final InstrumentationArtifact info = mapEntry.getValue();
            final JarEntry entry = new JarEntry(classPath + ".class");

            try {
                jarOS.putNextEntry(entry);
                jarOS.write(info.getClassBytes());
                jarOS.closeEntry();
            } catch (IOException e) {
                throw new ExportException(classPath, e);
            }
        }
    }
}
