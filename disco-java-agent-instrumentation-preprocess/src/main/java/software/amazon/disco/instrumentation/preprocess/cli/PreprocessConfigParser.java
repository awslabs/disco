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
import software.amazon.disco.agent.config.AgentConfig;

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
            System.err.println("Mandatory options not supplied, please use [--help] to get a list of all options supported by this CLI.");
            return null;
        }

        // only print help text if it's the first argument passed in and ignores all other args
        if (args[0].toLowerCase().equals("--help")) {
            printHelpText();
            return null;
        }

        setupAcceptedFlags();
        PreprocessConfig.PreprocessConfigBuilder builder = PreprocessConfig.builder().coreAgentConfig(new AgentConfig(null));

        OptionToMatch flagBeingMatched = null;

        for (String arg : args) {
            final String argLowered = arg.toLowerCase();

            if (flagBeingMatched == null) {
                // no previous flag found, expecting a flag

                if (!ACCEPTED_FLAGS.containsKey(argLowered)) {
                    System.err.println("Flag: [" + arg + "] is invalid");
                    return null;
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
                    System.err.println("Flag: [" + flagBeingMatched + "] requires an argument");
                    return null;
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
        if (flagBeingMatched != null) {
            System.err.println("Flag: [" + flagBeingMatched + "] requires an argument");
            return null;
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

        ACCEPTED_FLAGS.put("-out", new OptionToMatch("-out", true, false));
        ACCEPTED_FLAGS.put("-jps", new OptionToMatch("-jps", true, true));
        ACCEPTED_FLAGS.put("-ap", new OptionToMatch("-ap", true, false));
        ACCEPTED_FLAGS.put("-sp", new OptionToMatch("-sp", true, false));
        ACCEPTED_FLAGS.put("-suf", new OptionToMatch("-suf", true, false));
    }

    /**
     * Prints out the help text when the [--help] option is passed.
     */
    protected void printHelpText() {
        System.out.println("Disco Instrumentation Preprocess Library Command Line Interface\n"
                + "\t Usage: [options] \n"
                + "\t\t --help                          List all supported options supported by the CLI.\n"
                + "\t\t --outputDir | -out              <Output directory where the transformed packages will be stored. Same folder as the original file if not provided>\n"
                + "\t\t --jarPaths | -jps               <List of paths to the jar files to be instrumented>\n"
                + "\t\t --serializationPath | -sp       <Path to the jar where the serialized instrumentation state will be stored>\n"
                + "\t\t --agentPath | -ap               <Path to the Disco Agent that will be applied to the packages supplied>\n"
                + "\t\t --suffix | -suf                 <Suffix to be appended to the transformed packages>\n"
                + "\t\t --verbose                       Set the log level to log everything.\n"
                + "\t\t --silent                        Disable logging to the console.\n\n"
                + "The default behavior of the library will replace the original package scheduled for instrumentation if NO destination AND suffix are supplied.\n"
                + "An agent AND either a servicePackage or at least one dependenciesPath MUST be supplied."
        );
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
            case "-suf":
            case "--suffix":
                builder.suffix(argument);
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
