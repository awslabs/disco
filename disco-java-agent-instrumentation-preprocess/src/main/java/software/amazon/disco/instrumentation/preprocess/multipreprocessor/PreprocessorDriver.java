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

package software.amazon.disco.instrumentation.preprocess.multipreprocessor;

import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.Driver;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfigParser;
import software.amazon.disco.instrumentation.preprocess.instrumentation.StaticInstrumentationTransformer;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.DirectoryLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;

/**
 * Entry point of the preprocessing worker program(preprocessor) handling a portion of preprocessing work.
 * It is the entry point of the worker program instead of the whole preprocessing, should only be called internally from {@link MultiPreprocessorScheduler} and not interact with any external input.
 * A {@link StaticInstrumentationTransformer} instance is being created to orchestrate the instrumentation process of all class files supplied.
 */
public class PreprocessorDriver {
    private static final Logger log = LogManager.getLogger(PreprocessorDriver.class);

    public static void main(String[] args) {
        try {
            final PreprocessConfig config = new PreprocessConfigParser().parseCommandLine(args);

            // inject the agent jar into the classpath as earlier as possible to avoid ClassNotFound exception when resolving
            // types imported from libraries such as ByteBuddy shaded in the agent JAR
            Injector.addToBootstrapClasspath(Injector.createInstrumentation(), new File(config.getAgentPath()));

            // set up the preprocessor log
            Driver.configureLog(config.getLogLevel());

            StaticInstrumentationTransformer.builder()
                    .agentLoader(new DiscoAgentLoader())
                    .classFileLoader(JarLoader.class, new JarLoader())
                    .classFileLoader(DirectoryLoader.class, new DirectoryLoader())
                    .config(config)
                    .build()
                    .transform();
        } catch (Exception e) {
            log.fatal(PreprocessConstants.MESSAGE_PREFIX + "Preprocessor aborted", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
