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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.instrumentation.preprocess.exceptions.AgentLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.exceptions.ModuleLoaderNotProvidedException;
import software.amazon.disco.instrumentation.preprocess.export.JarModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.JarModuleLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ModuleTransformerTest {
    private static final String PACKAGE_SUFFIX = "suffix";

    ModuleTransformer spyTransformer;

    @Mock
    DiscoAgentLoader mockAgentLoader;

    @Mock
    JarModuleLoader mockJarPackageLoader;

    List<ModuleInfo> moduleInfos;

    @Before
    public void before() {
        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .suffix(PACKAGE_SUFFIX)
                        .build()
        );

        Mockito.doReturn(Arrays.asList(MockEntities.makeMockPackageInfo())).when(mockJarPackageLoader).loadPackages();

        moduleInfos = new ArrayList<>();
        moduleInfos.add(Mockito.mock(ModuleInfo.class));
        moduleInfos.add(Mockito.mock(ModuleInfo.class));
    }

    @Test
    public void testTransformWorksWithDefaultLogLevel(){
        spyTransformer.transform();

        Assert.assertEquals(LogManager.getLogger().getLevel(), Level.INFO);
    }

    @Test
    public void testTransformWorksWithVerboseLogLevel(){
        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .logLevel(Level.TRACE)
                        .build()
        );

        spyTransformer.transform();

        Assert.assertEquals(Level.TRACE, LogManager.getLogger().getLevel());
    }

    @Test(expected = AgentLoaderNotProvidedException.class)
    public void testTransformFailsWhenNoAgentLoaderProvided(){
        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .build()
        );
        spyTransformer.transform();
    }


    @Test(expected = ModuleLoaderNotProvidedException.class)
    public void testTransformFailsWhenNoPackageLoaderProvided() {
        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .agentLoader(mockAgentLoader)
                        .build()
        );
        spyTransformer.transform();
    }

    @Test
    public void testTransformWorksAndInvokesLoadAgentAndPackages() {
        spyTransformer = Mockito.spy(
                ModuleTransformer.builder()
                        .jarLoader(mockJarPackageLoader)
                        .agentLoader(mockAgentLoader)
                        .build()
        );
        spyTransformer.transform();

        Mockito.verify(mockAgentLoader).loadAgent();
        Mockito.verify(mockJarPackageLoader).loadPackages();
    }

    @Test
    public void testTransformWorksAndInvokesPackageLoader() {
        spyTransformer.transform();

        Mockito.verify(mockJarPackageLoader).loadPackages();
        Mockito.verify(spyTransformer).applyInstrumentation(Mockito.any());
    }


    @Test
    public void testApplyInstrumentationWorksAndInvokesExport() {
        Mockito.doCallRealMethod().when(spyTransformer).applyInstrumentation(Mockito.any());

        JarModuleExportStrategy s1 = Mockito.mock(JarModuleExportStrategy.class);
        Mockito.when(moduleInfos.get(0).getExportStrategy()).thenReturn(s1);

        Map<String, InstrumentedClassState> instrumentedClasses = MockEntities.makeInstrumentedClassesMap();
        Mockito.doReturn(instrumentedClasses).when(spyTransformer).getInstrumentedClasses();

        spyTransformer.applyInstrumentation(moduleInfos.get(0));

        Mockito.verify(moduleInfos.get(0)).getClassNames();
        Mockito.verify(s1).export(moduleInfos.get(0), instrumentedClasses, PACKAGE_SUFFIX);
        Assert.assertTrue(instrumentedClasses.isEmpty());
    }
}
