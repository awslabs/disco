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

import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.instrumentation.ModuleTransformer;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.DiscoAgentLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.JarModuleLoader;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * Entry point of the library. A {@link ModuleTransformer} instance is being created to orchestrate the instrumentation
 * process of all packages supplied.
 */
public class Driver {
    public static void main(String[] args) {
        final PreprocessConfig config = new PreprocessConfigParser().parseCommandLine(args);

        if (config == null) {
            System.exit(1);
        }

        final Instrumentation instrumentation = Injector.createInstrumentation();

        // inject the agent jar into the classpath as earlier as possible to avoid ClassNotFound exception when resolving
        // types imported from libraries such as ByteBuddy shaded in the agent JAR
        Injector.addToBootstrapClasspath(instrumentation, new File(config.getAgentPath()));

        ModuleTransformer.builder()
                .agentLoader(new DiscoAgentLoader(config.getAgentPath(), config.getCoreAgentConfig()))
                .jarLoader(new JarModuleLoader(config.getJarPaths()))
                .suffix(config.getSuffix())
                .logLevel(config.getLogLevel())
                .build()
                .transform();
    }
}
