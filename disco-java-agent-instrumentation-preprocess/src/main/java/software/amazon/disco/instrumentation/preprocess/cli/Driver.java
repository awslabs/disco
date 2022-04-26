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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.instrumentation.StaticInstrumentationTransformer;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.DirectoryLoader;
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
        if (args.length == 0 || args[0].toLowerCase().equals("--help")) {
            printHelpText();
            System.exit(0);
        }

        try {
            final PreprocessConfig config = new PreprocessConfigParser().parseCommandLine(args);

            // inject the agent jar into the classpath as earlier as possible to avoid ClassNotFound exception when resolving
            // types imported from libraries such as ByteBuddy shaded in the agent JAR
            Injector.addToBootstrapClasspath(Injector.createInstrumentation(), new File(config.getAgentPath()));

            configureLogLevel(config.getLogLevel());

            StaticInstrumentationTransformer.builder()
                .agentLoader(new DiscoAgentLoader())
                .classFileLoader(JarLoader.class, new JarLoader())
                .classFileLoader(DirectoryLoader.class, new DirectoryLoader())
                .config(config)
                .build()
                .transform();
        } catch (Throwable e) {
            log.fatal(PreprocessConstants.MESSAGE_PREFIX + "Build-Time Instrumentation aborted", e);
            System.exit(1);
        }
    }

    /**
     * Prints out the help text when the [--help] option is passed.
     */
    protected static void printHelpText() {
        System.out.println("Disco Instrumentation Preprocess Library Command Line Interface\n"
            + "\t Usage: [options] \n"
            + "\t\t --help                              List all supported options supported by the CLI.\n"
            + "\t\t @                                   <Path to a response file containing args to be supplied to the Preprocess tool. Cannot be used in combination with other args>\n"
            + "\t\t --outputDir | -out                  <Root output directory where the transformed packages will be stored.>\n"
            + "\t\t --sourcePaths | -sps                <List of paths to be instrumented delimited by ':'. An optional relative output path can be specified by appending '@' followed by the path. E.g. SomeJar.jar:AnotherJar.jar@lib>\n"
            + "\t\t --serializationPath | -sp           <Path to the jar where the serialized instrumentation state will be stored>\n"
            + "\t\t --agentPath | -ap                   <Path to the Disco Agent that will be applied to the packages supplied>\n"
            + "\t\t --agentArg | -arg                   <Arguments that will be passed to the agent>\n"
            + "\t\t --suffix | -suf                     <Suffix to be appended to the transformed packages>\n"
            + "\t\t --javaVersion | -jv                 <Version of java to compile the transformed classes>\n"
            + "\t\t --jdkSupport | -jdks                <Path to the JDK runtime to be instrumented. rt.jar for JDK 8, java.base.jmod for JDK 9 and higher>\n"
            + "\t\t --failOnUnresolvableDependency      Abort the Static Instrumentation process completely if flag is present, log exception as warning otherwise\n"
            + "\t\t --verbose                           Set the log level to log everything.\n"
            + "\t\t --silent                            Disable logging to the console.\n\n"
            + "The default behavior of the library will replace the original Jar (non-jdk) scheduled for instrumentation if NO outputDir AND suffix are supplied.\n"
            + "To avoid polluting the jdk lib directories, the output file will be stored under the directory where the tool is executed if no output dir is specified\n"
        );
    }

    /**
     * Sets the log level of the Preprocessor
     *
     * @param level target log level
     */
    public static void configureLogLevel(final Level level) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(Driver.class.getClassLoader(), false);

        context.getConfiguration()
            .getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
            .setLevel(level);

        context.updateLoggers();
    }
}
