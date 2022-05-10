/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.config;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentConfigParserTest {
    private AgentConfigParser parser;
    private Properties prop;
    private File overrideFile;
    private AgentConfig config;
    private ClassLoader classloader;

    static File configOverrideFile;
    static File fakeAgent;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeAll() throws IOException {
        File discoDir = temporaryFolder.newFolder("disco");
        fakeAgent = new File(discoDir, "disco-java-agent-1.0.jar");
        configOverrideFile = Paths.get(discoDir.getAbsolutePath(), AgentConfigParser.CONFIG_FILE_NAME).toFile();
    }

    @Before
    public void before() throws MalformedURLException {
        parser = Mockito.spy(new AgentConfigParser());
        config = new AgentConfig(null);

        overrideFile = Mockito.mock(File.class);
        Mockito.doReturn(true).when(overrideFile).isFile();
        Mockito.doReturn(true).when(overrideFile).exists();
        Mockito.doReturn(overrideFile).when(parser).getDiscoConfigOverrideFileFromAgentPath(Mockito.any(ClassLoader.class));

        prop = Mockito.mock(Properties.class);
        Mockito.doReturn(prop).when(parser).readPropertiesFile(Mockito.any(File.class));

        classloader = Mockito.mock(ClassLoader.class);

        URL url = Paths.get("file:" + fakeAgent.getAbsolutePath() + "!/software/amazon/disco/agent/config/AgentConfigParser.class").toUri().toURL();
        Mockito.doReturn(url).when(classloader).getResource(AgentConfigParser.class.getName().replace('.', '/') + ".class");
    }

    @After
    public void after() {
        if (configOverrideFile.exists()) {
            configOverrideFile.delete();
        }
    }

    @Test
    public void testArgumentParsing() {
        final String argLine = "verbose:runtimeonly:pluginpath=path/to/plugins";
        Mockito.doNothing().when(parser).applyConfigOverride(Mockito.any(AgentConfig.class));

        AgentConfig config = parser.parseCommandLine(argLine);
        assertTrue(config.isRuntimeOnly());
        assertTrue(config.isVerbose());
        assertFalse(config.isExtraverbose());
        assertEquals("path/to/plugins", config.getPluginPath());
        Mockito.verify(parser).applyConfigOverride(config);
    }

    @Test
    public void testApplyConfigOverride_whenOverridePropertyIsDefinedAndValueIsTrue() {
        config.setRuntimeOnly(false);
        Assert.assertFalse(config.isRuntimeOnly());

        Mockito.doReturn(true).when(prop).containsKey("runtimeonly");
        Mockito.doReturn("true").when(prop).getProperty("runtimeonly");

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverride_whenOverridePropertyIsDefinedAndValueIsFalse() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());

        Mockito.doReturn("false").when(prop).getProperty("runtimeonly");

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverride_whenOverridePropertyIsDefinedAndValueIsEmptyString() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());

        Mockito.doReturn("").when(prop).getProperty("runtimeonly");

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverride_whenOverridePropertyIsDefinedAndValueIsRandomString() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());

        Mockito.doReturn("random").when(prop).getProperty("runtimeonly");

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverride_whenOverridePropertyIsNotDefined() {
        config.setRuntimeOnly(false);
        Assert.assertFalse(config.isRuntimeOnly());
        Mockito.doReturn(null).when(prop).getProperty("runtimeonly");

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverride_whenOverrideFileNotExist() {
        config.setRuntimeOnly(false);
        Assert.assertFalse(config.isRuntimeOnly());
        Mockito.doReturn(null).when(prop).getProperty("runtimeonly");
        Mockito.doReturn(false).when(overrideFile).exists();

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testGetDiscoConfigOverrideFileFromAgentPathReturnsNull_whenConfigFileNotExist() {
        Mockito.doCallRealMethod().when(parser).getDiscoConfigOverrideFileFromAgentPath(classloader);

        File file = parser.getDiscoConfigOverrideFileFromAgentPath(classloader);

        Assert.assertNull(file);
    }

    @Test
    public void testGetDiscoConfigOverrideFileFromAgentPathReturnsFile_whenConfigFileExist() throws IOException {
        Mockito.doCallRealMethod().when(parser).getDiscoConfigOverrideFileFromAgentPath(classloader);

        configOverrideFile.createNewFile();

        File file = parser.getDiscoConfigOverrideFileFromAgentPath(classloader);
        Assert.assertEquals(configOverrideFile.getAbsolutePath(), file.getAbsolutePath());
    }

    @Test
    public void testReadPropertiesFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("runtimeonly", "true");
        createPropertiesFile(configOverrideFile, properties);
        OutputStream output = new FileOutputStream(configOverrideFile);
        properties.store(output, null);
        Mockito.doCallRealMethod().when(parser).readPropertiesFile(configOverrideFile);

        Properties prop = parser.readPropertiesFile(configOverrideFile);

        Assert.assertEquals(properties, prop);
    }

    private void createPropertiesFile(File outputFile, Properties properties) throws IOException {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        try (OutputStream output = new FileOutputStream(outputFile)) {
            properties.store(output, null);
        } catch (IOException e) {
            throw e;
        }
    }
}
