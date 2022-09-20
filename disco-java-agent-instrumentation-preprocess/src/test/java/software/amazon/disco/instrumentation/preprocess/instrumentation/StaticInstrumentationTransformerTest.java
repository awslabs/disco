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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.ClassFileLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.DirectoryLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class StaticInstrumentationTransformerTest {
    @Rule()
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    DiscoAgentLoader mockAgentLoader;

    @Mock
    JarLoader mockJarPackageLoader;

    @Mock
    DirectoryLoader mockDirectoryLoader;

    PreprocessConfig config;
    Map<Class<? extends ClassFileLoader>, ClassFileLoader> loaders;
    File fakeJar;

    @Before
    public void before() throws IOException {
        fakeJar = temporaryFolder.newFile("someJar.jar");

        Mockito.doReturn(MockEntities.makeMockJarInfo())
            .when(mockJarPackageLoader)
            .load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));

        Mockito.doReturn(MockEntities.makeMockJarInfo())
            .when(mockDirectoryLoader)
            .load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));

        config = PreprocessConfig.builder()
            .agentPath("a path")
            .sourcePath("lib", new HashSet<>(Arrays.asList(fakeJar.getAbsolutePath(), temporaryFolder.getRoot().getAbsolutePath())))
            .failOnUnresolvableDependency(false)
            .outputDir("build/private/disco/static-instrumentation")
            .build();

        loaders = new HashMap<>();
        loaders.put(JarLoader.class, mockJarPackageLoader);
        loaders.put(DirectoryLoader.class, mockDirectoryLoader);
    }

    @After
    public void after() {
        TransformerExtractor.getTransformers().clear();
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testTransformFailsWithNullConfig() throws PreprocessCacheException {
        StaticInstrumentationTransformer.builder()
            .config(null)
            .build()
            .transform();
    }

    @Test
    public void testTransformWorksAndInvokesHelperMethods() throws PreprocessCacheException {
        StaticInstrumentationTransformer transformer = configureStaticInstrumentationTransformer();

        transformer.transform();

        Mockito.verify(mockAgentLoader).loadAgent(Mockito.any(PreprocessConfig.class), Mockito.any(TransformerExtractor.class));
        Mockito.verify(mockJarPackageLoader, Mockito.times(1)).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));
        Mockito.verify(mockDirectoryLoader, Mockito.times(1)).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));

        Mockito.verify(transformer).processAllSources();
        Mockito.verify(transformer).logInstrumentationSummary();
    }

    @Test
    public void testProcessAllSourcesWorksAndPopulatesInstrumentationOutcome() throws PreprocessCacheException {
        StaticInstrumentationTransformer transformer = configureStaticInstrumentationTransformer();

        transformer.processAllSources();

        assertEquals(2, transformer.getAllOutcomes().size());
        assertEquals(InstrumentationOutcome.Status.NO_OP, transformer.getAllOutcomes().get(0).getStatus());
        assertEquals(InstrumentationOutcome.Status.NO_OP, transformer.getAllOutcomes().get(1).getStatus());
    }

    @Test
    public void testProcessAllSourcesWorksAndPopulatesInstrumentationOutcomeWithSourceLoadingError() throws IllegalClassFormatException, PreprocessCacheException {
        StaticInstrumentationTransformer transformer = configureStaticInstrumentationTransformer();

        TransformerExtractor.getTransformers().clear();

        ClassFileTransformer mockTransformer = Mockito.mock(ClassFileTransformer.class);
        TransformerExtractor.getTransformers().add(mockTransformer);
        Mockito.doThrow(new InstrumentationException("some thing went wrong", new IllegalStateException())).when(mockTransformer)
            .transform(Mockito.any(), Mockito.anyString(), Mockito.isNull(), Mockito.isNull(), Mockito.any(byte[].class));

        transformer.processAllSources();

        assertEquals(2, transformer.getAllOutcomes().size());
        assertEquals(InstrumentationOutcome.Status.WARNING_OCCURRED, transformer.getAllOutcomes().get(0).getStatus());
        assertEquals(InstrumentationOutcome.Status.WARNING_OCCURRED, transformer.getAllOutcomes().get(1).getStatus());
    }

    private StaticInstrumentationTransformer configureStaticInstrumentationTransformer() {
        StaticInstrumentationTransformer transformer = Mockito.spy(
            StaticInstrumentationTransformer.builder()
                .classFileLoaders(loaders)
                .agentLoader(mockAgentLoader)
                .config(config)
                .build()
        );

        return transformer;
    }
}
