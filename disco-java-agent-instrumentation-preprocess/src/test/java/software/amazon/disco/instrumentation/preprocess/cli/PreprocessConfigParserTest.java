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

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.exceptions.ArgumentParserException;

import java.util.Arrays;
import java.util.HashSet;

public class PreprocessConfigParserTest {
    String outputDir = "/d";
    String serialization = "/s";
    String agent = "/a";
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
        PreprocessConfig config = preprocessConfigParser.parseCommandLine(new String[]{"--verbose"});
        PreprocessConfig silentConfig = preprocessConfigParser.parseCommandLine(new String[]{"--silent"});

        Assert.assertEquals(Level.TRACE, config.getLogLevel());
        Assert.assertEquals(Level.OFF, silentConfig.getLogLevel());
    }

    @Test
    public void parseCommandLineWorksWithFullCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
                "--outputDir", outputDir,
                "--jarpaths", "/d1", "/d2", "/d3",
                "--serializationpath", serialization,
                "--agentPath", agent,
                "--suffix", suffix,
                "--javaversion", "11",
                "--agentarg","arg"
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        Assert.assertEquals(outputDir, config.getOutputDir());
        Assert.assertEquals(serialization, config.getSerializationJarPath());
        Assert.assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getJarPaths());
        Assert.assertEquals(agent, config.getAgentPath());
        Assert.assertEquals(suffix, config.getSuffix());
        Assert.assertEquals("11", config.getJavaVersion());
        Assert.assertEquals("arg", config.getAgentArg());
    }

    @Test
    public void testParseCommandLineWorksWithShortHandCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
                "-out", outputDir,
                "-jps", "/d1", "/d2", "/d3",
                "-sp", serialization,
                "-ap", agent,
                "-suf", suffix,
                "-jv", "11",
                "-arg","arg"
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        Assert.assertEquals(outputDir, config.getOutputDir());
        Assert.assertEquals(serialization, config.getSerializationJarPath());
        Assert.assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getJarPaths());
        Assert.assertEquals(agent, config.getAgentPath());
        Assert.assertEquals(suffix, config.getSuffix());
        Assert.assertEquals("11", config.getJavaVersion());
        Assert.assertEquals("arg", config.getAgentArg());
    }

    @Test
    public void testParseCommandLineWithDuplicatePaths(){
        String[] args = new String[]{
                "-jps", "/d1", "/d1", "/d2",
        };
        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);
        Assert.assertEquals(2, config.getJarPaths().size());
    }
}
