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
import org.mockito.Mockito;

import java.util.Arrays;

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

    @Test
    public void parseCommandLineReturnsNullWithNullArgs() {
        Assert.assertNull(preprocessConfigParser.parseCommandLine(null));
    }

    @Test
    public void parseCommandLineReturnsNullWithEmptyArgs() {
        Assert.assertNull(preprocessConfigParser.parseCommandLine(new String[]{}));
    }

    @Test
    public void parseCommandLineReturnsNullWithInvalidFlag() {
        String[] args = new String[]{"--help", "--suff", suffix};
        Assert.assertNull(preprocessConfigParser.parseCommandLine(args));
    }

    @Test
    public void parseCommandLineReturnsNullWithUnmatchedFlagAsLastArg() {
        String[] args = new String[]{"--help", "--suffix"};
        Assert.assertNull(preprocessConfigParser.parseCommandLine(args));
    }

    @Test
    public void parseCommandLineReturnsNullWithHelpFlag() {
        String[] args = new String[]{"--help", "--suffix", suffix};
        Assert.assertNull(preprocessConfigParser.parseCommandLine(args));
    }

    @Test
    public void parseCommandLineReturnsNullWithInvalidFormat() {
        String[] args = new String[]{"--suffix", "--verbose"};
        Assert.assertNull(preprocessConfigParser.parseCommandLine(args));
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
                "--suffix", suffix
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        Assert.assertEquals(outputDir, config.getOutputDir());
        Assert.assertEquals(serialization, config.getSerializationJarPath());
        Assert.assertEquals(Arrays.asList("/d1", "/d2", "/d3"), config.getJarPaths());
        Assert.assertEquals(agent, config.getAgentPath());
        Assert.assertEquals(suffix, config.getSuffix());
    }

    @Test
    public void parseCommandLineWorksWithShortHandCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
                "-out", outputDir,
                "-jps", "/d1", "/d2", "/d3",
                "-sp", serialization,
                "-ap", agent,
                "-suf", suffix
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        Assert.assertEquals(outputDir, config.getOutputDir());
        Assert.assertEquals(serialization, config.getSerializationJarPath());
        Assert.assertEquals(Arrays.asList("/d1", "/d2", "/d3"), config.getJarPaths());
        Assert.assertEquals(agent, config.getAgentPath());
        Assert.assertEquals(suffix, config.getSuffix());
    }

    @Test
    public void parseCommandLineWorkWithHelpFlag() {
        PreprocessConfigParser spyParser = Mockito.spy(preprocessConfigParser);

        spyParser.parseCommandLine(new String[]{"--help"});
        Mockito.verify(spyParser).printHelpText();
        Mockito.clearInvocations(spyParser);

        spyParser.parseCommandLine(new String[]{"--verbose", "--help"});
        Mockito.verify(spyParser, Mockito.never()).printHelpText();
    }
}
