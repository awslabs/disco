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

package software.amazon.disco.instrumentation.preprocess.instrumentation.cache;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.AGENT_CHECKSUM_PROPERTY_KEY;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.MANIFEST_FILE_NAME;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.PLUGINS_CHECKSUM_PROPERTY_KEY;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.PREPROCESSOR_CHECKSUM_PROPERTY_KEY;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY;
import static software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy.TEMPORARY_MANIFEST_FOLDER_NAME;

public class ChecksumCacheStrategyTests {
    @ClassRule()
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    final static String AGENT_CHECKSUM_PROPERTY_VAL = "dummy_agent_check_sum";
    final static String PLUGINS_CHECKSUM_PROPERTY_VAL = "dummy_plugins_check_sum";
    final static String PREPROCESSOR_CHECKSUM_PROPERTY_VAL = "dummy_preprocessor_check_sum";
    final static String JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL = "dummy_java_base_module_checksum";
    final static String PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL = "dummy_config_string";

    static File fakeAgentJar;
    static File pluginsDir;
    static File fakePluginJarA;
    static File fakePluginJarB;

    Properties dummyManifest;
    PreprocessConfig config;
    ChecksumCacheStrategy strategy;
    Path mockPreprocessorPath;
    Map expectedInstrumentationContext;

