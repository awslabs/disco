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

package software.amazon.disco.instrumentation.preprocess.cli;

import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SkipSignedJarHandlingStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreprocessConfigTest {
    private PreprocessConfig.PreprocessConfigBuilder preprocessConfigBuilder;
    private static final Map<String, Set<String>> sourcePaths = new LinkedHashMap<String, Set<String>>() {{
        put("lib1", new LinkedHashSet<>(Arrays.asList("/d1", "/d2", "/d3", "/d4", "/d5")));
        put("lib2", new LinkedHashSet<>(Arrays.asList("/d11", "/d22", "/d33")));
        put("lib3", new LinkedHashSet<>(Arrays.asList("/d111")));
    }};
    private static final SignedJarHandlingStrategy skipSignedJarHandlingStrategy = new SkipSignedJarHandlingStrategy();
    private static final SignedJarHandlingStrategy instrumentSignedJarHandlingStrategy = new InstrumentSignedJarHandlingStrategy();
    private static final String OUTPUT_DIR = "/outputDir";
    private static final String AGENT_PATH = "/agentpath";
    private static final String SUFFIX = "suffix";
    private static final String SERIALIZATION_JAR_PATH = "/sp";
    private static final String JAVA_VERSION = "11";
    private static final String AGENT_ARG = "arg";
    private static final boolean FAIL_ON_UNRESOLVABLE_DEPENDENCY = true;

    @Before
    public void before() {
        preprocessConfigBuilder = PreprocessConfig.builder();
    }

    @Test
    public void testToCommandlineArguments_returnsNoFlagsForUnsetOptions() {
        PreprocessConfig config = preprocessConfigBuilder
                .sourcePaths(sourcePaths)
                .outputDir(OUTPUT_DIR)
                .agentPath(AGENT_PATH)
                .suffix(SUFFIX)
                .serializationJarPath(SERIALIZATION_JAR_PATH)
                .javaVersion(JAVA_VERSION)
                .agentArg(AGENT_ARG)
                .build();
        String[] commandlineArguments = config.toCommandlineArguments();

        assertArrayEquals(new String[]{"--sourcepaths", "/d1:/d2:/d3:/d4:/d5@lib1", "--sourcepaths", "/d11:/d22:/d33@lib2", "--sourcepaths", "/d111@lib3", "--outputdir", OUTPUT_DIR
                , "--agentpath", AGENT_PATH, "--suffix", SUFFIX, "--serializationpath", SERIALIZATION_JAR_PATH, "--javaversion", JAVA_VERSION, "--agentarg", AGENT_ARG}, commandlineArguments);

        List<String> commandlineArgumentsList =  new ArrayList<>(Arrays.asList(commandlineArguments));
        List<String> flagsForUnsetOptions = new ArrayList<>(Arrays.asList("--signedjarhandlingstrategy", "--verbose", "--extraverbose", "--silent", "--jdksupport","--failonunresolvabledependency"));
        commandlineArgumentsList.retainAll(flagsForUnsetOptions);

        //generated command-line arguments should not contain flags for unset options
        assertEquals(0, commandlineArgumentsList.size());
    }

    @Test
    public void testToCommandlineArguments_convertSourcesPathsCorrectly() {
        PreprocessConfig config = preprocessConfigBuilder.sourcePaths(sourcePaths).build();
        String[] commandlineArguments = config.toCommandlineArguments();
        assertArrayEquals(new String[]{"--sourcepaths", "/d1:/d2:/d3:/d4:/d5@lib1", "--sourcepaths", "/d11:/d22:/d33@lib2", "--sourcepaths", "/d111@lib3"}, commandlineArguments);
    }

    @Test
    public void testToCommandlineArguments_convertSignedJarHandlingStrategyCorrectly() {
        PreprocessConfig configWithSkipSignedJarHandlingStrategy = preprocessConfigBuilder.signedJarHandlingStrategy(skipSignedJarHandlingStrategy).build();
        PreprocessConfig configWithInstrumentSignedJarHandlingStrategy = preprocessConfigBuilder.signedJarHandlingStrategy(instrumentSignedJarHandlingStrategy).build();

        assertArrayEquals(new String[]{"--signedjarhandlingstrategy", "skip"}, configWithSkipSignedJarHandlingStrategy.toCommandlineArguments());
        assertArrayEquals(new String[]{"--signedjarhandlingstrategy", "instrument"}, configWithInstrumentSignedJarHandlingStrategy.toCommandlineArguments());
    }

    @Test
    public void testToCommandlineArguments_convertDifferentLogLevelCorrectly() {
        PreprocessConfig configWithDebugLogLevel = preprocessConfigBuilder.logLevel(Logger.Level.DEBUG).build();
        PreprocessConfig configWithTraceLogLevel = preprocessConfigBuilder.logLevel(Logger.Level.TRACE).build();
        PreprocessConfig configWithFatalLogLevel = preprocessConfigBuilder.logLevel(Logger.Level.FATAL).build();

        assertArrayEquals(new String[]{"--verbose"}, configWithDebugLogLevel.toCommandlineArguments());
        assertArrayEquals(new String[]{"--extraverbose"}, configWithTraceLogLevel.toCommandlineArguments());
        assertArrayEquals(new String[]{"--silent"}, configWithFatalLogLevel.toCommandlineArguments());
    }

    @Test
    public void testToCommandlineArguments_convertFailOnUnresolvableDependencyCorrectly() {
        PreprocessConfig configWithoutFailOnUnresolvableDependencyConfig = preprocessConfigBuilder.build();
        PreprocessConfig configWithFailOnUnresolvableDependencyConfig = preprocessConfigBuilder.failOnUnresolvableDependency(FAIL_ON_UNRESOLVABLE_DEPENDENCY).build();

        assertFalse(Arrays.asList(configWithoutFailOnUnresolvableDependencyConfig.toCommandlineArguments()).contains("--failonunresolvabledependency"));
        assertTrue(Arrays.asList(configWithFailOnUnresolvableDependencyConfig.toCommandlineArguments()).contains("--failonunresolvabledependency"));
    }
}
