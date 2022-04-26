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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import software.amazon.disco.instrumentation.preprocess.exceptions.ArgumentParserException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses command line arguments supplied to the preprocess tool.
 */
public class PreprocessConfigParser {
    private static final Map<String, OptionToMatch> ACCEPTED_FLAGS = new HashMap();

    /**
     * Parses the command line arguments supplied to the library.
     * Transformed package will override original file if NO destination And (prefix OR suffix) are supplied.
     *
     * @param args arguments passed to be parsed
     * @return an instance of {@link PreprocessConfig}, null if format of the args is invalid
     */
    public PreprocessConfig parseCommandLine(String[] args) {
        if (args == null || args.length == 0) {
            throw new ArgumentParserException("Mandatory options not supplied, please use [--help] to get a list of all options supported by this CLI.");
        }

        setupAcceptedFlags();
        PreprocessConfig.PreprocessConfigBuilder builder = PreprocessConfig.builder();

        final LinkedList<String> argsToParse = new LinkedList(Arrays.asList(args));
        OptionToMatch flagBeingMatched = null;

        while (!argsToParse.isEmpty()) {
            final String arg = argsToParse.pop().trim();
            final String argLowered = arg.toLowerCase();

            if (argLowered.startsWith("@")) {
                // prepends args retrieved from response file in the beginning of the queue to respect order
                argsToParse.addAll(0, readArgsFromFile(arg.substring(1)));
                continue;
            }

            if (flagBeingMatched == null) {
                // no previous flag found or previous flag already matched, expecting a new flag
                final OptionToMatch option = ACCEPTED_FLAGS.get(argLowered);

                if (option == null) {
                    throw new ArgumentParserException("Flag: [" + arg + "] is invalid");
                }

                if (option.hasArgument) {
                    // parser is now expecting an argument in the next iteration
                    flagBeingMatched = option;
                } else {
                    processFlagWithNoArg(argLowered, builder);
                }
            } else {
                // previous flag still expecting an argument but another flag is discovered
                if (ACCEPTED_FLAGS.containsKey(argLowered)) {
                    throw new ArgumentParserException("Flag: [" + flagBeingMatched.getFlag() + "] requires an argument");
                }

                matchArgWithFlag(flagBeingMatched, arg, builder);
                flagBeingMatched = null;
            }
        }

        // the last flag discovered is missing its arg
        if (flagBeingMatched != null && !flagBeingMatched.isMatched) {
            throw new ArgumentParserException("Flag: [" + flagBeingMatched.getFlag() + "] requires an argument");
        }

        return builder.build();
    }

    /**
     * Setups the map that contains all the accepted options of this CLI.
     */
    protected void setupAcceptedFlags() {
        ACCEPTED_FLAGS.put("--help", new OptionToMatch("--help", false));
        ACCEPTED_FLAGS.put("--verbose", new OptionToMatch("--verbose", false));
        ACCEPTED_FLAGS.put("--extraverbose", new OptionToMatch("--extraverbose", false));
        ACCEPTED_FLAGS.put("--silent", new OptionToMatch("--silent", false));
        ACCEPTED_FLAGS.put("--failonunresolvabledependency", new OptionToMatch("--failonunresolvabledependency", false));

        ACCEPTED_FLAGS.put("--outputdir", new OptionToMatch("--outputdir", true));
        ACCEPTED_FLAGS.put("--sourcepaths", new OptionToMatch("--sourcepaths", true));
        ACCEPTED_FLAGS.put("--agentpath", new OptionToMatch("--agentpath", true));
        ACCEPTED_FLAGS.put("--serializationpath", new OptionToMatch("--serializationpath", true));
        ACCEPTED_FLAGS.put("--suffix", new OptionToMatch("--suffix", true));
        ACCEPTED_FLAGS.put("--javaversion", new OptionToMatch("--javaversion", true));
        ACCEPTED_FLAGS.put("--agentarg", new OptionToMatch("--agentarg", true));
        ACCEPTED_FLAGS.put("--jdksupport", new OptionToMatch("--jdksupport", true));

        ACCEPTED_FLAGS.put("-out", new OptionToMatch("-out", true));
        ACCEPTED_FLAGS.put("-sps", new OptionToMatch("-sps", true));
        ACCEPTED_FLAGS.put("-ap", new OptionToMatch("-ap", true));
        ACCEPTED_FLAGS.put("-sp", new OptionToMatch("-sp", true));
        ACCEPTED_FLAGS.put("-suf", new OptionToMatch("-suf", true));
        ACCEPTED_FLAGS.put("-jv", new OptionToMatch("-jv", true));
        ACCEPTED_FLAGS.put("-arg", new OptionToMatch("-arg", true));
        ACCEPTED_FLAGS.put("-jdks", new OptionToMatch("-jdks", true));
    }

