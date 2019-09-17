package com.amazon.disco.agent.config;


import java.util.Arrays;

/**
 * Parses command line arguments passed to Alpha One agent via command line
 */
public class AgentConfigParser {

    /**
     * Parses command line arguments.
     *
     * @param args - the arguments passed in to premain
     */
    public AgentConfig parseCommandLine(String args) {
        if (args == null || args.isEmpty()) {
            return new AgentConfig(null);
        }

        //TODO these regexes look overwrought.

        //split by colon so that e.g. "arg=value1:arg2=value2" becomes ["arg1=value1", "arg2=value2"]
        String[] individualArgs = args.split("(?<!\\\\):");

        //for each arg=value pair, split by "="
        final AgentConfig result = new AgentConfig(Arrays.asList(individualArgs));

        for (String arg : individualArgs) {
            String[] pair = arg.split("(?<!\\\\)=");
            String value = pair.length > 1
                    ? pair[1]
                        .replaceAll("\\\\:", ":")
                        .replaceAll("\\\\=", "=")
                    : "";

            switch (pair[0].toLowerCase()) {
                case "applicationname":
                    result.setApplicationName(value);
                    break;
                case "nodefaultinstallables":
                    result.setInstallDefaultInstallables(false);
                    break;
                case "installables":
                    result.setCustomInstallableClasses(value.split(","));
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

        if (result.getApplicationName() == null) {
            throw new IllegalArgumentException();
        }

        return result;
    }
}
