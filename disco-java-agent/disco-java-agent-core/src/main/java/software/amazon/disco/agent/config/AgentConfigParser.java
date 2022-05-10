/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.config;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

/**
 * Parses command line arguments passed to the DiSCo agent via command line
 */
public class AgentConfigParser {
    protected static final String CONFIG_FILE_NAME = "disco.properties";

    /**
     * Parses command line arguments.
     *
     * @param args - the arguments passed in to premain
     * @return an AgentConfig generated from the given args
     */
    public AgentConfig parseCommandLine(String args) {
        final AgentConfig result = new AgentConfig();

        if (args != null && !args.isEmpty()) {
            //TODO these regexes look overwrought.

            //split by colon so that e.g. "arg=value1:arg2=value2" becomes ["arg1=value1", "arg2=value2"]
            String[] individualArgs = args.split("(?<!\\\\):");
            result.setArgs(Arrays.asList(individualArgs));

            //for each arg=value pair, split by "="
            for (String arg : individualArgs) {
                String[] pair = arg.split("(?<!\\\\)=");
                String value = pair.length > 1
                    ? pair[1]
                    .replaceAll("\\\\:", ":")
                    .replaceAll("\\\\=", "=")
                    : "";

                switch (pair[0].toLowerCase(Locale.ROOT)) {
                    case "runtimeonly":
                        result.setRuntimeOnly(true);
                        break;
                    case "pluginpath":
                        result.setPluginPath(value);
                        break;
                    case "verbose":
                        result.setVerbose(true);
                        break;
                    case "extraverbose":
                        result.setExtraverbose(true);
                        break;
                    case "loggerfactory":
                        result.setLoggerFactoryClass(value);
                        break;
                    default:
                        //not an error, do nothing. individual interceptors might receive this arg instead
                        break;
                }
            }
        }

        applyConfigOverride(result);
        return result;
    }

    /**
     * Apply overrides to the passed in 'AgentConfig' instance if the config override file is present. If the 'configOverridePath' field of the
     * passed in 'AgentConfig' instance is set, that path will be searched. Otherwise, the path to the directory containing the config override
     * will be determined by the locating of the Disco agent Jar file.
     *
     * @param config AgentConfig instance that may have some of its fields overridden
     */
    protected void applyConfigOverride(final AgentConfig config) {
        final File configOverrideFile = getDiscoConfigOverrideFileFromAgentPath(ClassLoader.getSystemClassLoader());

        if (configOverrideFile != null) {
            final Properties prop = readPropertiesFile(configOverrideFile);
            if (prop != null && prop.getProperty("runtimeonly") != null) {
                // values such as 'null', empty string, random string other than 'true' (case-insensitive) will all be considered as 'false'
                config.setRuntimeOnly(Boolean.parseBoolean(prop.getProperty("runtimeonly")));
            }
        }
    }

    /**
     * Read a .properties file which contains 'AgentConfig' overrides such as 'runtimeonly'.
     *
     * @param file file to be read
     * @return an instance of 'Properties' containing the config overrides
     */
    protected Properties readPropertiesFile(final File file) {
        try (InputStream input = new FileInputStream(file)) {
            final Properties prop = new Properties();
            prop.load(input);
            return prop;
        } catch (IOException | IllegalArgumentException e) {
            // log to STDOUT if failed to read properties file since 'LoggerFactory' hasn't been configured yet.
            System.out.println("Disco(Agent) failed to load config override file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the path to the 'disco/config' dir on a deployment environment from the path to the Disco agent itself.
     * This method makes the assumption that the {@link AgentConfigParser} class is loaded from a physical Jar file which is placed at the
     * root of the 'disco' directory.
     *
     * @return path to the 'disco/config' dir on a deployed environment
     */
    protected File getDiscoConfigOverrideFileFromAgentPath(final ClassLoader classLoader) {
        // get the resource url of this class which should be a Jar entry.
        final URL classLocation = classLoader.getResource(AgentConfigParser.class.getName().replace('.', '/') + ".class");

        // this variable may be 'null' if the agent was installed using the 'Inject' api where 'DiscoJavaAgent-1.0.jar' is appended to the
        // bootstrap classloader, making non-class file resources within the Jar undiscoverable.
        if (classLocation != null) {
            final String[] pathSegments = classLocation.getFile().split("!");

            // only move forward with parsing if class was loaded from a Jar.
            if (pathSegments.length == 2) {
                // get the path by parsing the Jar file url in the form of 'file:path_to_disco.jar!/software/amazon/disco/agent/config/AgentConfigParser.class'
                final File discoAgent = new File(pathSegments[0].split(":")[1]);
                final File discoDir = discoAgent.getParentFile();
                final File configOverrideFile = new File(discoDir.getAbsolutePath(), CONFIG_FILE_NAME);

                if (configOverrideFile.exists() && configOverrideFile.isFile()) {
                    return configOverrideFile;
                }
            }
        }
        return null;
    }
}
