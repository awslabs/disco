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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Parses command line arguments passed to the DiSCo agent via command line.
 * The presence of a config file named {@link #CONFIG_FILE_NAME} under the plugin path supplied will be checked and used to override args parsed from the command line.
 * See {@link #applyConfigOverride(AgentConfig)}} for more details.
 */
public class AgentConfigParser {
    protected static final String CONFIG_FILE_NAME = "disco.config";

    /**
     * Parses command line arguments and applies overrides from a config file if applicable.
     *
     * @param args - the arguments passed in to premain
     * @return an 'AgentConfig' instance generated from the given args and corresponding overrides.
     */
    public AgentConfig parseCommandLine(String args) {
        final AgentConfig result = new AgentConfig(new ArrayList<>());

        if (args != null && !args.isEmpty()) {
            // iterate over the map entries in the order where entries were inserted.
            final LinkedHashMap<String, String> argsMap = parseArgsStringToMap(args);
            for (Map.Entry<String, String> argPair : argsMap.entrySet()) {
                applyArgToConfig(result, argPair.getKey(), argPair.getValue());
            }
        }

        applyConfigOverride(result);
        return result;
    }

    /**
     * Parse the Disco agent args represented as a single string into a LinkedHashMap to preserve order.
     * Arg keys are inserted into the resulting TreeMap in a case-insensitive fashion.
     *
     * @param argStr args retrieved from the command line or a config file presented as ':' delimited list of args
     * @return a 'LinkedHashMap' instance representing Disco agent arg pairs, empty if no args were supplied.
     */
    protected LinkedHashMap<String, String> parseArgsStringToMap(final String argStr) {
        final LinkedHashMap<String, String> argsMap = new LinkedHashMap<>();

        if (argStr == null || argStr.isEmpty()) {
            return argsMap;
        }

        //TODO these regexes look overwrought.
        String[] individualArgs = argStr.split("(?<!\\\\):");

        insertArgsToMap(individualArgs, argsMap);

        return argsMap;
    }

    /**
     * Split each element of the passed-in args array into key value pairs and insert them to the map.
     *
     * @param individualArgs args to be inserted into the map. A single element of the array would take the form of "key=value" or "key"
     * @param argsMap        map in which args will be inserted to
     */
    private void insertArgsToMap(final String[] individualArgs, final Map<String, String> argsMap) {
        for (String arg : individualArgs) {
            //for each arg=value pair, split by "="
            final String[] pair = arg.split("(?<!\\\\)=");
            final String value = pair.length > 1
                ? pair[1]
                .replaceAll("\\\\:", ":")
                .replaceAll("\\\\=", "=")
                : "";

            argsMap.put(pair[0], value);
        }
    }

    /**
     * Apply an arg key/value pair to the 'AgentConfig' instance passed in.
     * The key will be compared in a switch statement in a case-insensitive fashion, whereas the value casing will be kept intact.
     *
     * @param config   'AgentConfig' instance where the arg key/value pair will be applied to
     * @param argKey   arg key to be parsed in a case-insensitive fashion
     * @param argValue arg value to be applied to the 'AgentConfig' instance
     */
    private void applyArgToConfig(final AgentConfig config, final String argKey, final String argValue) {
        switch (argKey.toLowerCase(Locale.ROOT)) {
            case "runtimeonly":
                if (argValue.isEmpty() || argValue.equalsIgnoreCase("true")) {
                    config.setRuntimeOnly(true);
                } else if (argValue.equalsIgnoreCase("false")) {
                    config.setRuntimeOnly(false);
                }
                break;
            case "pluginpath":
                config.setPluginPath(argValue);
                break;
            case "verbose":
                config.setVerbose(true);
                break;
            case "extraverbose":
                config.setExtraverbose(true);
                break;
            case "loggerfactory":
                config.setLoggerFactoryClass(argValue);
                break;
            default:
                //not an error, do nothing. individual interceptors might receive this arg instead
                break;
        }

        final String argPair = argKey + (argValue == null || argValue.isEmpty() ? "" : "=" + argValue);
        config.getArgs().add(argPair);
    }

    /**
     * Apply overrides to the passed in 'AgentConfig' instance if the config override file is present.
     *
     * Args expressed in the config file must follow the format where each key value pair occupies a separate line. For instance, the content of a particular config file containing
     * more than 1 arg may look like this:'runtimeonly\nverbose\nloggerfactory=com.amazon.SomeLogger'. These args are parsed one at a time in order. Duplicated entries will result in
     * the existing value being overridden by the most recently discovered value.
     *
     * On the other hand, 'pluginpath' is an exception argument which can't have its value overwritten by the config file. Since the config file is retrieved from the very same
     * plugin path supplied via the command line arg, allowing the config file to override this plugin path would be counter-intuitive.
     * <p>
     * In addition, the absence of an arg in the config file, e.g. 'runtimeonly', does not indicate that the existing field in the 'AgentConfig' instance should be reverted to
     * its default value. In other words, args expressed in the config file are only additive.
     *
     * @param config AgentConfig instance that may have some of its fields overwritten.
     */
    protected void applyConfigOverride(final AgentConfig config) {
        final List<String> argsListFromConfigFile = readConfigFileFromPluginPath(config.getPluginPath());

        if (argsListFromConfigFile == null || argsListFromConfigFile.isEmpty()) {
            return;
        }

        final LinkedHashMap<String, String> argsMap = new LinkedHashMap<>();
        insertArgsToMap(argsListFromConfigFile.toArray(new String[1]), argsMap);

        // iterate over the map entries in the order of insertion.
        for (Map.Entry<String, String> argPair : argsMap.entrySet()) {
            if (argPair.getKey().equalsIgnoreCase("pluginpath")) {
                // overriding the plugin path is not supported
                System.err.println("Disco(Agent) overwriting the 'pluginPath' agent argument is not supported. Value supplied will be ignored.");
                continue;
            } else {
                applyArgToConfig(config, argPair.getKey(), argPair.getValue());
            }
        }
    }

    /**
     * Read a config file which contains 'AgentConfig' overrides such as 'runtimeonly'. Each line is trimmed before being appended to the
     * returning list.
     *
     * @param pluginPath plugin path supplied via the command line args
     * @return the list of args read from the config file if present, null if file doesn't exist
     */
    protected List<String> readConfigFileFromPluginPath(final String pluginPath) {
        // return immediately since it's not possible to determine the location of the config file.
        if (pluginPath == null) {
            return null;
        }

        // check for the presence of the config file
        final File configFile = new File(pluginPath, CONFIG_FILE_NAME);
        if (!configFile.exists() || !configFile.isFile()) {
            return null;
        }

        try (Scanner scanner = new Scanner(configFile)) {
            final List<String> argsRead = new ArrayList<>();

            while (scanner.hasNextLine()) {
                final String arg = scanner.nextLine().trim();
                if (!arg.isEmpty()) {
                    argsRead.add(arg);
                }
            }

            return argsRead;
        } catch (IOException | IllegalArgumentException e) {
            // log to STDOUT if failed to read config file since 'LoggerFactory' hasn't been configured yet.
            System.err.println("Disco(Agent) failed to load config override file: " + e.getMessage());
            return null;
        }
    }
}
