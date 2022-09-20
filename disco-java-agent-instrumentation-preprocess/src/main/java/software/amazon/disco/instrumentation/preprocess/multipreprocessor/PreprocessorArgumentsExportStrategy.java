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

package software.amazon.disco.instrumentation.preprocess.multipreprocessor;

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Strategy used to export sub-preprocessors command-line arguments as txt files to a directory.
 */
public class PreprocessorArgumentsExportStrategy {
    /**
     * Export sub-preprocessors command-line arguments as txt files to a directory.
     *
     * @param preprocessorRawCommandlineArgsList a list of sub-preprocessors command-line arguments
     * @param config                             {@link PreprocessConfig} contains output directory for preprocessing
     * @param relativeOutputPath                 relative output path where the sub-preprocessors command-line arguments files will be stored
     * @return a list of paths to stored command-line arguments files
     */
    public List<String> exportArguments(final List<String[]> preprocessorRawCommandlineArgsList, final PreprocessConfig config, final String relativeOutputPath) {
        List<String> argsFilePaths = new ArrayList<>();
        final File preprocessorArgsTempFolder = Paths.get(config.getOutputDir(), relativeOutputPath).toFile();
        setUp(preprocessorArgsTempFolder);

        for (int i = 0; i < preprocessorRawCommandlineArgsList.size(); i++) {
            final String fileName = "sub-preprocessor-" + i + "-" + new Date().getTime() + ".txt";
            //save txt file containing sub-preprocessor command-line arguments to tmp folder
            String preprocessorArgsFilePath = saveArgsFileToDisk(preprocessorRawCommandlineArgsList.get(i), preprocessorArgsTempFolder.getAbsolutePath(), fileName);
            argsFilePaths.add("@" + preprocessorArgsFilePath);
        }
        return argsFilePaths;
    }

    /**
     * Set up work for exporting sub-preprocessors commandline arguments as txt files.
     * Clear the folder for storing arguments files if the folder already exists
     * Create a new folder for storing arguments files if the folder has not existed before
     */
    protected void setUp(File preprocessorArgsTempFolder) {
        //if the folder to store the arguments files are already there, clear the folder
        if (preprocessorArgsTempFolder.exists()) {
            for (File subFile : preprocessorArgsTempFolder.listFiles()) {
                subFile.delete();
            }
        } else {
            preprocessorArgsTempFolder.mkdirs();
        }
    }

    /**
     * Writes the sub-preprocessor's command-line arguments to its designated file.
     *
     * @param commandlineArguments command-line arguments for a sub-preprocessor
     * @param outputPath           path to the output directory where the command-line arguments files will be stored
     * @param relativePath         relative path of the arguments file to the output directory.
     */
    protected String saveArgsFileToDisk(String[] commandlineArguments, final String outputPath, final String relativePath) {
        final File destinationFile = Paths.get(outputPath, relativePath).toFile();
        try {
            destinationFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));
            writer.write(String.join(" ", commandlineArguments));
            writer.close();
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            throw new ExportException("Failed to write command-line arguments to: " + destinationFile.getName(), e);
        }
    }
}
