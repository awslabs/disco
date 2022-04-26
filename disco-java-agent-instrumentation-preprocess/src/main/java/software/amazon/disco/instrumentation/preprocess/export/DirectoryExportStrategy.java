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
import java.util.Map;

/**
 * Strategy to export transformed class files to a directory.
 */
public class DirectoryExportStrategy extends ExportStrategy {
    private static final Logger log = LogManager.getLogger(DirectoryExportStrategy.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void export(final SourceInfo info, final Map<String, InstrumentationArtifact> artifacts, final PreprocessConfig config, final String relativePath) {
        if (artifacts == null || artifacts.isEmpty()) {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Nothing to export, skipping");
        } else {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Exporting " + artifacts.size() + " artifacts from source: " + info.getSourceFile().getPath());

            for (Map.Entry<String, InstrumentationArtifact> entry : artifacts.entrySet()) {
                saveArtifactToDisk(entry, config, relativePath);
            }
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Exporting completed");
        }
    }

    /**
     * Writes the bytecode of a transformed class to its designated file.
     *
     * @param entry        entry representing a transformed class to be saved
     * @param config       configuration file containing instructions to instrument a source
     * @param relativePath relative path to the root parent directory.
     */
    protected void saveArtifactToDisk(final Map.Entry<String, InstrumentationArtifact> entry, final PreprocessConfig config, final String relativePath) {
        final File destinationFile = createOutputFile(config.getOutputDir(), relativePath, entry.getKey() + ".class");

        log.trace(PreprocessConstants.MESSAGE_PREFIX + "Exporting class: " + destinationFile.getName());

        try (FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
            fileOutputStream.write(entry.getValue().getClassBytes());
        } catch (Throwable t) {
            throw new ExportException("Failed to export class: " + destinationFile.getName(), t);
        }
    }
}
