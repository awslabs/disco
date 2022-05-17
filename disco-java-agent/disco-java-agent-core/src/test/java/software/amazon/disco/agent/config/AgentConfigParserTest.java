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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentConfigParserTest {
    private AgentConfigParser parser;
    private LinkedHashMap<String, String> argsMap;
    private File overrideFile;
    private AgentConfig config;

    static File configOverrideFile;
    static File pluginPath;
    static File discoDir;
    static String args = "verbose:runtimeonly:pluginpath=some_location";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeAll() throws IOException {
        discoDir = temporaryFolder.newFolder("disco");
        pluginPath = new File(discoDir, "plugin");
        pluginPath.mkdirs();

        configOverrideFile = Paths.get(pluginPath.getAbsolutePath(), AgentConfigParser.CONFIG_FILE_NAME).toFile();
        configOverrideFile.createNewFile();

        try (FileOutputStream outputStream = new FileOutputStream(configOverrideFile)) {
            outputStream.write(args.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Before
    public void before() throws MalformedURLException {
        parser = Mockito.spy(new AgentConfigParser());
        config = new AgentConfig(new ArrayList<>());
        config.setPluginPath("somePath");

        overrideFile = Mockito.mock(File.class);
        Mockito.doReturn(true).when(overrideFile).isFile();
        Mockito.doReturn(true).when(overrideFile).exists();

        argsMap = new LinkedHashMap<>();

        Mockito.doReturn(argsMap).when(parser).parseArgsStringToMap(Mockito.anyString());
        Mockito.doReturn(args).when(parser).readConfigFileFromPluginPath(Mockito.anyString());
    }

    @Test
    public void testArgumentParsing() {
        Mockito.doNothing().when(parser).applyConfigOverride(Mockito.any(AgentConfig.class));
        Mockito.doCallRealMethod().when(parser).parseArgsStringToMap(Mockito.anyString());

        AgentConfig config = parser.parseCommandLine(args);

        assertTrue(config.isRuntimeOnly());
        assertTrue(config.isVerbose());
        assertFalse(config.isExtraverbose());
        assertEquals("some_location", config.getPluginPath());
        Mockito.verify(parser).applyConfigOverride(config);
    }

    @Test
    public void testApplyConfigOverride() {
        config.setRuntimeOnly(false);
        config.setVerbose(false);
        config.setLoggerFactoryClass(null);

        argsMap.put("runtimeonly", "");
        argsMap.put("verbose", "");
        argsMap.put("loggerfactory", "Factory");

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
        Assert.assertTrue(config.isVerbose());
        Assert.assertEquals("Factory", config.getLoggerFactoryClass());
    }

    @Test
    public void testApplyConfigOverrideChangeRuntimeOnlyToTrue_whenOverridePropertyIsDefinedAndValueIsEmptyString() {
        config.setRuntimeOnly(false);
        Assert.assertFalse(config.isRuntimeOnly());

        argsMap.put("runtimeonly", "");

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverrideMaintainsRuntimeOnlyValue_whenOverridePropertyIsDefinedAndValueIsRandomString() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());

        argsMap.put("runtimeonly", "random string");

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverrideChangeRuntimeOnlyToFalse_whenOverridePropertyIsDefinedAndValueIsFalse() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());
        argsMap.put("runtimeonly", "fAlse");

        parser.applyConfigOverride(config);

        Assert.assertFalse(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverrideMaintainsRuntimeOnlyValue_whenOverridePropertyIsNotDefined() {
        config.setRuntimeOnly(true);
        Assert.assertTrue(config.isRuntimeOnly());
        Assert.assertTrue(argsMap.isEmpty());

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverrideMaintainsRuntimeOnlyValue_whenOverrideFileNotExist() {
        config.setRuntimeOnly(true);

        Assert.assertTrue(config.isRuntimeOnly());
        Assert.assertFalse(new File(config.getPluginPath(), "disco.config").exists());

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
    }

    @Test
    public void testApplyConfigOverrideIgnoresArg_whenPluginPathIsSupplied() {
        config.setPluginPath("original");

        argsMap.put("pluginpath", "new path");

        parser.applyConfigOverride(config);

        Assert.assertEquals("original", config.getPluginPath());
    }

    @Test
    public void testGetDiscoConfigOverrideFileFromAgentPathReturnsNull_whenConfigFileNotExist() {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());

        String configFileStr = parser.readConfigFileFromPluginPath(temporaryFolder.getRoot().getAbsolutePath());

        Assert.assertNull(configFileStr);
    }

    @Test
    public void testGetDiscoConfigOverrideFileFromAgentPathReturnsFileContent_whenConfigFileExist() {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());

        String configFileStr = parser.readConfigFileFromPluginPath(pluginPath.getAbsolutePath());

        Assert.assertEquals(args, configFileStr);
    }
}
