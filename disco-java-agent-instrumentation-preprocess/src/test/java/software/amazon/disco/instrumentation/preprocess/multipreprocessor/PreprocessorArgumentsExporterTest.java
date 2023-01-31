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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreprocessorArgumentsExporterTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private PreprocessConfig config;
    private PreprocessorArgumentsExporter strategy;
    private static final List<String[]> preprocessorRawCommandlineArgsList = Arrays.asList(new String[]{"sub-preprocessor-1", "arg1", "arg2"}, new String[]{"sub-preprocessor-2", "arg1", "arg2"});
    private String outputPath;
    private File outputDir;

    @Before
    public void before() {
        config = PreprocessConfig.builder().outputDir(tempFolder.getRoot().getAbsolutePath() + "/out").build();
        strategy = Mockito.spy(new PreprocessorArgumentsExporter());
        outputPath = config.getOutputDir() + "/" + PreprocessConstants.PREPROCESSOR_ARGS_TEMP_FOLDER;
        outputDir = Paths.get(outputPath).toFile();
    }

    @After
    public void cleanup() {
        if (outputDir.exists()) {
            for (File subfile : outputDir.listFiles()) {
                subfile.delete();
            }
            outputDir.delete();
        }
    }

    @Test
    public void testExportArgumentsWorksAndInvokesHelperMethods() {
        strategy.exportArguments(preprocessorRawCommandlineArgsList, config, PreprocessConstants.PREPROCESSOR_ARGS_TEMP_FOLDER);
        Mockito.verify(strategy, Mockito.times(1)).setUp(Mockito.eq(outputDir));
        Mockito.verify(strategy, Mockito.times(2)).saveArgsFileToDisk(Mockito.any(String[].class), Mockito.eq(outputPath), Mockito.anyString());
    }

    @Test
    public void testSetupWhenOutputDirectoryNotExists() {
        strategy.setUp(outputDir);
        assertTrue(outputDir.exists());
    }

    @Test
    public void testSetupWhenOutputDirectoryExists() throws IOException {
        outputDir.mkdirs();
        final File previousFile = Paths.get(outputPath, "previous.txt").toFile();
        previousFile.createNewFile();
        //output directory has files from previous run
        assertTrue(outputDir.listFiles().length > 0);

        strategy.setUp(outputDir);
        //output directory has been cleared
        assertEquals(0, outputDir.listFiles().length);
    }

    @Test(expected = ExportException.class)
    public void testSetupWhenFailsToCreateOutputDirectory() {
        File mockedOutputDir = Mockito.mock(File.class);
        Mockito.doReturn(false).when(mockedOutputDir).mkdirs();

        strategy.setUp(mockedOutputDir);
    }

    @Test
    public void testSaveArgsFileToDiskWorks() throws IOException {
        outputDir.mkdirs();
        for (int i = 0; i < preprocessorRawCommandlineArgsList.size(); i++) {
            String[] preprocessorCommandlineArgs = preprocessorRawCommandlineArgsList.get(i);
            String argsFileRelativePath = "worker-" + i + "txt";
            String argsFilePath = strategy.saveArgsFileToDisk(preprocessorCommandlineArgs, outputDir.getAbsolutePath(), argsFileRelativePath);
            String content = new String(Files.readAllBytes(new File(argsFilePath).toPath()));
            String expectedArgsFileContent = String.join(" ", preprocessorCommandlineArgs);

            assertEquals(outputDir.getAbsolutePath() + "/" + argsFileRelativePath, argsFilePath);
            assertEquals(expectedArgsFileContent, content);
        }
    }

    @Test(expected = ExportException.class)
    public void testSaveArgsFileToDiskFailsAndThrowsException() {
        strategy.saveArgsFileToDisk(preprocessorRawCommandlineArgsList.get(0), "/somePath", "worker-0.txt");
    }
}