    /**
     * Matches the argument to the previously discovered flag. eg: -out 'arg to match'.
     *
     * @param option   a valid option to be matched with an argument
     * @param argument argument to be matched to the flag
     * @param builder  {@link PreprocessConfig.PreprocessConfigBuilder builder} to build the {@link PreprocessConfig}
     */
    protected void matchArgWithFlag(OptionToMatch option, String argument, PreprocessConfig.PreprocessConfigBuilder builder) {
        switch (option.getFlag().toLowerCase()) {
            case "-out":
            case "--outputdir":
                builder.outputDir(argument);
                break;
            case "--serializationpath":
            case "-sp":
                builder.serializationJarPath(argument);
                break;
            case "-sps":
            case "--sourcepaths":
                final String[] segments = argument.split("@");

                if (segments.length > 2) {
                    throw new InvalidConfigEntryException("Invalid value provided for sourcePaths");
                }

                addToSourceMap(builder, segments.length == 1 ? "" : segments[1], segments[0].split(":"));
                break;
            case "-ap":
            case "--agentpath":
                builder.agentPath(argument);
                break;
            case "-arg":
            case "--agentarg":
                builder.agentArg(argument);
                break;
            case "-suf":
            case "--suffix":
                builder.suffix(argument);
                break;
            case "-jv":
            case "--javaversion":
                builder.javaVersion(argument);
                break;
            case "-jdks":
            case "--jdksupport":
                builder.jdkPath(argument);
                break;
            default:
                // will never be invoked since flags are already validated.
        }

        option.isMatched = true;
    }

    /**
     * Matches the argument to the previously discovered flag. eg: -out 'arg to match'.
     *
     * @param flag    a valid previously discovered flag
     * @param builder {@link PreprocessConfig.PreprocessConfigBuilder builder} to build the {@link PreprocessConfig}
     */
    protected void processFlagWithNoArg(String flag, PreprocessConfig.PreprocessConfigBuilder builder) {
        switch (flag.toLowerCase()) {
            case "--help":
                // ignore this flag since its not supplied as the first arg.
                break;
            case "--verbose":
                builder.logLevel(Level.DEBUG);
                break;
            case "--extraverbose":
                builder.logLevel(Level.ALL);
                break;
            case "--silent":
                builder.logLevel(Level.OFF);
                break;
            case "--failonunresolvabledependency":
                builder.failOnUnresolvableDependency(true);
                break;
            default:
                // will never be invoked since flags are already validated.
        }
    }

    /**
     * Extract the string content from a text file.
     *
     * @param filePath path to the text file
     * @return an array of string args
     */
    protected List<String> readArgsFromFile(final String filePath) {
        final File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) {
            throw new ArgumentParserException("Invalid response file: " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            return Arrays.asList(sb.toString().trim().split("\\s+"));
        } catch (Throwable t) {
            throw new ArgumentParserException("Failed to read options from response file: " + filePath);
        }
    }

    /**
     * Helper method to append sources to be processed to the existing map entry if exists.
     *
     * @param builder PreprocessConfigBuilder instance to be used to create the final config
     * @param key     relative path where the transformed source file(s) will be stored
     * @param sources sources to be processed
     */
    private void addToSourceMap(final PreprocessConfig.PreprocessConfigBuilder builder, final String key, final String[] sources) {
        // Needs to build the config file in order to retrieve the corresponding map entry due to the lack of getters for builder
        final Map<String, Set<String>> existingSources = builder.build().getSourcePaths();

        if (existingSources.containsKey(key)) {
            existingSources.get(key).addAll(Arrays.asList(sources));
        } else {
            builder.sourcePath(key, new HashSet<>(Arrays.asList(sources)));
        }
    }

    /**
     * A struct that describes a valid option.
     */
    @AllArgsConstructor()
    @RequiredArgsConstructor()
    @Getter
    class OptionToMatch {
        final String flag;
        final boolean hasArgument;

        boolean isMatched;
    }
}
