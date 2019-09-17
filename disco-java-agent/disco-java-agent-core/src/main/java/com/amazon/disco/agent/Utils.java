package com.amazon.disco.agent;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

/**
 * Various utilities methods.
 */
public class Utils {
    private final static Utils INSTANCE = new Utils();

    private File getSystemTemporaryFolder() {
        File tmpFolder = new File(System.getProperty("java.io.tmpdir"), "alphaone");
        if (!tmpFolder.exists())
            tmpFolder.mkdirs();
        return tmpFolder;
    }

    /**
     * Private constructor for singleton semantics
     */
    private Utils() {
    }

    protected Map<String, String> env() {
        // Extracted for testing purposes
        return System.getenv();
    }

    /**
     * Singleton access
     * @return the Utils singleton
     */
    public static Utils getInstance() {
        return INSTANCE;
    }

    /**
     * Searches for value of given system property passed to JVM, if can't find it
     * tries to find value in process environment variables.
     *
     * @param systemPropertyName the name of system property to search for
     * @param environmentVarName the name of environment variable to search for
     * @return an Optional value for the requested config
     */
    public Optional<String> getProcessConfigurationValue(final String systemPropertyName, final String environmentVarName) {
        String resultValue = System.getProperty(systemPropertyName);
        if (resultValue == null)
            resultValue = System.getenv(environmentVarName);
        return Optional.of(resultValue);
    }

    /**
     * File util used to extract content from a resource file for testing.
     * @param filename the file to read from, which must be on the correct path
     * @return string content from the resource file.
     */
    public String getStringFromResource(final String filename) {
        InputStream inputStream = Utils.class.getResourceAsStream(filename);
        return inputStream != null ? (new Scanner(inputStream,
                StandardCharsets.UTF_8.name())).useDelimiter("(\\A)").next() : null;
    }

    /**
     * Finds a folder for temporary files. Firstly tries to search Apollo paths, if it fails
     * use system temporary directory
     *
     * @return the folder where temporary files should be stored.
     */
    public File getTempFolder() {
        Map<String, String> env = env();

        String tmpFolderName = env.get("ENVROOT");
        if (tmpFolderName == null)
            tmpFolderName = env.get("APOLLO_ACTUAL_ENVIRONMENT_ROOT");
        if (tmpFolderName == null)
            tmpFolderName = env.get("APOLLO_CANONICAL_ENVIRONMENT_ROOT");

        if (tmpFolderName != null) {
            File tmpFolder = new File(new File(new File(new File(tmpFolderName,
                    "var"),
                    "tmp"),
                    "user"),
                    "alphaone");
            if (!tmpFolder.exists() && !tmpFolder.mkdirs())
                return getSystemTemporaryFolder();

            return tmpFolder;
        } else {
            return getSystemTemporaryFolder();
        }
    }
}
