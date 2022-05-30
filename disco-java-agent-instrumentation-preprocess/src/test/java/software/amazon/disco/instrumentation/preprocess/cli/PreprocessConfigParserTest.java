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

package software.amazon.disco.instrumentation.preprocess.cli;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.exceptions.ArgumentParserException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SkipSignedJarHandlingStrategy;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreprocessConfigParserTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    String outputDir = "/d";
    String serialization = "/s";
    String agent = "/agent_path";
    String jdkpath = "/java.base.jmod";
    String suffix = "-suffix";

    static PreprocessConfigParser preprocessConfigParser;

    @Before
    public void before() {
        preprocessConfigParser = new PreprocessConfigParser();
    }

    @Test(expected = ArgumentParserException.class)
    public void parseCommandLineReturnsNullWithNullArgs() {
        preprocessConfigParser.parseCommandLine(null);
    }

    @Test(expected = ArgumentParserException.class)
    public void parseCommandLineFailsWithEmptyArgs() {
        preprocessConfigParser.parseCommandLine(new String[]{});
    }

    @Test(expected = ArgumentParserException.class)
    public void parseCommandLineFailsWithInvalidFlag() {
        String[] args = new String[]{"--suff", suffix};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test(expected = ArgumentParserException.class)
    public void parseCommandLineFailsWithUnmatchedFlagAsLastArg() {
        String[] args = new String[]{"--suffix"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test(expected = ArgumentParserException.class)
    public void parseCommandLineFailsWithInvalidFormat() {
        String[] args = new String[]{"--suffix", "--verbose"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test
    public void parseCommandLineWorksWithDifferentLogLevels() {
        PreprocessConfig silentConfig = preprocessConfigParser.parseCommandLine(new String[]{"--silent"});
        PreprocessConfig verboseConfig = preprocessConfigParser.parseCommandLine(new String[]{"--verbose"});
        PreprocessConfig extraverboseConfig = preprocessConfigParser.parseCommandLine(new String[]{"--extraverbose"});

        assertEquals(Logger.Level.FATAL, silentConfig.getLogLevel());
        assertEquals(Logger.Level.DEBUG, verboseConfig.getLogLevel());
        assertEquals(Logger.Level.TRACE, extraverboseConfig.getLogLevel());
    }

    @Test
    public void parseCommandLineWorksAndReturnsConfigWithDefaultValue() {
        String[] args = new String[]{
            "--sourcepaths", "/d1:/d2:/d3",
            "--agentPath", agent
        };
        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertFalse(config.isFailOnUnresolvableDependency());
        assertEquals(Logger.Level.INFO, config.getLogLevel());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getSourcePaths().get(""));
        assertTrue(config.getSignedJarHandlingStrategy() instanceof InstrumentSignedJarHandlingStrategy);
    }

    @Test
    public void parseCommandLineWorksWithFullCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
            "--outputDir", outputDir,
            "--sourcepaths", "/d1:/d2:/d3@lib",
            "--serializationpath", serialization,
            "--agentPath", agent,
            "--suffix", suffix,
            "--javaversion", "11",
            "--agentarg", "arg",
            "--jdksupport", jdkpath,
            "--failonunresolvabledependency",
            "--signedjarhandlingstrategy", "skip"
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(outputDir, config.getOutputDir());
        assertEquals(serialization, config.getSerializationJarPath());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getSourcePaths().get("lib"));
        assertEquals(agent, config.getAgentPath());
        assertEquals(suffix, config.getSuffix());
        assertEquals("11", config.getJavaVersion());
        assertEquals("arg", config.getAgentArg());
        assertEquals(jdkpath, config.getJdkPath());
        assertTrue(config.isFailOnUnresolvableDependency());
        assertTrue(config.getSignedJarHandlingStrategy() instanceof SkipSignedJarHandlingStrategy);
    }

    @Test
    public void testParseCommandLineWorksWithShortHandCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
            "-out", outputDir,
            "-sps", "/d1:/d2:/D3@lib",
            "-sp", serialization,
            "-ap", agent,
            "-suf", suffix,
            "-jv", "11",
            "-arg", "arg",
            "-jdks", jdkpath
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(outputDir, config.getOutputDir());
        assertEquals(serialization, config.getSerializationJarPath());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/D3")), config.getSourcePaths().get("lib"));
        assertEquals(agent, config.getAgentPath());
        assertEquals(suffix, config.getSuffix());
        assertEquals("11", config.getJavaVersion());
        assertEquals("arg", config.getAgentArg());
        assertEquals(jdkpath, config.getJdkPath());
    }

    @Test
    public void testParseCommandLineWorksWithPathToResponseFile() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{
            "@" + responseFile.getAbsolutePath()
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);
        assertEquals(outputDir, config.getOutputDir());
        assertEquals(agent, config.getAgentPath());
    }

    @Test
    public void testParseCommandLineAppendsResponseFileArgsWithCommandLineArgs() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{"@" + responseFile.getAbsolutePath(), "-sps", "/d1:/d1:/d2", "-out", "new_value"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(agent, config.getAgentPath());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2")), config.getSourcePaths().get(""));

        // value from response file should be overridden by value from command line arg
        assertEquals("new_value", config.getOutputDir());
    }

    @Test
    public void testParseCommandLineJoinsResponseFileSourcePathsWithCommandLineSourcePaths() throws Exception {
        String fileContent = "-sps /d3:/d4:/d5 -sps /d6@lib";
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{"@" + responseFile.getAbsolutePath(), "-sps", "/d1:/d1:/d2"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3", "/d4", "/d5")), config.getSourcePaths().get(""));
        assertEquals(new HashSet<>(Arrays.asList("/d6")), config.getSourcePaths().get("lib"));
    }

    @Test
    public void testParseCommandLineWorksWithDuplicatePaths() {
        String[] args = new String[]{"-sps", "/d1:/d1:/d2@lib",};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(1, config.getSourcePaths().size());
        assertEquals(2, config.getSourcePaths().get("lib").size());
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testParseCommandLineFailsWithInvalidSourcePathOption(){
        String[] args = new String[]{"-sps", "/d1:/d1:/d2@lib@tomcat",};

        preprocessConfigParser.parseCommandLine(args);
    }

    @Test
    public void testReadOptionsFromFileWorksWithSingleWhiteSpace() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        List<String> args = preprocessConfigParser.readArgsFromFile(responseFile.getAbsolutePath());

        assertEquals(4, args.size());
        assertEquals("-out", args.get(0));
        assertEquals(outputDir, args.get(1));
        assertEquals("-ap", args.get(2));
        assertEquals(agent, args.get(3));
    }

    @Test
    public void testReadOptionsFromFileWorksWithMultipleWhiteSpaceAndTab() throws Exception {
        String fileContent = "   -out   " + outputDir + " -ap\t" + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        List<String> args = preprocessConfigParser.readArgsFromFile(responseFile.getAbsolutePath());

        assertEquals(4, args.size());
        assertEquals("-out", args.get(0));
        assertEquals(outputDir, args.get(1));
        assertEquals("-ap", args.get(2));
        assertEquals(agent, args.get(3));
    }

    @Test(expected = ArgumentParserException.class)
    public void testReadOptionsFromFileFailsWhenPathToResponseFileIsNonExistent() {
        preprocessConfigParser.readArgsFromFile("path_to_file");
    }

    @Test(expected = ArgumentParserException.class)
    public void testReadOptionsFromFileFailsWhenPathToResponseFileIsADirectory() {
        preprocessConfigParser.readArgsFromFile(tempFolder.getRoot().getAbsolutePath());
    }
}
