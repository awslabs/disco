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
import software.amazon.disco.instrumentation.preprocess.export.JarModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.JarModuleLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ModuleTransformerTest {
    private static final String PACKAGE_SUFFIX = "suffix";

    ModuleTransformer spyTransformer;
    PreprocessConfig config;

    @Mock
    DiscoAgentLoader mockAgentLoader;

    @Mock
    JarModuleLoader mockJarPackageLoader;

    @Mock
    ModuleInfo moduleInfo;

    @Before
    public void before() {
        config = PreprocessConfig.builder()
                .agentPath("a path")
                .jarPath("a path")
                .suffix(PACKAGE_SUFFIX)
                .build();

        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .config(config)
                        .build()
        );

        Mockito.doReturn(Arrays.asList(MockEntities.makeMockModuleInfo()))
                .when(mockJarPackageLoader).loadPackages(Mockito.any(PreprocessConfig.class));
    }

    @After
    public void after(){
        TransformerExtractor.getTransformers().clear();
    }

    @Test
    public void testTransformWorksWithVerboseLogLevel() {
        config = PreprocessConfig.builder()
                .agentPath("a path")
                .jarPath("a path")
                .logLevel(Level.TRACE)
                .build();

        ModuleTransformer.builder()
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
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .config(config)
                        .build()
        );
        spyTransformer.transform();

        Mockito.verify(mockAgentLoader).loadAgent(Mockito.any(PreprocessConfig.class), Mockito.any(TransformerExtractor.class));
        Mockito.verify(mockJarPackageLoader).loadPackages(Mockito.any(PreprocessConfig.class));
    }

    @Test
    public void testTransformWorksAndInvokesPackageLoader() {
        spyTransformer.transform();

        Mockito.verify(mockJarPackageLoader).loadPackages(Mockito.any(PreprocessConfig.class));
        Mockito.verify(spyTransformer).applyInstrumentation(Mockito.any());
    }

    @Test
    public void testApplyInstrumentationWorks() throws IllegalClassFormatException {
        JarModuleExportStrategy strategy = Mockito.mock(JarModuleExportStrategy.class);
        Map<String, InstrumentedClassState> instrumentedClasses = MockEntities.makeInstrumentedClassesMap();
        File file = Mockito.mock(File.class);

        Map<String, byte[]> byteArrayMap = new HashMap<>();
        byteArrayMap.put("ClassA", new byte[]{1});
        byteArrayMap.put("ClassB", new byte[]{2});

        TransformerExtractor transformerExtractor = new TransformerExtractor();
        ClassFileTransformer transformer_1 = Mockito.mock(ClassFileTransformer.class);
        ClassFileTransformer transformer_2 = Mockito.mock(ClassFileTransformer.class);
        transformerExtractor.addTransformer(transformer_1);
        transformerExtractor.addTransformer(transformer_2);

        Mockito.when(moduleInfo.getExportStrategy()).thenReturn(strategy);
        Mockito.when(moduleInfo.getClassByteCodeMap()).thenReturn(byteArrayMap);
        Mockito.when(moduleInfo.getFile()).thenReturn(file);
        Mockito.when(file.getAbsolutePath()).thenReturn("mock/path");
        Mockito.doReturn(instrumentedClasses).when(spyTransformer).getInstrumentedClasses();

        spyTransformer.applyInstrumentation(moduleInfo);

        Mockito.verify(moduleInfo).getClassByteCodeMap();
        Mockito.verify(strategy).export(moduleInfo, instrumentedClasses, config);
        Mockito.verify(transformer_1).transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassA"), Mockito.eq(null), Mockito.eq(null), Mockito.eq(new byte[]{1}));
        Mockito.verify(transformer_1).transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassB"), Mockito.eq(null), Mockito.eq(null), Mockito.eq(new byte[]{2}));
        Assert.assertTrue(instrumentedClasses.isEmpty());
    }
}
