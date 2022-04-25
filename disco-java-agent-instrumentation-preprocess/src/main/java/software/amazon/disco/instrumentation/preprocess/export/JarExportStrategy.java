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
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Strategy to export transformed classes to a local Jar
 */
public class JarExportStrategy extends ExportStrategy {
    private static final Logger log = LogManager.getLogger(JarExportStrategy.class);

    /**
     * Exports all transformed classes to a Jar file. A temporary Jar File will be created to store all
     * the transformed classes and then be renamed to replace the original Jar.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void export(final SourceInfo sourceInfo, final Map<String, InstrumentationArtifact> instrumented, final PreprocessConfig config, final String relativeOutputPath) {
        final String jarName = sourceInfo.getSourceFile().getName();

        if (!instrumented.isEmpty()) {
            log.info(String.format(PreprocessConstants.MESSAGE_PREFIX + "Saving instrumented classes to %s.", jarName));

            final File file = createOutputFile(config.getOutputDir(), relativeOutputPath, sourceInfo.getSourceFile().getName());

            try (JarOutputStream jarOS = new JarOutputStream(new FileOutputStream(file)); JarFile jarFile = new JarFile(sourceInfo.getSourceFile())) {
                copyExistingJarEntries(jarOS, jarFile, instrumented);
                saveInstrumentationArtifactsToJar(jarOS, instrumented);
            } catch (IOException e) {
                throw new ExportException("Failed to create output Jar file", e);
            }

            log.debug(String.format(PreprocessConstants.MESSAGE_PREFIX + "Exporting completed for %s", jarName));
        } else {
            log.debug(String.format(PreprocessConstants.MESSAGE_PREFIX + "No classes instrumented for %s, skipping to next Jar", jarName));
        }
    }

    /**
     * Copies existing entries from the original Jar to the temporary Jar while skipping any
     * transformed classes.
     *
     * @param jarOS     {@link JarOutputStream} used to write entries to the output jar
     * @param jarFile   the original JarFile
     * @param artifacts a map of instrumentation artifacts with their bytecode
     */
    protected void copyExistingJarEntries(final JarOutputStream jarOS, final JarFile jarFile, final Map<String, InstrumentationArtifact> artifacts) {
        log.info(PreprocessConstants.MESSAGE_PREFIX + "Copying existing entries from file: " + jarFile.getName());
        final Enumeration entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = (JarEntry) entries.nextElement();
            final String keyToCheck = entry.getName().endsWith(".class") ? entry.getName().substring(0, entry.getName().lastIndexOf(".class")) : entry.getName();

            try {
                if (!artifacts.containsKey(keyToCheck)) {
                    if (entry.isDirectory()) {
                        jarOS.putNextEntry(entry);
                    } else {
                        copyJarEntry(jarOS, jarFile, entry);
                    }
                }
            } catch (IOException e) {
                throw new ExportException("Failed to copy class: " + entry.getName(), e);
            }
        }
    }
}
