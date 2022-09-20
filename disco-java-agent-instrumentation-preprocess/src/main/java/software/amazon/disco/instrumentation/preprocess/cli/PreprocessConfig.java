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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SkipSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.CacheStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.NoOpCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for the config created from the command line args
 */
@Builder(toBuilder = true)
@Getter
public class PreprocessConfig {
    /**
     * a set of sources to be processed indexed by their corresponding relative path on the deployed environment. For instance, one
     * possible map entry is the paths to 'aws-java-sdk-core' as value and 'lib' as key.
     */
    @Singular
    @Setter
    private Map<String, Set<String>> sourcePaths;
    private SignedJarHandlingStrategy signedJarHandlingStrategy;
    private final String outputDir;
    private final String agentPath;
    private final String suffix;
    private final Logger.Level logLevel;
    private final String javaVersion;
    private final String agentArg;
    private final boolean failOnUnresolvableDependency;

    @Setter
    private String jdkPath;

    @Setter
    private CacheStrategy cacheStrategy;

    /**
     * Getter for the 'logLevel' field
     *
     * @return configured log level, default level is 'INFO' if not set
     */
    public Logger.Level getLogLevel() {
        if (logLevel == null) {
            return Logger.Level.INFO;
        }
        return logLevel;
    }

    /**
     * Getter for 'signedJarHandlingStrategy' field
     *
     * @return configured strategy, default strategy is 'InstrumentSignedJarHandlingStrategy' if not set
     */
    public SignedJarHandlingStrategy getSignedJarHandlingStrategy() {
        if (signedJarHandlingStrategy == null) {
            this.signedJarHandlingStrategy = new InstrumentSignedJarHandlingStrategy();
        }
        return signedJarHandlingStrategy;
    }

    /**
     * Convert config file to command-line arguments
     *
     * @return command-line arguments String array
     */
    public String[] toCommandlineArguments() {
        List<String> commandlineArguments = new ArrayList<>();

        // sourcePaths
        for (final Map.Entry<String, Set<String>> entry : sourcePaths.entrySet()) {
            Set<String> sources = entry.getValue();
            sources.remove("");

            if (!sources.isEmpty()) {
                commandlineArguments.add("--sourcepaths");
                final StringBuilder builder = new StringBuilder();
                builder.append(String.join(":", sources))
                    .append(entry.getKey().isEmpty() ? "" : "@")
                    .append(entry.getKey());
                commandlineArguments.add(builder.toString());
            }
        }

        // signedJarHandlingStrategy
        if (signedJarHandlingStrategy instanceof SkipSignedJarHandlingStrategy) {
            commandlineArguments.add("--signedjarhandlingstrategy");
            commandlineArguments.add("skip");
        }
        if (signedJarHandlingStrategy instanceof InstrumentSignedJarHandlingStrategy) {
            commandlineArguments.add("--signedjarhandlingstrategy");
            commandlineArguments.add("instrument");
        }

        // outputDir
        if (outputDir != null) {
            commandlineArguments.add("--outputdir");
            commandlineArguments.add(outputDir);
        }

        //agentPath
        if (agentPath != null) {
            commandlineArguments.add("--agentpath");
            commandlineArguments.add(agentPath);
        }

        // suffix
        if (suffix != null) {
            commandlineArguments.add("--suffix");
            commandlineArguments.add(suffix);
        }

        // logLevel
        if (logLevel != null) {
            switch (logLevel) {
                case DEBUG:
                    commandlineArguments.add("--verbose");
                    break;
                case TRACE:
                    commandlineArguments.add("--extraverbose");
                    break;
                case FATAL:
                    commandlineArguments.add("--silent");
                    break;
                default:
                    break;
            }
        }

        // javaVersion
        if (javaVersion != null) {
            commandlineArguments.add("--javaversion");
            commandlineArguments.add(javaVersion);
        }

        // agentArg
        if (agentArg != null) {
            commandlineArguments.add("--agentarg");
            commandlineArguments.add(agentArg);
        }

        // jdkPath
        if (jdkPath != null) {
            commandlineArguments.add("--jdksupport");
            commandlineArguments.add(jdkPath);
        }

        // failOnUnresolvableDependency
        if (failOnUnresolvableDependency) {
            commandlineArguments.add("--failonunresolvabledependency");
        }

        // cache strategy
        if (!(getCacheStrategy() instanceof NoOpCacheStrategy)) {
            commandlineArguments.add("--cachestrategy");
            commandlineArguments.add(getCacheStrategy().getSimpleName());
        }

        return commandlineArguments.toArray(new String[0]);
    }

    /**
     * Getter for the preprocessing caching strategy. Default strategy is {@link NoOpCacheStrategy} if it hasn't been provided explicitly.
     *
     * @return configured caching strategy
     */
    public CacheStrategy getCacheStrategy() {
        return cacheStrategy == null ? new NoOpCacheStrategy() : this.cacheStrategy;
    }

    /**
     * A toString method for capturing the content of the config that is relevant to the instrumentation context for caching purposes.
     *
     * @return a string representing arguments are that relevant to the instrumentation context for caching.
     */
    public String toStringForCaching() {
        return "PreprocessConfig{" +
            ", signedJarHandlingStrategy=" + getSignedJarHandlingStrategy().getClass().getName() +
            ", outputDir='" + outputDir + '\'' +
            ", agentPath='" + agentPath + '\'' +
            ", suffix='" + suffix + '\'' +
            ", javaVersion='" + javaVersion + '\'' +
            ", agentArg='" + agentArg + '\'' +
            ", failOnUnresolvableDependency=" + failOnUnresolvableDependency +
            ", cacheStrategy=" + getCacheStrategy().getSimpleName() +
            '}';
    }
}
