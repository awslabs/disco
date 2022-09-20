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

package software.amazon.disco.instrumentation.preprocess.cli;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.NoOpCacheStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriverTests {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    Map<String, Set<String>> mockSourcePaths;
    ChecksumCacheStrategy mockCheckSumStrategy;
    PreprocessConfig mockConfig;

    @Before
    public void before(){
        mockSourcePaths = Mockito.spy(new HashMap<>());
        mockCheckSumStrategy = Mockito.mock(ChecksumCacheStrategy.class);
        mockConfig = Mockito.mock(PreprocessConfig.class);

        Mockito.doReturn(mockCheckSumStrategy).when(mockConfig).getCacheStrategy();
    }

    @Test
    public void testRemoveCachedInputSourcesForProcessingReturnsImmediately_whenStrategyIsNOOP() throws PreprocessCacheException {
        Mockito.doReturn(mockSourcePaths).when(mockConfig).getSourcePaths();
        Mockito.doReturn(new NoOpCacheStrategy()).when(mockConfig).getCacheStrategy();

        Driver.removeCachedInputSourcesForProcessing(mockConfig);
        Mockito.verifyNoInteractions(mockSourcePaths);
    }

    @Test
    public void testRemoveCachedInputSourcesForProcessingRemovesCachedNonJDKDependencies_whenStrategyIsCheckSum() throws PreprocessCacheException {
        Map<String, Set<String>> sourcePaths = Collections.singletonMap(
            "destination_path", new HashSet<>(Arrays.asList("cached_dependency_1", "cached_dependency_2", "uncached_dependency"))
        );

        Mockito.doReturn(sourcePaths).when(mockConfig).getSourcePaths();
        Mockito.doReturn(true).when(mockCheckSumStrategy).isSourceCached(Paths.get("cached_dependency_1"));
        Mockito.doReturn(true).when(mockCheckSumStrategy).isSourceCached(Paths.get("cached_dependency_2"));
        Mockito.doReturn(false).when(mockCheckSumStrategy).isSourceCached(Paths.get("uncached_dependency"));

        Driver.removeCachedInputSourcesForProcessing(mockConfig);

        Assert.assertEquals(1, sourcePaths.get("destination_path").size());
        Assert.assertTrue(sourcePaths.get("destination_path").contains("uncached_dependency"));
    }

    @Test
    public void testRemoveCachedInputSourcesForProcessingRemovesCachedJDKBaseModule_whenStrategyIsCheckSum() throws IOException, PreprocessCacheException {
        File jdkDir = new File(tempFolder.getRoot(), "/some_dir/some_jdk");
        File jdkBaseModule = new File(jdkDir, "lib/rt.jar");
        jdkBaseModule.getParentFile().mkdirs();
        jdkBaseModule.createNewFile();

        Mockito.doReturn(Collections.emptyMap()).when(mockConfig).getSourcePaths();
        Mockito.doReturn(jdkDir.getAbsolutePath()).when(mockConfig).getJdkPath();
        Mockito.doReturn(true).when(mockCheckSumStrategy).isSourceCached(Mockito.any());

        Driver.removeCachedInputSourcesForProcessing(mockConfig);

        Mockito.verify(mockConfig).setJdkPath(null);
    }
}
