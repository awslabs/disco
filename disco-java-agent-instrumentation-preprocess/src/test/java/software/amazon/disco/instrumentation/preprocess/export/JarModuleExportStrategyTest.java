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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.class)
public class JarModuleExportStrategyTest {
    static final String ORIGINAL_FILE_PATH = System.getProperty("java.io.tmpdir") + "disco/tests/" + "original.jar";
    static final String DESTINATION_PATH = System.getProperty("java.io.tmpdir") + "disco/tests/destination";
    static final String PACKAGE_SUFFIX = "suffix";

    ModuleInfo moduleInfo;

    @Mock
    Map<String, InstrumentedClassState> instrumented;

    @Mock
    JarFile mockJarFile;

    @Mock
    File mockFile;

    @Mock
    JarModuleExportStrategy mockStrategy;

    @Mock
    JarOutputStream mockJarOS;

    JarModuleExportStrategy strategy;

    @Before
    public void before() {
        strategy = new JarModuleExportStrategy();
        moduleInfo = MockEntities.makeMockPackageInfo();
        Mockito.doCallRealMethod().when(mockStrategy).export(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testExportWorksAndInvokesCreateTempFile() {
        mockStrategy.export(moduleInfo, instrumented, null, null);
        Mockito.verify(mockStrategy).createTempFile(moduleInfo);
    }

    @Test
    public void testExportWorksAndInvokesBuildOutputJar() {
        mockStrategy.export(moduleInfo, instrumented, null, null);
        Mockito.verify(mockStrategy).buildOutputJar(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testExportWorksAndInvokesMoveTempFileToDestination() {
        mockStrategy.export(moduleInfo, instrumented, null, null);
        Mockito.verify(mockStrategy).moveTempFileToDestination(Mockito.eq(moduleInfo), Mockito.any(), Mockito.any());
    }

    @Test
    public void testSaveTransformedClassesWorksAndCreatesNewEntries() throws IOException {
        strategy.saveTransformedClasses(mockJarOS, MockEntities.makeInstrumentedClassesMap());

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

        mockStrategy.copyExistingJarEntries(mockJarOS, moduleInfo, MockEntities.makeInstrumentedClassesMap());

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

        strategy.copyJarEntry(mockJarOS, mockJarFile, new JarEntry("fff"));
    }

    @Test
    public void testCopyJarEntryWorksAndWritesToOS() throws IOException {
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(null)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);

        strategy.copyJarEntry(mockJarOS, mockJarFile, null);

        Mockito.verify(mockJarOS).putNextEntry(null);
        Mockito.verify(mockJarOS).write(Mockito.any(), Mockito.eq(0), Mockito.eq(1));
        Mockito.verify(mockJarOS).closeEntry();
    }

    @Test
    public void testCreateTempFileWorks() {
        File file = strategy.createTempFile(moduleInfo);

        Assert.assertNotNull(file);
        file.delete();
    }

    @Test
    public void testMoveTempFileToDestinationToReplaceOriginal() throws IOException {
        // create original file and assume temp/disco is where the original package is
        File tempFile = createOriginalFile();

        long originalLength = tempFile.length();

        // create temp file named path.temp.jar
        Mockito.when(moduleInfo.getFile()).thenReturn(mockFile);
        Mockito.when(mockFile.getAbsolutePath()).thenReturn(ORIGINAL_FILE_PATH);
        File file = strategy.createTempFile(moduleInfo);

        // replace original file
        Path path = strategy.moveTempFileToDestination(moduleInfo, null, file);

        Assert.assertNotEquals(originalLength, path.toFile().length());
        Assert.assertEquals(tempFile.getAbsolutePath(), path.toFile().getAbsolutePath());

        file.delete();
        path.toFile().delete();
    }

    @Test
    public void testMoveTempFileToDestinationWorks() throws IOException {
        String destination = DESTINATION_PATH;
        strategy = new JarModuleExportStrategy(destination);

        // create original file and assume temp/disco/tests is where the original package is
        File original = createOriginalFile();

        File file = strategy.createTempFile(moduleInfo);

        // move to destination
        Path path = strategy.moveTempFileToDestination(moduleInfo, null, file);

        Assert.assertEquals(destination, path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(moduleInfo.getFile().getName(), path.toFile().getName());
        Assert.assertTrue(original.exists());

        file.delete();
        path.toFile().delete();
        original.delete();
    }

    @Test
    public void testMoveTempFileToDestinationWorksWithSuffix() throws IOException {
        String destination = DESTINATION_PATH;
        strategy = new JarModuleExportStrategy(destination);

        // create original file and assume temp/disco/tests is where the original package is
        File original = createOriginalFile();

        File file = strategy.createTempFile(moduleInfo);

        // move to destination
        Path path = strategy.moveTempFileToDestination(moduleInfo, PACKAGE_SUFFIX, file);

        String nameToCheck = moduleInfo.getFile()
                .getName()
                .substring(0, moduleInfo.getFile().getName().lastIndexOf(PreprocessConstants.JAR_EXTENSION))
                + PACKAGE_SUFFIX
                + PreprocessConstants.JAR_EXTENSION;

        Assert.assertEquals(destination, path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(nameToCheck, path.toFile().getName());
        Assert.assertTrue(original.exists());

        file.delete();
        path.toFile().delete();
        original.delete();
    }

    @Test(expected = ModuleExportException.class)
    public void testMoveTempFileToDestinationFailsWhenFileAlreadyExistsWithSameName() throws IOException {
        String destination = DESTINATION_PATH;
        strategy = new JarModuleExportStrategy(destination);

        // create original file and assume temp/disco/tests is where the original package is
        File original = createOriginalFile();

        File file = strategy.createTempFile(moduleInfo);

        // move to destination
        Path path = strategy.moveTempFileToDestination(moduleInfo, PACKAGE_SUFFIX, file);

        String nameToCheck = moduleInfo.getFile()
                .getName()
                .substring(0, moduleInfo.getFile().getName().lastIndexOf(PreprocessConstants.JAR_EXTENSION))
                + PACKAGE_SUFFIX
                + PreprocessConstants.JAR_EXTENSION;

        Assert.assertEquals(destination, path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(nameToCheck, path.toFile().getName());
        Assert.assertTrue(original.exists());

        // mock another package of the same name being moved to the same dir as the first one
        try {
            strategy.moveTempFileToDestination(moduleInfo, PACKAGE_SUFFIX, file);
        } finally {
            file.delete();
            path.toFile().delete();
            original.delete();
        }
    }

    private File createOriginalFile() throws IOException {
        File tempFile = new File(ORIGINAL_FILE_PATH);

        tempFile.getParentFile().mkdirs();
        FileOutputStream os = new FileOutputStream(tempFile);
        os.write(12);
        os.close();

        return tempFile;
    }
}
