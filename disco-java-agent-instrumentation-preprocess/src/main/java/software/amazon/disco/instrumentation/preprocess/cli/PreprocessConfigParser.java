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

import java.util.HashMap;
import java.util.Map;

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

        OptionToMatch flagBeingMatched = null;

        for (String arg : args) {
            final String argLowered = arg.toLowerCase();

            if (flagBeingMatched == null) {
                // no previous flag found, expecting a flag

                if (!ACCEPTED_FLAGS.containsKey(argLowered)) {
                    throw new ArgumentParserException("Flag: [" + arg + "] is invalid");
                }

                final OptionToMatch option = ACCEPTED_FLAGS.get(argLowered);
                if (option.hasArgument) {
                    flagBeingMatched = ACCEPTED_FLAGS.get(argLowered);
                } else {
                    processFlagWithNoArg(argLowered, builder);
                }
            } else {
                // previous flag still expecting an argument but another flag is discovered
                if (ACCEPTED_FLAGS.containsKey(argLowered) && !flagBeingMatched.isMatched()) {
                    throw new ArgumentParserException("Flag: [" + flagBeingMatched.getFlag() + "] requires an argument");
                }

                // a previously detected option that accepts multi values is now finished matching its arguments.
                // and a new option is now being matched
                if (ACCEPTED_FLAGS.containsKey(argLowered) && flagBeingMatched.isMatched()) {
                    final OptionToMatch option = ACCEPTED_FLAGS.get(argLowered);

                    if (option.hasArgument) {
                        flagBeingMatched = option;
                    } else {
                        processFlagWithNoArg(argLowered, builder);
                    }
                    continue;
                }

                if (flagBeingMatched.hasArgument) {
                    flagBeingMatched = matchArgWithFlag(flagBeingMatched, arg, builder);
                }
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
        ACCEPTED_FLAGS.put("--help", new OptionToMatch("--help", false, false));
        ACCEPTED_FLAGS.put("--verbose", new OptionToMatch("--verbose", false, false));
        ACCEPTED_FLAGS.put("--silent", new OptionToMatch("--silent", false, false));

        ACCEPTED_FLAGS.put("--outputdir", new OptionToMatch("--outputdir", true, false));
        ACCEPTED_FLAGS.put("--jarpaths", new OptionToMatch("--jarpaths", true, true));
        ACCEPTED_FLAGS.put("--agentpath", new OptionToMatch("--agentpath", true, false));
        ACCEPTED_FLAGS.put("--serializationpath", new OptionToMatch("--serializationpath", true, false));
        ACCEPTED_FLAGS.put("--suffix", new OptionToMatch("--suffix", true, false));
        ACCEPTED_FLAGS.put("--javaversion", new OptionToMatch("--javaversion", true, false));
        ACCEPTED_FLAGS.put("--agentarg", new OptionToMatch("--agentarg", true, false));

        ACCEPTED_FLAGS.put("-out", new OptionToMatch("-out", true, false));
        ACCEPTED_FLAGS.put("-jps", new OptionToMatch("-jps", true, true));
        ACCEPTED_FLAGS.put("-ap", new OptionToMatch("-ap", true, false));
        ACCEPTED_FLAGS.put("-sp", new OptionToMatch("-sp", true, false));
        ACCEPTED_FLAGS.put("-suf", new OptionToMatch("-suf", true, false));
        ACCEPTED_FLAGS.put("-jv", new OptionToMatch("-jv", true, false));
        ACCEPTED_FLAGS.put("-arg", new OptionToMatch("-arg", true, false));
    }

    /**
     * Matches the argument to the previously discovered flag. eg: [-out <arg to match>].
     *
     * @param option   a valid option to be matched with an argument
     * @param argument argument to be matched to the flag
     * @param builder  {@link PreprocessConfig.PreprocessConfigBuilder builder} to build the {@link PreprocessConfig}
     * @return the same {@link OptionToMatch} instance if the option accepts multiple values, null if option only accepts one value.
     */
    protected OptionToMatch matchArgWithFlag(OptionToMatch option, String argument, PreprocessConfig.PreprocessConfigBuilder builder) {
        OptionToMatch result = null;

        switch (option.getFlag().toLowerCase()) {
            case "-out":
            case "--outputdir":
                builder.outputDir(argument);
                break;
            case "--serializationpath":
            case "-sp":
                builder.serializationJarPath(argument);
                break;
            case "-jps":
            case "--jarpaths":
                builder.jarPath(argument);
                option.isMatched = true;
                result = option;
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
            default:
                // will never be invoked since flags are already validated.
        }

        return result;
    }

    /**
     * Matches the argument to the previously discovered flag. eg: [-out <arg to match>].
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
                builder.logLevel(Level.TRACE);
                break;
            case "--silent":
                builder.logLevel(Level.OFF);
                break;
            default:
                // will never be invoked since flags are already validated.
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
        final boolean acceptsMultiValues;

        boolean isMatched;
    }
}
