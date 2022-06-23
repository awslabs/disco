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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    static List<String> argsList = Arrays.asList("verbose", "runtimeonly", "loggerfactory=com.amazon.SomeFactory");

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
            outputStream.write("verbose\nruntimeonly\n".getBytes(StandardCharsets.UTF_8));
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
        Mockito.doReturn(argsList).when(parser).readConfigFileFromPluginPath(Mockito.anyString());
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

        parser.applyConfigOverride(config);

        Assert.assertTrue(config.isRuntimeOnly());
        Assert.assertTrue(config.isVerbose());
        Assert.assertEquals("com.amazon.SomeFactory", config.getLoggerFactoryClass());
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
        Mockito.doReturn(Arrays.asList("runtimeonly=false")).when(parser).readConfigFileFromPluginPath(Mockito.anyString());

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
    public void testReadConfigFileFromPluginPathReturnsArgs() throws IOException {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());
        File fakePluginDir = temporaryFolder.newFolder();
        generateTestConfigFile(" \nruntimeonly\nverbose\n\n loggerFactory=com.amazon.SomeFactory ", fakePluginDir);

        List<String> args = parser.readConfigFileFromPluginPath(fakePluginDir.getAbsolutePath());

        Assert.assertEquals(3, args.size());
        assertTrue(args.contains("runtimeonly"));
        assertTrue(args.contains("verbose"));
        assertTrue(args.contains("loggerFactory=com.amazon.SomeFactory"));
    }

    @Test
    public void testReadConfigFileFromPluginPathReturnsEmptyList_whenFileIsEmpty() throws IOException {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());
        File fakePluginDir = temporaryFolder.newFolder();
        File emptyConfigFile = new File(fakePluginDir, "disco.config");
        emptyConfigFile.createNewFile();

        List<String> args = parser.readConfigFileFromPluginPath(fakePluginDir.getAbsolutePath());

        assertTrue(args.isEmpty());
    }

    @Test
    public void testReadConfigFileFromPluginPathReturnsNull_whenFileNotExist() throws IOException {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());

        List<String> args = parser.readConfigFileFromPluginPath(temporaryFolder.newFolder().getAbsolutePath());

        assertNull(args);
    }

    @Test
    public void testReadConfigFileFromPluginPathReturnsArgs_whenConfigFileHasOneEntryAndEndsWithNewLine() throws IOException {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());
        File fakePluginDir = temporaryFolder.newFolder();
        generateTestConfigFile("runtimeonly\n", fakePluginDir);

        List<String> args = parser.readConfigFileFromPluginPath(fakePluginDir.getAbsolutePath());

        assertEquals(1, args.size());
        assertEquals("runtimeonly", args.get(0));
    }

    @Test
    public void testReadConfigFileFromPluginPathReturnsNull_whenPluginPathNotExist() {
        Mockito.doCallRealMethod().when(parser).readConfigFileFromPluginPath(Mockito.anyString());

        List<String> args = parser.readConfigFileFromPluginPath("invalid_path");

        assertNull(args);
    }

    private void generateTestConfigFile(String content, File pluginPath) throws IOException {
        File configFile = new File(pluginPath, "disco.config");
        configFile.createNewFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(configFile)) {
            fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
