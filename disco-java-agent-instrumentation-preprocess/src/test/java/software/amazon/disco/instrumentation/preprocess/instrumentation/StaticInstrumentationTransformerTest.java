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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.export.JarExportStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarInfo;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;
import software.amazon.disco.instrumentation.preprocess.MockEntities;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class StaticInstrumentationTransformerTest {
    private static final String PACKAGE_SUFFIX = "suffix";

    StaticInstrumentationTransformer spyTransformer;
    PreprocessConfig config;

    @Mock
    DiscoAgentLoader mockAgentLoader;

    @Mock
    JarLoader mockJarPackageLoader;

    @Mock
    JarInfo jarInfo;

    @Before
    public void before() {
        config = PreprocessConfig.builder()
                .agentPath("a path")
                .jarPath("a path")
                .suffix(PACKAGE_SUFFIX)
                .build();

        spyTransformer = Mockito.spy(
                StaticInstrumentationTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .config(config)
                        .build()
        );

        Mockito.doReturn(Arrays.asList(MockEntities.makeMockJarInfo()))
                .when(mockJarPackageLoader).load(Mockito.any(PreprocessConfig.class));
    }

    @After
    public void after(){
        TransformerExtractor.getTransformers().clear();
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testTransformFailsWithNullConfig(){
        StaticInstrumentationTransformer.builder()
                .config(null)
                .build()
                .transform();
    }

    @Test
    public void testTransformWorksWithVerboseLogLevel() {
        config = PreprocessConfig.builder()
                .agentPath("a path")
                .jarPath("a path")
                .logLevel(Level.TRACE)
                .build();

        StaticInstrumentationTransformer.builder()
                .jarLoader(mockJarPackageLoader)
                .agentLoader(mockAgentLoader)
                .config(config)
                .build().transform();

        Assert.assertEquals(Level.TRACE, LogManager.getLogger().getLevel());
    }

    @Test
    public void testTransformWorksWithDefaultLogLevel() {
        spyTransformer.transform();
        Assert.assertEquals(LogManager.getLogger().getLevel(), Level.INFO);
    }

    @Test
    public void testTransformWorksAndInvokesLoadAgentAndPackages() {
        spyTransformer = Mockito.spy(
                StaticInstrumentationTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .config(config)
                        .build()
        );
        spyTransformer.transform();

        Mockito.verify(mockAgentLoader).loadAgent(Mockito.any(PreprocessConfig.class), Mockito.any(TransformerExtractor.class));
        Mockito.verify(mockJarPackageLoader).load(Mockito.any(PreprocessConfig.class));
    }

    @Test
    public void testTransformWorksAndInvokesPackageLoader() {
        spyTransformer.transform();

        Mockito.verify(mockJarPackageLoader).load(Mockito.any(PreprocessConfig.class));
        Mockito.verify(spyTransformer).applyInstrumentation(Mockito.any());
    }

    @Test
    public void testApplyInstrumentationWorks() throws IllegalClassFormatException {
        Instrumentation delegate = Mockito.mock(Instrumentation.class);
        JarExportStrategy strategy = Mockito.mock(JarExportStrategy.class);
        Map<String, InstrumentedClassState> instrumentedClasses = MockEntities.makeInstrumentedClassesMap();
        File file = Mockito.mock(File.class);

        Map<String, byte[]> byteArrayMap = new HashMap<>();
        byteArrayMap.put("ClassA", new byte[]{1});
        byteArrayMap.put("ClassB", new byte[]{2});

        TransformerExtractor transformerExtractor = new TransformerExtractor(delegate);
        ClassFileTransformer transformer_1 = Mockito.mock(ClassFileTransformer.class);
        ClassFileTransformer transformer_2 = Mockito.mock(ClassFileTransformer.class);
        transformerExtractor.addTransformer(transformer_1);
        transformerExtractor.addTransformer(transformer_2);

        Mockito.when(jarInfo.getExportStrategy()).thenReturn(strategy);
        Mockito.when(jarInfo.getClassByteCodeMap()).thenReturn(byteArrayMap);
        Mockito.when(jarInfo.getFile()).thenReturn(file);
        Mockito.when(file.getAbsolutePath()).thenReturn("mock/path");
        Mockito.doReturn(instrumentedClasses).when(spyTransformer).getInstrumentedClasses();

        spyTransformer.applyInstrumentation(jarInfo);

        Mockito.verify(strategy).export(jarInfo, instrumentedClasses, config);
        Mockito.verify(transformer_1).transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassA"), Mockito.eq(null), Mockito.eq(null), Mockito.eq(new byte[]{1}));
        Mockito.verify(transformer_1).transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassB"), Mockito.eq(null), Mockito.eq(null), Mockito.eq(new byte[]{2}));
        Assert.assertTrue(instrumentedClasses.isEmpty());
    }
}
