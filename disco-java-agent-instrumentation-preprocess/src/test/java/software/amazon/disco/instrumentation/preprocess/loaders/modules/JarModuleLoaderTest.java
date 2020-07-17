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

package software.amazon.disco.instrumentation.preprocess.loaders.modules;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoModuleToInstrumentException;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoPathProvidedException;
import software.amazon.disco.instrumentation.preprocess.export.JarModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class JarModuleLoaderTest {
    static final List<String> PATHS = MockEntities.makeMockPathsWithDuplicates();
    static final List<File> MOCK_FILES = MockEntities.makeMockFiles();
    static final List<JarEntry> MOCK_JAR_ENTRIES = MockEntities.makeMockJarEntries();

    JarModuleLoader loader;

    @Mock
    JarFile jarFile;

    @Mock
    File mockFile;

    @Before
    public void before() throws NoPathProvidedException {
        loader = new JarModuleLoader(PATHS);

        Mockito.when(mockFile.isDirectory()).thenReturn(false);
        Mockito.when(mockFile.getName()).thenReturn("ATestJar.jar");
    }

    @Test(expected = NoPathProvidedException.class)
    public void testConstructorFailWithEmptyPathList() throws NoPathProvidedException {
        new JarModuleLoader(new ArrayList<>());
    }

    @Test(expected = NoPathProvidedException.class)
    public void testConstructorFailWithNullPathList() throws NoPathProvidedException {
        new JarModuleLoader(null);
    }

    @Test
    public void testConstructorWorksAndHasDefaultStrategy() {
        Assert.assertTrue(loader.getStrategy().getClass().equals(JarModuleExportStrategy.class));
    }

    @Test
    public void testConstructorWorksAndNoDuplicatePaths() {
        Assert.assertTrue(loader.getPaths().size() == PATHS.size() - 1);

        for (String path : PATHS) {
            Assert.assertTrue(loader.getPaths().contains(path));
        }
    }

    @Test
    public void testConstructorWorksWithNonDefaultStrategy() {
        ModuleExportStrategy mockStrategy = Mockito.mock(ModuleExportStrategy.class);

        loader = new JarModuleLoader(mockStrategy, PATHS);
        Assert.assertNotEquals(JarModuleExportStrategy.class, loader.getStrategy().getClass());
    }

    @Test
    public void testProcessFileWorksWithValidFileExtension(){
        JarModuleLoader loader = Mockito.mock(JarModuleLoader.class);
        JarFile jar = Mockito.mock(JarFile.class);

        Mockito.doCallRealMethod().when(loader).processFile(mockFile);
        Mockito.doReturn(jar).when(loader).makeJarFile(mockFile);

        Assert.assertNotNull(loader.processFile(mockFile));

        Mockito.when(mockFile.getName()).thenReturn("ATestJar.JAR");
        Assert.assertNotNull(loader.processFile(mockFile));
    }

    @Test
    public void testProcessFileWorksWithInvalidFileExtensionAndReturnNull(){
        JarModuleLoader loader = Mockito.mock(JarModuleLoader.class);

        Mockito.when(mockFile.getName()).thenReturn("ATestJar.txt");
        Mockito.doCallRealMethod().when(loader).processFile(mockFile);

        Assert.assertNull(loader.processFile(mockFile));
    }

    @Test
    public void testProcessFileWorksAndInvokesInjectFileToSystemClassPath() {
        JarModuleLoader packageLoader = Mockito.mock(JarModuleLoader.class);
        Mockito.when(packageLoader.processFile(Mockito.any())).thenCallRealMethod();

        JarFile mockJarfile = Mockito.mock(JarFile.class);
        Mockito.when(packageLoader.makeJarFile(mockFile)).thenReturn(mockJarfile);

        packageLoader.processFile(mockFile);

        Mockito.verify(packageLoader).injectFileToSystemClassPath(mockFile);
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadPackagesFailOnEmptyPackageInfoList() {
        loader.loadPackages();
    }

    @Test
    public void testLoadPackagesWorksWithOnePackageInfo() {
        JarModuleLoader packageLoader = Mockito.spy(new JarModuleLoader(Arrays.asList(PATHS.get(0))));

        Mockito.doCallRealMethod().when(packageLoader).loadPackages();
        Mockito.when(packageLoader.discoverFilesInPath(PATHS.get(0))).thenReturn(Arrays.asList(MOCK_FILES.get(0)));
        Mockito.doReturn(MockEntities.makeMockPackageInfo()).when(packageLoader).loadPackage(MOCK_FILES.get(0));

        packageLoader.loadPackages();

        Mockito.verify(packageLoader).loadPackage(Mockito.any());
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadPackagesFailsWithNoPackageInfoCreated() {
        JarModuleLoader packageLoader = Mockito.spy(new JarModuleLoader(Arrays.asList(PATHS.get(0))));

        Mockito.doCallRealMethod().when(packageLoader).loadPackages();
        Mockito.when(packageLoader.discoverFilesInPath(PATHS.get(0))).thenReturn(Arrays.asList(MOCK_FILES.get(0)));

        packageLoader.loadPackages();

        Mockito.verify(packageLoader).loadPackage(Mockito.any());
    }

    @Test
    public void testLoadPackagesWorksAndCalledThreeTimesWithThreePaths() {
        JarModuleLoader packageLoader = Mockito.spy(new JarModuleLoader(PATHS));

        Mockito.doCallRealMethod().when(packageLoader).loadPackages();

        Mockito.when(packageLoader.discoverFilesInPath(PATHS.get(1))).thenReturn(Arrays.asList(MOCK_FILES.get(0)));
        Mockito.when(packageLoader.discoverFilesInPath(PATHS.get(2))).thenReturn(Arrays.asList(MOCK_FILES.get(1)));
        Mockito.when(packageLoader.discoverFilesInPath(PATHS.get(3))).thenReturn(Arrays.asList(MOCK_FILES.get(2)));

        try {
            packageLoader.loadPackages();
        } catch (NoModuleToInstrumentException e) {
            // swallow
        }

        Mockito.verify(packageLoader, Mockito.times(3)).loadPackage(Mockito.any());
    }

    @Test
    public void testLoadPackagesWorksAndReturnsValidPackageInfoObjectAndInvokesProcessFile() {
        JarModuleLoader packageLoader = Mockito.spy(new JarModuleLoader(new JarModuleExportStrategy(), PATHS));

        List<String> classes = MOCK_JAR_ENTRIES
                .stream()
                .map(jarEntry -> jarEntry.getName().substring(0, jarEntry.getName().lastIndexOf(".class")))
                .collect(Collectors.toList());

        Mockito.doCallRealMethod().when(packageLoader).loadPackage(MOCK_FILES.get(0));
        Mockito.doReturn(jarFile).when(packageLoader).processFile(MOCK_FILES.get(0));
        Mockito.doReturn(MOCK_JAR_ENTRIES).when(packageLoader).extractEntries(Mockito.any());

        final ModuleInfo info = packageLoader.loadPackage(MOCK_FILES.get(0));

        Mockito.verify(packageLoader, Mockito.times(1)).processFile(Mockito.any());
        Assert.assertTrue(info.getClassNames().size() == MOCK_JAR_ENTRIES.size());
        Assert.assertArrayEquals(classes.toArray(), info.getClassNames().toArray());
        Assert.assertSame(MOCK_FILES.get(0), info.getFile());
        Assert.assertSame(jarFile, info.getJarFile());
        Assert.assertSame(JarModuleExportStrategy.class, info.getExportStrategy().getClass());
    }
}