    @BeforeClass
    public static void beforeAll() throws Exception {
        fakeAgentJar = TestUtils.createJar(temporaryFolder, "fakeAgent.jar", Collections.singletonMap("ClassA.class", "class".getBytes(StandardCharsets.UTF_8)));
        pluginsDir = temporaryFolder.newFolder("plugins");

        fakePluginJarA = new File(pluginsDir, "plugin_a.jar");
        TestUtils.createJar(fakePluginJarA, Collections.singletonMap("ClassA.class", "classA".getBytes(StandardCharsets.UTF_8)));

        fakePluginJarB = new File(pluginsDir, "plugin_b.jar");
        TestUtils.createJar(fakePluginJarB, Collections.singletonMap("ClassB.class", "classB".getBytes(StandardCharsets.UTF_8)));

        StringBuilder manifestContentBuilder = new StringBuilder("#somecomments\n")
            .append(PLUGINS_CHECKSUM_PROPERTY_KEY)
            .append("=previous_dummy_plugins_check_sum\n")
            .append(AGENT_CHECKSUM_PROPERTY_KEY)
            .append("=previous_dummy_agent_check_sum\n")
            .append(PREPROCESSOR_CHECKSUM_PROPERTY_KEY)
            .append("=previous_PREPROCESSOR_PROPERTY_VAL\n")
            .append(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY)
            .append("=previous_dummy_java_base_module_check_sum\n")
            .append(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY)
            .append("=previous_dummy_config_string\n")
            .append("someJar.jar=some_hash\n");

        TestUtils.createFile(temporaryFolder.getRoot(), MANIFEST_FILE_NAME, manifestContentBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Before
    public void before() throws PreprocessCacheException {
        dummyManifest = new Properties();
        dummyManifest.setProperty(AGENT_CHECKSUM_PROPERTY_KEY, AGENT_CHECKSUM_PROPERTY_VAL);
        dummyManifest.setProperty(PLUGINS_CHECKSUM_PROPERTY_KEY, PLUGINS_CHECKSUM_PROPERTY_VAL);
        dummyManifest.setProperty(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, PREPROCESSOR_CHECKSUM_PROPERTY_VAL);
        dummyManifest.setProperty(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL);
        dummyManifest.setProperty(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL);

        expectedInstrumentationContext = new HashMap<>();
        expectedInstrumentationContext.putAll(dummyManifest);

        dummyManifest.setProperty("someJar.jar", "some_hash");

        strategy = Mockito.spy(new ChecksumCacheStrategy());
        strategy.runtimeCache = dummyManifest;

        Mockito.doReturn(dummyManifest).when(strategy).readPropertiesFromFile(Mockito.any());

        mockPreprocessorPath = Mockito.mock(Path.class);
        Mockito.doReturn(mockPreprocessorPath).when(strategy).getPreprocessorJarPath();

        Mockito.doCallRealMethod().when(strategy).readPropertiesFromFile(Mockito.any());

        config = Mockito.spy(PreprocessConfig.builder()
            .outputDir(temporaryFolder.getRoot().getAbsolutePath())
            .agentPath(fakeAgentJar.getAbsolutePath())
            .agentArg("pluginpath=" + pluginsDir.getAbsolutePath())
            .build());

        Mockito.doReturn(PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL).when(config).toStringForCaching();
    }

    @Test
    public void testLoadManifestFileToRuntimeCache_whenManifestIsAbsent() throws PreprocessCacheException {
        config = PreprocessConfig.builder()
            .outputDir("InvalidDir")
            .agentPath(fakeAgentJar.getAbsolutePath())
            .agentArg("pluginpath=" + pluginsDir.getAbsolutePath())
            .build();

        Mockito.doReturn(AGENT_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksum(Paths.get(fakeAgentJar.getAbsolutePath()));
        Mockito.doReturn(PLUGINS_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksumForPlugins(Mockito.any());
        Mockito.doReturn(PREPROCESSOR_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksum(mockPreprocessorPath);
        Mockito.doCallRealMethod().when(strategy).readPropertiesFromFile(Mockito.any());
        Mockito.doReturn(dummyManifest).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        assertEquals(6, strategy.runtimeCache.size());
        verifyInstrumentationContextInCache(strategy.runtimeCache);
        assertEquals("some_hash", strategy.runtimeCache.getProperty("someJar.jar"));
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheNotResetCache_whenManifestIsPresent() throws PreprocessCacheException {
        Mockito.doReturn(AGENT_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksum(Paths.get(fakeAgentJar.getAbsolutePath()));
        Mockito.doReturn(PLUGINS_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksumForPlugins(Mockito.any());
        Mockito.doReturn(PREPROCESSOR_CHECKSUM_PROPERTY_VAL).when(strategy).computeChecksum(mockPreprocessorPath);

        Mockito.doReturn(dummyManifest).when(strategy).readPropertiesFromFile(Mockito.any());
        Mockito.doReturn(dummyManifest).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        assertEquals(6, strategy.runtimeCache.size());
        verifyInstrumentationContextInCache(strategy.runtimeCache);
        assertEquals("some_hash", strategy.runtimeCache.getProperty("someJar.jar"));
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheResetCache_whenManifestIsPresentAndAgentChanged() throws PreprocessCacheException {
        assertEquals(6, strategy.runtimeCache.size());

        Map currentInstrumentationContext = generateInstrumentationContext();
        currentInstrumentationContext.put(AGENT_CHECKSUM_PROPERTY_KEY, "updated_dummy_agent_check_sum");
        Mockito.doReturn(currentInstrumentationContext).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        expectedInstrumentationContext.put(AGENT_CHECKSUM_PROPERTY_KEY, "updated_dummy_agent_check_sum");
        verifyInstrumentationContextInCache(strategy.runtimeCache);
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheResetCache_whenManifestIsPresentAndPreprocessorChanged() throws PreprocessCacheException {
        assertEquals(6, strategy.runtimeCache.size());

        Map currentInstrumentationContext = generateInstrumentationContext();
        currentInstrumentationContext.put(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, "updated_preprocessor_check_sum");
        Mockito.doReturn(currentInstrumentationContext).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        expectedInstrumentationContext.put(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, "updated_preprocessor_check_sum");
        verifyInstrumentationContextInCache(strategy.runtimeCache);
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheResetCache_whenManifestIsPresentAndPluginsChanged() throws PreprocessCacheException {
        assertEquals(6, strategy.runtimeCache.size());

        Map currentInstrumentationContext = generateInstrumentationContext();
        currentInstrumentationContext.put(PLUGINS_CHECKSUM_PROPERTY_KEY, "updated_dummy_plugins_check_sum");
        Mockito.doReturn(currentInstrumentationContext).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        expectedInstrumentationContext.put(PLUGINS_CHECKSUM_PROPERTY_KEY, "updated_dummy_plugins_check_sum");
        verifyInstrumentationContextInCache(strategy.runtimeCache);
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheResetCache_whenManifestIsPresentAndPreprocessorConfigChanged() throws PreprocessCacheException {
        assertEquals(6, dummyManifest.size());

        Properties currentInstrumentationContext = generateInstrumentationContext();
        currentInstrumentationContext.put(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, "updated_value");

        Mockito.doReturn(currentInstrumentationContext).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        assertEquals(5, strategy.runtimeCache.size());
        expectedInstrumentationContext.put(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, "updated_value");
        verifyInstrumentationContextInCache(strategy.runtimeCache);
    }

    @Test
    public void testLoadManifestFileToRuntimeCacheResetCache_whenManifestIsPresentAndJavaRuntimeBaseChanged() throws PreprocessCacheException {
        assertEquals(6, dummyManifest.size());

        Properties currentInstrumentationContext = generateInstrumentationContext();
        currentInstrumentationContext.put(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, "updated_value");

        Mockito.doReturn(currentInstrumentationContext).when(strategy).computeCurrentInstrumentationContext(config);

        strategy.loadManifestFileToRuntimeCache(config);

        assertEquals(5, strategy.runtimeCache.size());
        expectedInstrumentationContext.put(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, "updated_value");
        verifyInstrumentationContextInCache(strategy.runtimeCache);
    }

    @Test(expected = PreprocessCacheException.class)
    public void testValidateCachedInstrumentationContextThrows_whenPreprocessorChecksumIsMissing() throws Exception {
        strategy.runtimeCache = generateInstrumentationContext();
        strategy.runtimeCache.remove(PREPROCESSOR_CHECKSUM_PROPERTY_KEY);

        strategy.validateCachedInstrumentationContext();
    }

    @Test(expected = PreprocessCacheException.class)
    public void testValidateCachedInstrumentationContextThrows_whenPluginsCheckSumIsMissing() throws Exception {
        strategy.runtimeCache = generateInstrumentationContext();
        strategy.runtimeCache.remove(PLUGINS_CHECKSUM_PROPERTY_KEY);

        strategy.validateCachedInstrumentationContext();
    }

    @Test(expected = PreprocessCacheException.class)
    public void testValidateCachedInstrumentationContextThrows_whenAgentCheckSumIsMissing() throws Exception {
        strategy.runtimeCache = generateInstrumentationContext();
        strategy.runtimeCache.remove(AGENT_CHECKSUM_PROPERTY_KEY);

        strategy.validateCachedInstrumentationContext();
    }

    @Test(expected = PreprocessCacheException.class)
    public void testValidateCachedInstrumentationContextThrows_whenJavaRunTimeBaseModuleIsMissing() throws Exception {
        strategy.runtimeCache = generateInstrumentationContext();
        strategy.runtimeCache.remove(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY);

        strategy.validateCachedInstrumentationContext();
    }

    @Test
    public void testIsSourceCachedReturnsFalse_whenCacheEmpty() throws PreprocessCacheException {
        assertFalse(strategy.isSourceCached(Paths.get("somePath")));
    }

    @Test
    public void testIsSourceCachedReturnsFalse_whenInputIsNull() throws PreprocessCacheException {
        assertFalse(strategy.isSourceCached(null));
    }

    @Test
    public void testIsSourceCachedReturnsFalse_whenCheckSumNotMatch() throws PreprocessCacheException {
        dummyManifest.setProperty("dummy_jar", "dummy_checksum");
        Mockito.when(strategy.computeChecksum(Mockito.any())).thenReturn("different_dummy_checksum");

        assertFalse(strategy.isSourceCached(Paths.get("dummy_jar")));
    }

    @Test
    public void testIsSourceCachedReturnsTrue_whenCheckSumMatch() throws PreprocessCacheException {
        dummyManifest.setProperty("dummy_jar", "dummy_checksum");
        Mockito.when(strategy.computeChecksum(Mockito.any())).thenReturn("dummy_checksum");

        assertTrue(strategy.isSourceCached(Paths.get("dummy_jar")));
    }

    @Test
    public void testCacheSource() throws PreprocessCacheException {
        Mockito.doCallRealMethod().when(strategy).cacheSource(Mockito.any());
        Mockito.when(strategy.computeChecksum(Mockito.any())).thenReturn("dummy_checksum");

        strategy.cacheSource(Paths.get("dummy_jar"));

        assertTrue(strategy.runtimeCache.containsKey("dummy_jar"));
        assertEquals("dummy_checksum", strategy.runtimeCache.getProperty("dummy_jar"));
    }

    @Test
    public void testComputeCheckSumReturnsNull_whenInputIsNull() throws PreprocessCacheException {
        Mockito.doCallRealMethod().when(strategy).computeChecksum(Mockito.any());

        assertNull(strategy.computeChecksum(null));
    }

    @Test
    public void testComputeCheckSumReturnsNull_whenInputIsInvalidFile() throws PreprocessCacheException {
        Mockito.doCallRealMethod().when(strategy).computeChecksum(Mockito.any());

        File file = new File("dummy_file");

        assertFalse(file.exists());
        assertNull(strategy.computeChecksum(file.toPath()));
    }

    @Test
    public void testComputeCheckSumReturnsNull_whenInputIsDirectory() throws PreprocessCacheException {
        Mockito.doCallRealMethod().when(strategy).computeChecksum(Mockito.any());

        assertNull(strategy.computeChecksum(temporaryFolder.getRoot().toPath()));
    }

    @Test
    public void testComputeCheckSumReturnsValidCheckSum() throws PreprocessCacheException {
        Mockito.doCallRealMethod().when(strategy).computeChecksum(Mockito.any());
        assertTrue(fakeAgentJar.exists());

        String checkSum = strategy.computeChecksum(fakeAgentJar.toPath());

        assertNotNull(checkSum);
        assertFalse(checkSum.isEmpty());
    }

    @Test
    public void testComputeCheckSumForPluginsReturnsEmptyString_whenNoPluginsDiscovered() throws PreprocessCacheException, IOException {
        config = PreprocessConfig.builder().agentArg("pluginpath=" + temporaryFolder.newFolder().getAbsolutePath()).build();

        String checkSum = strategy.computeChecksumForPlugins(config);

        assertTrue(checkSum.isEmpty());
    }

    @Test
    public void testComputeChecksumForPluginsReturnsValidChecksum() throws Exception {
        // create 3 fake plugins to be discovered
        File tempDir = temporaryFolder.newFolder();
        List<File> pluginsList = Arrays.asList(
            TestUtils.createDummyPlugin(new File(tempDir, "plugin_a.jar"), "ClassA.class"),
            TestUtils.createDummyPlugin(new File(tempDir, "plugin_b.jar"), "ClassB.class"),
            TestUtils.createDummyPlugin(new File(tempDir, "plugin_c.jar"), "ClassC.class")
        );

        config = PreprocessConfig.builder().agentArg("pluginpath=" + tempDir.getAbsolutePath()).build();

        String checksum = strategy.computeChecksumForPlugins(config);

        assertFalse(checksum.isEmpty());

        // expected return value is a ',' delimited list of checksum entries where each entry represents the checksum of a single plugin in the format of
        // '<absolute_path_to_plugin=checksum>'
        String[] checksumSegments = checksum.split(",");

        assertEquals(3, checksumSegments.length);

        for (int i = 0; i < pluginsList.size(); i++) {
            String[] plugin_aChecksumValuePair = checksumSegments[i].split("=");
            // first segment is the absolute path to the plugin Jar
            assertEquals(pluginsList.get(i).getAbsolutePath(), plugin_aChecksumValuePair[0]);

            // second segment is the checksum of the plugin Jar
            assertEquals(strategy.computeChecksum(pluginsList.get(i).toPath()), plugin_aChecksumValuePair[1]);
        }
    }

    @Test
    public void testSerializeRuntimeCacheToFile() throws IOException, PreprocessCacheException {
        File outputDir = temporaryFolder.newFolder();

        strategy.runtimeCache.setProperty("some_key", "some_value");
        strategy.runtimeCache.setProperty("some_other_key", "some_other_value");
        Mockito.doReturn(generateInstrumentationContext()).when(strategy).computeCurrentInstrumentationContext(Mockito.any());

        strategy.serializeRuntimeCacheToTempManifestFile(PreprocessConfig.builder().outputDir(outputDir.getAbsolutePath()).build());

        File[] manifests = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME).listFiles();
        assertEquals(1, manifests.length);

        Properties properties = strategy.readPropertiesFromFile(manifests[0]);
        assertEquals("some_value", properties.getProperty("some_key"));
        assertEquals("some_other_value", properties.getProperty("some_other_key"));
    }

    @Test
    public void testSerializeRuntimeCacheToFileSkips_whenRuntimeCacheEmpty() throws IOException, PreprocessCacheException {
        dummyManifest.clear();
        File outputDir = temporaryFolder.newFolder();

        strategy.serializeRuntimeCacheToTempManifestFile(PreprocessConfig.builder().outputDir(outputDir.getAbsolutePath()).build());

        File[] manifests = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME).listFiles();
        assertNull(manifests);
    }

    @Test
    public void testMergeTempCacheManifests() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = new Properties();
        prop_a.setProperty(AGENT_CHECKSUM_PROPERTY_KEY, AGENT_CHECKSUM_PROPERTY_VAL);
        prop_a.setProperty(PLUGINS_CHECKSUM_PROPERTY_KEY, PLUGINS_CHECKSUM_PROPERTY_VAL);
        prop_a.setProperty(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, PREPROCESSOR_CHECKSUM_PROPERTY_VAL);
        prop_a.setProperty(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL);
        prop_a.setProperty(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL);
        prop_a.setProperty("jar_a.jar", "dummy_hash_jar_a");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        Properties prop_b = new Properties();
        prop_b.setProperty(AGENT_CHECKSUM_PROPERTY_KEY, AGENT_CHECKSUM_PROPERTY_VAL);
        prop_b.setProperty(PLUGINS_CHECKSUM_PROPERTY_KEY, PLUGINS_CHECKSUM_PROPERTY_VAL);
        prop_b.setProperty(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, PREPROCESSOR_CHECKSUM_PROPERTY_VAL);
        prop_b.setProperty(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL);
        prop_b.setProperty(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL);
        prop_b.setProperty("jar_b.jar", "dummy_hash_jar_b");
        serializePropertiesFile(new File(manifestsTempDir, "2222@host.properties"), prop_b);

        strategy.runtimeCache = generateInstrumentationContext();
        strategy.runtimeCache.setProperty("someJar.jar", "some_hash");

        // the 2 generated temp manifests will be merged into a pre-populated runtime cache
        strategy.mergeTempCacheManifests(outputDir);

        File expectedMergedManifest = new File(outputDir, MANIFEST_FILE_NAME);
        assertTrue(expectedMergedManifest.exists());

        Properties propertiesFromMergedManifest = strategy.readPropertiesFromFile(expectedMergedManifest);
        assertEquals(8, propertiesFromMergedManifest.size());
        assertEquals(AGENT_CHECKSUM_PROPERTY_VAL, propertiesFromMergedManifest.getProperty(AGENT_CHECKSUM_PROPERTY_KEY));
        assertEquals(PLUGINS_CHECKSUM_PROPERTY_VAL, propertiesFromMergedManifest.getProperty(PLUGINS_CHECKSUM_PROPERTY_KEY));
        assertEquals(PREPROCESSOR_CHECKSUM_PROPERTY_VAL, propertiesFromMergedManifest.getProperty(PREPROCESSOR_CHECKSUM_PROPERTY_KEY));
        assertEquals(PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL, propertiesFromMergedManifest.getProperty(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY));
        assertEquals(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL, propertiesFromMergedManifest.getProperty(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY));
        assertEquals("dummy_hash_jar_a", propertiesFromMergedManifest.getProperty("jar_a.jar"));
        assertEquals("dummy_hash_jar_b", propertiesFromMergedManifest.getProperty("jar_b.jar"));
        assertEquals("some_hash", propertiesFromMergedManifest.getProperty("someJar.jar"));
    }

    @Test
    public void testMergeTempCacheManifestsReturns_whenTemporaryManifestDirNotExisting() throws PreprocessCacheException, IOException {
        strategy.mergeTempCacheManifests(temporaryFolder.newFolder());

        Mockito.verify(strategy, Mockito.never()).readPropertiesFromFile(Mockito.any());
    }

    @Test
    public void testMergeTempCacheManifestsReturns_whenNoTemporaryManifestDiscovered() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        strategy.mergeTempCacheManifests(outputDir);

        Mockito.verify(strategy, Mockito.never()).readPropertiesFromFile(Mockito.any());
    }

    @Test(expected = PreprocessCacheException.class)
    public void testMergeTempCacheManifestsThrowsException_whenAgentChecksumNotMatch() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = generateInstrumentationContext();
        prop_a.setProperty(AGENT_CHECKSUM_PROPERTY_KEY, "inconsistent_value");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        strategy.mergeTempCacheManifests(outputDir);
    }

    @Test(expected = PreprocessCacheException.class)
    public void testMergeTempCacheManifestsThrowsException_whenPluginsChecksumNotMatch() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = generateInstrumentationContext();
        prop_a.setProperty(PLUGINS_CHECKSUM_PROPERTY_KEY, "inconsistent_value");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        strategy.mergeTempCacheManifests(outputDir);
    }

    @Test(expected = PreprocessCacheException.class)
    public void testMergeTempCacheManifestsThrowsException_whenPreprocessorChecksumNotMatch() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = generateInstrumentationContext();
        prop_a.setProperty(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, "inconsistent_value");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        strategy.mergeTempCacheManifests(outputDir);
    }

    @Test(expected = PreprocessCacheException.class)
    public void testMergeTempCacheManifestsThrowsException_whenPreprocessorConfigNotMatch() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = generateInstrumentationContext();
        prop_a.setProperty(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, "inconsistent_value");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        strategy.mergeTempCacheManifests(outputDir);
    }

    @Test(expected = PreprocessCacheException.class)
    public void testMergeTempCacheManifestsThrowsException_whenJavaRuntimeBaseNotMatch() throws IOException, PreprocessCacheException {
        File outputDir = new File(temporaryFolder.newFolder(), "static-instrumentation");
        File manifestsTempDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);
        manifestsTempDir.mkdirs();

        Properties prop_a = generateInstrumentationContext();
        prop_a.setProperty(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, "inconsistent_value");
        serializePropertiesFile(new File(manifestsTempDir, "1111@host.properties"), prop_a);

        strategy.mergeTempCacheManifests(outputDir);
    }

    private void serializePropertiesFile(File file, Properties properties) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            // save the runtime cache to the manifest file.
            properties.store(fileOutputStream, null);
        }
    }

    private Properties generateInstrumentationContext() {
        Properties context = new Properties();

        context.put(AGENT_CHECKSUM_PROPERTY_KEY, AGENT_CHECKSUM_PROPERTY_VAL);
        context.put(PLUGINS_CHECKSUM_PROPERTY_KEY, PLUGINS_CHECKSUM_PROPERTY_VAL);
        context.put(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, PREPROCESSOR_CHECKSUM_PROPERTY_VAL);
        context.put(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_VAL);
        context.put(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, PREPROCESSOR_CONFIG_STRING_PROPERTY_VAL);

        return context;
    }

    private void verifyInstrumentationContextInCache(Map cache) {
        expectedInstrumentationContext.forEach((key, val) -> assertEquals(val, cache.get(key)));
    }
}
