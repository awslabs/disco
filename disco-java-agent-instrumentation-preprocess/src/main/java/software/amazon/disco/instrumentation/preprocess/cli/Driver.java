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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.instrumentation.StaticInstrumentationTransformer;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;

/**
 * Entry point of the library. A {@link StaticInstrumentationTransformer} instance is being created to orchestrate the instrumentation
 * process of all class files supplied.
 */
public class Driver {
    private static final Logger log = LogManager.getLogger(Driver.class);

    public static void main(String[] args) {
        // only print help text if it's the first argument passed in and ignores all other args
        if (args[0].toLowerCase().equals("--help")) {
            printHelpText();
            System.exit(0);
        }

        try {
            final PreprocessConfig config = new PreprocessConfigParser().parseCommandLine(args);

            // inject the agent jar into the classpath as earlier as possible to avoid ClassNotFound exception when resolving
            // types imported from libraries such as ByteBuddy shaded in the agent JAR
            Injector.addToBootstrapClasspath(Injector.createInstrumentation(), new File(config.getAgentPath()));

            StaticInstrumentationTransformer.builder()
                    .agentLoader(new DiscoAgentLoader())
                    .jarLoader(new JarLoader())
                    .config(config)
                    .build()
                    .transform();
        } catch (RuntimeException e) {
            log.error(PreprocessConstants.MESSAGE_PREFIX + "Failed to perform static instrumentation", e);
            System.exit(1);
        }
    }

    /**
     * Prints out the help text when the [--help] option is passed.
     */
    protected static void printHelpText() {
        System.out.println("Disco Instrumentation Preprocess Library Command Line Interface\n"
                + "\t Usage: [options] \n"
                + "\t\t --help                          List all supported options supported by the CLI.\n"
                + "\t\t --outputDir | -out              <Output directory where the transformed packages will be stored. Same folder as the original file if not provided>\n"
                + "\t\t --jarPaths | -jps               <List of paths to the jar files to be instrumented>\n"
                + "\t\t --serializationPath | -sp       <Path to the jar where the serialized instrumentation state will be stored>\n"
                + "\t\t --agentPath | -ap               <Path to the Disco Agent that will be applied to the packages supplied>\n"
                + "\t\t --agentArg | -arg               <Arguments that will be passed to the agent>\n"
                + "\t\t --suffix | -suf                 <Suffix to be appended to the transformed packages>\n"
                + "\t\t --javaversion | -jv             <Version of java to compile the transformed classes>\n"
                + "\t\t --verbose                       Set the log level to log everything.\n"
                + "\t\t --silent                        Disable logging to the console.\n\n"
                + "The default behavior of the library will replace the original Jar scheduled for instrumentation if NO outputDir AND suffix are supplied.\n"
                + "One agentPath and at least one jarPaths must be provided in order to perform static instrumentation"
        );
    }
}
