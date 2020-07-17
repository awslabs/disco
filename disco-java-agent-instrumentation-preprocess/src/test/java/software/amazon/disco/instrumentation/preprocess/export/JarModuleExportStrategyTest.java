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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.exceptions.ModuleExportException;
import software.amazon.disco.instrumentation.preprocess.exceptions.UnableToReadJarEntryException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentedClassState;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarModuleExportStrategyTest {
    static final String PACKAGE_SUFFIX = "suffix";
    static final String TEMP_FILE_NAME = "temp.jar";
    static final String ORIGINAL_FILE_NAME = "mock.jar";
    static final String OUT_DIR = "out";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    ModuleInfo mockModuleInfo;
    Map<String, InstrumentedClassState> instrumented;
    JarFile mockJarFile;
    JarOutputStream mockJarOS;
    JarModuleExportStrategy mockStrategy;
    JarModuleExportStrategy spyStrategy;

    @Before
    public void before() throws IOException {
        instrumented = Mockito.mock(Map.class);
        mockJarFile = Mockito.mock(JarFile.class);
        mockStrategy = Mockito.mock(JarModuleExportStrategy.class);
        mockJarOS = Mockito.mock(JarOutputStream.class);

        spyStrategy = Mockito.spy(new JarModuleExportStrategy());
        mockModuleInfo = MockEntities.makeMockPackageInfo();
        Mockito.doCallRealMethod().when(mockStrategy).export(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(mockStrategy.createTempFile(Mockito.any())).thenReturn(tempFolder.newFile(TEMP_FILE_NAME));
    }

    @Test
    public void testExportWorksAndInvokesCreateTempFile() {
        mockStrategy.export(mockModuleInfo, instrumented, null);
        Mockito.verify(mockStrategy).createTempFile(mockModuleInfo);
    }

    @Test
    public void testExportWorksAndInvokesBuildOutputJar() {
        mockStrategy.export(mockModuleInfo, instrumented, null);
        Mockito.verify(mockStrategy).buildOutputJar(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testExportWorksAndInvokesMoveTempFileToDestination() {
        mockStrategy.export(mockModuleInfo, instrumented, null);
        Mockito.verify(mockStrategy).moveTempFileToDestination(Mockito.eq(mockModuleInfo), Mockito.any(), Mockito.any());
    }

    @Test
    public void testSaveTransformedClassesWorksAndCreatesNewEntries() throws IOException {
        spyStrategy.saveTransformedClasses(mockJarOS, MockEntities.makeInstrumentedClassesMap());

        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS, Mockito.times(3)).putNextEntry(jarEntryArgument.capture());
        Assert.assertTrue(jarEntryArgument.getAllValues().size() == 3);

        ArgumentCaptor<byte[]> byteArrayArgument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockJarOS, Mockito.times(3)).write(byteArrayArgument.capture());
        Assert.assertTrue(byteArrayArgument.getAllValues().size() == 3);

        Mockito.verify(mockJarOS, Mockito.times(3)).closeEntry();
    }

    @Test
    public void testCopyExistingJarEntriesWorksWithFilesAndPath() throws IOException {
        Mockito.doCallRealMethod().when(mockStrategy).copyExistingJarEntries(Mockito.eq(mockJarOS), Mockito.any(), Mockito.any());

        mockStrategy.copyExistingJarEntries(mockJarOS, mockModuleInfo, MockEntities.makeInstrumentedClassesMap());

        // 3 out of 6 classes have not been instrumented
        Mockito.verify(mockStrategy, Mockito.times(3)).copyJarEntry(Mockito.eq(mockJarOS), Mockito.any(), Mockito.any());

        // 1 path to be copied
        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS).putNextEntry(jarEntryArgument.capture());
        Assert.assertTrue(jarEntryArgument.getValue().getName().equals("pathA/"));
    }

    @Test(expected = UnableToReadJarEntryException.class)
    public void testCopyJarEntryFailsWithNullEntry() throws IOException {
        Mockito.doReturn(null).when(mockJarFile).getInputStream(Mockito.any());

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, new JarEntry("fff"));
    }

    @Test
    public void testCopyJarEntryWorksAndWritesToOS() throws IOException {
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(null)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, null);

        Mockito.verify(mockJarOS).putNextEntry(null);
        Mockito.verify(mockJarOS).write(Mockito.any(), Mockito.eq(0), Mockito.eq(1));
        Mockito.verify(mockJarOS).closeEntry();
    }

    @Test
    public void testMoveTempFileToDestinationToReplaceOriginal() throws IOException {
        // create original file and assume temp/disco is where the original package is
        File originalFile = createOriginalFile();
        Assert.assertEquals(1, originalFile.length());

        long originalLength = originalFile.length();

        // create temp file named temp.jar
        Mockito.when(mockModuleInfo.getFile()).thenReturn(originalFile);
        File file = spyStrategy.createTempFile(mockModuleInfo);

        // replace original file
        Path path = spyStrategy.moveTempFileToDestination(mockModuleInfo, null, file);

        Assert.assertNotEquals(originalLength, path.toFile().length());
        Assert.assertEquals(originalFile.getAbsolutePath(), path.toFile().getAbsolutePath());
        Assert.assertEquals(0, path.toFile().length());
        Assert.assertEquals(0, originalFile.length());
    }

    @Test
    public void testMoveTempFileToDestinationWorks() throws IOException {
        File outDir = tempFolder.newFolder(OUT_DIR);
        spyStrategy = new JarModuleExportStrategy(outDir.getAbsolutePath());

        // create original file and assume temp/disco/tests is where the original package is
        File originalFile = createOriginalFile();
        Assert.assertEquals(1, originalFile.length());

        // create temp file named temp.jar
        Mockito.when(mockModuleInfo.getFile()).thenReturn(originalFile);
        File file = spyStrategy.createTempFile(mockModuleInfo);

        // move to destination
        Path path = spyStrategy.moveTempFileToDestination(mockModuleInfo, null, file);

        Assert.assertEquals(outDir.getAbsolutePath(), path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(mockModuleInfo.getFile().getName(), path.toFile().getName());
        Assert.assertEquals(ORIGINAL_FILE_NAME, path.toFile().getName());
        Assert.assertNotEquals(originalFile.length(), path.toFile().length());
        Assert.assertTrue(originalFile.exists());
    }

    @Test
    public void testMoveTempFileToDestinationWorksWithSuffix() throws IOException {
        File outputDir = tempFolder.newFolder(OUT_DIR);
        spyStrategy = new JarModuleExportStrategy(outputDir.getAbsolutePath());

        // create original file and assume temp/disco/tests is where the original package is
        File originalFile = createOriginalFile();

        File tempFile = spyStrategy.createTempFile(mockModuleInfo);

        // move to destination
        Mockito.when(mockModuleInfo.getFile()).thenReturn(originalFile);
        Path path = spyStrategy.moveTempFileToDestination(mockModuleInfo, PACKAGE_SUFFIX, tempFile);

        String nameToCheck = mockModuleInfo.getFile()
                .getName()
                .substring(0, mockModuleInfo.getFile().getName().lastIndexOf(PreprocessConstants.JAR_EXTENSION))
                + PACKAGE_SUFFIX
                + PreprocessConstants.JAR_EXTENSION;

        Assert.assertEquals(outputDir.getAbsolutePath(), path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(nameToCheck, path.toFile().getName());
        Assert.assertTrue(originalFile.exists());
    }

    private File createOriginalFile() throws IOException {
        File tempFile = tempFolder.newFile(ORIGINAL_FILE_NAME);

        tempFile.getParentFile().mkdirs();
        FileOutputStream os = new FileOutputStream(tempFile);
        os.write(12);
        os.close();

        return tempFile;
    }
}
