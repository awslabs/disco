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

package software.amazon.disco.instrumentation.preprocess.loaders.agents;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.agent.DiscoAgentTemplate;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoAgentToLoadException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.TransformationListener;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarFile;

public class DiscoAgentLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = NoAgentToLoadException.class)
    public void testLoadAgentFailOnNullPaths() throws NoAgentToLoadException {
        new DiscoAgentLoader().loadAgent(null, null);
    }

    @Test
    public void testParsingJavaVersionWorks() {
        PreprocessConfig config = PreprocessConfig.builder()
                .agentPath("path")
                .javaVersion("11")
                .build();
        ClassFileVersion version = DiscoAgentLoader.parseClassFileVersionFromConfig(config);
        Assert.assertEquals(ClassFileVersion.JAVA_V11, version);

        // test if default is set to java 8
        PreprocessConfig anotherConfig = PreprocessConfig.builder()
                .agentPath("path")
                .build();
        version = DiscoAgentLoader.parseClassFileVersionFromConfig(anotherConfig);
        Assert.assertEquals(ClassFileVersion.JAVA_V8, version);
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testParsingJavaVersionFailsWithInvalidJavaVersion() {
        PreprocessConfig config = PreprocessConfig.builder()
                .agentPath("path")
                .javaVersion("a version")
                .build();
        DiscoAgentLoader.parseClassFileVersionFromConfig(config);
    }

    @Test
    public void testLoadAgentRegistersAgentBuilderTransformerAndInstallsAgent() throws Exception {
        Instrumentation delegate = Mockito.mock(Instrumentation.class);
        Instrumentation instrumentation = Mockito.spy(new TransformerExtractor(delegate));
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        Mockito.when(agentBuilder.with(Mockito.any(ByteBuddy.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.Listener.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.PoolStrategy.class))).thenReturn(agentBuilder);

        Map<String, byte[]> entries = new HashMap<>();
        entries.put("Foo.class", "Foo.class".getBytes());

        File file = TestUtils.createJar(temporaryFolder, "TestJarFile", entries);
        PreprocessConfig config = PreprocessConfig.builder().agentPath(file.getAbsolutePath()).build();

        Assert.assertNull(DiscoAgentTemplate.getAgentConfigFactory());

        DiscoAgentLoader loader = Mockito.spy(new DiscoAgentLoader());
        loader.loadAgent(config, instrumentation);

        // check if agentConfigSupplier with an AgentBuilderTransformer is set
        Supplier<AgentConfig> agentConfigSupplier = DiscoAgentTemplate.getAgentConfigFactory();
        Assert.assertNotNull(agentConfigSupplier);
        Assert.assertNotNull(agentConfigSupplier.get());
        agentConfigSupplier.get().getAgentBuilderTransformer().apply(agentBuilder, null);
        Mockito.verify(agentBuilder).with(Mockito.any(ByteBuddy.class));
        Mockito.verify(agentBuilder).with(Mockito.any(TransformationListener.class));
        Mockito.verify(agentBuilder).with(Mockito.any(AgentBuilder.PoolStrategy.class));

        // check if a ByteBuddy instance with the correct java version is being installed using its own equals method
        Assert.assertEquals(ClassFileVersion.JAVA_V8, DiscoAgentLoader.parseClassFileVersionFromConfig(config));
        ArgumentCaptor<ByteBuddy> byteBuddyArgumentCaptor = ArgumentCaptor.forClass(ByteBuddy.class);
        Mockito.verify(agentBuilder).with(byteBuddyArgumentCaptor.capture());
        Assert.assertEquals(new ByteBuddy(ClassFileVersion.JAVA_V8), byteBuddyArgumentCaptor.getValue());

        // the Injector will invoke addToBootstrapClasspath() which in turn calls the method tested below
        ArgumentCaptor<JarFile> jarFileArgumentCaptor = ArgumentCaptor.forClass(JarFile.class);
        Mockito.verify(instrumentation).appendToBootstrapClassLoaderSearch(jarFileArgumentCaptor.capture());
        Assert.assertEquals(file.getAbsolutePath(), jarFileArgumentCaptor.getValue().getName());
    }
}
