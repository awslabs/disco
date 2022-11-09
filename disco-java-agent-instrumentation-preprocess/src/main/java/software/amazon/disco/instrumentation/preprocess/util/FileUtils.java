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

package software.amazon.disco.instrumentation.preprocess.util;

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.plugin.PluginDiscovery;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Utility class for performing JarFile related tasks.
 */
public class FileUtils {
    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    /**
     * Reads the byte[] of a JarEntry from a JarFile
     *
     * @param jarfile JarFile where the binary data will be read
     * @param entry   JarEntry to be read
     * @return byte[] of the entry
     * @throws IllegalArgumentException exception thrown when failed to extract the byte array of a Jar entry
     */
    public static byte[] readEntryFromJar(JarFile jarfile, JarEntry entry) {
        try (final InputStream entryStream = jarfile.getInputStream(entry)) {
            final byte[] buffer = new byte[2048];

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (int len = entryStream.read(buffer); len != -1; len = entryStream.read(buffer)) {
                os.write(buffer, 0, len);
            }
            return os.toByteArray();

        } catch (IOException e) {
            throw new IllegalArgumentException(entry.getName(), e);
        }
    }

    /**
     * Verify whether a Jar is signed using the JDK provided tool 'jarsigner'.
     * <p>
     * The 'jarsigner' tool will be invoked on a separate process via a 'ProcessBuilder' instance due to the fact that the tool may simply exit its process when an error is
     * encountered which will indirectly terminate the Preprocessor's process, effectively breaking its own error handling workflow.
     * <p>
     * In addition to the exit value, the console output of the 'jarsigner' tool will also be parsed since a status code of '0' simply denotes that the tool has completed its
     * execution successfully but not whether the Jar in question was signed or not.
     * <p>
     * 3 possible outcomes can be returned from this method:
     * UNSIGNED
     * - The 'jarsigner' tool did not detect any Jar signing artifacts(certificate, signed manifest and so on...) after having scanned the Jar.
     * SIGNED
     * - The 'jarsigner' tool successfully verified the Jar and validated that it was not tampered with after being signed.
     * INVALID
     * - An error occurred during the verification process. Potential failure points are: invalid signing state, keystore loading error, IO related exceptions and so on...
     * <p>
     * See 'sun.security.tools.jarsigner.Main' for more detail.
     *
     * @param file Jar file to be verified
     * @return the enum value denoting the outcome of the verification process.
     */
    public static JarSigningVerificationOutcome verifyJar(final File file) {
        logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Verifying Jar: " + file.getAbsolutePath());

        // when running tests via Intellij, 'System.getProperty("java.home")' would return the path to the jre, e.g. '/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home/jre',
        // whereas when running the same tests via './gradlew build', the path would be pointing to the parent home directory, e.g. '/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home'
        final File javaHomeDir = new File(System.getProperty("java.home"));
        final File jarSignerDir = javaHomeDir.getAbsolutePath().endsWith("jre") ? new File(javaHomeDir.getParentFile(), "bin") : new File(javaHomeDir, "bin");

        final ProcessBuilder processBuilder = new ProcessBuilder(jarSignerDir + "/jarsigner", "-verify", file.getAbsolutePath());
        try {
            final Process process = processBuilder.start();
            processBuilder.redirectErrorStream(true);

            final int exitCode = process.waitFor();
            final String result = readInputStream(process.getInputStream());

            logger.trace(PreprocessConstants.MESSAGE_PREFIX + "Jar verification exit code is: " + exitCode);
            logger.trace(PreprocessConstants.MESSAGE_PREFIX + "Jar verification output message:\n " + result);

            if (exitCode == 0) {
                return result.contains("jar is unsigned.") ? JarSigningVerificationOutcome.UNSIGNED : JarSigningVerificationOutcome.SIGNED;
            } else {
                // a failure occurred while verifying the given Jar indicating that the Jar signing state is invalid.
                return JarSigningVerificationOutcome.INVALID;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(PreprocessConstants.MESSAGE_PREFIX + "Failed to verify Jar: " + file.getAbsolutePath(), e);
            return JarSigningVerificationOutcome.INVALID;
        }
    }

    /**
     * Convenience method to create a temporary file named after the output of 'ManagementFactory.getRuntimeMXBean().getName()' to uniquely
     * identify this file across multiple concurrent instances of preprocessors.
     *
     * @param temporaryManifestDir dir where the temporary manifest file will be created under
     * @return the generated temporary file
     * @throws IOException thrown when failed to create temporary file
     */
    public static File createTemporaryManifestFile(final File temporaryManifestDir) throws IOException {
        if (!temporaryManifestDir.exists()) {
            temporaryManifestDir.mkdirs();
        }

        // temp manifest file is named uniquely across processes in the format of '<pid>@<hostname>'.
        final File manifest = new File(temporaryManifestDir, ManagementFactory.getRuntimeMXBean().getName());
        manifest.createNewFile();

        return manifest;
    }

    /**
     * Scan the plugin directory supplied to discover all Disco plugins. Each discovered File is then validated with the help of 'PluginDiscovery#validateDiscoPlugin()`
     * method in Disco core.
     *
     * @param agentConfig agent config meant to be supplied to the Disco agent containing args such as "pluginpath"
     * @return a set of discovered plugin Jar files.
     */
    public static Set<File> scanPluginsFromAgentConfig(final String agentConfig) {
        final Set<File> pluginsDiscovered = new HashSet<>();

        // extract the arg pair "pluginpath=path_to_plugins"
        final String pluginDirArgPair = Arrays.stream(agentConfig.split(":"))
            .filter(arg -> arg.toLowerCase(Locale.ROOT).startsWith("pluginpath"))
            .findFirst()
            .get();

        if (pluginDirArgPair == null) {
            return pluginsDiscovered;
        }

        final File pluginDir = new File(pluginDirArgPair.split("=")[1]);
        if (pluginDir.exists() && pluginDir.isDirectory()) {
            for (File plugin : pluginDir.listFiles(file -> file.getName().endsWith(".jar"))) {
                try {
                    if (PluginDiscovery.validateDiscoPlugin(plugin) != null) {
                        pluginsDiscovered.add(plugin);
                    }
                } catch (Exception e) {
                    logger.warn(PreprocessConstants.MESSAGE_PREFIX + "Error occurred while validating plugin file: " + plugin.getAbsolutePath(), e);
                }
            }
        }

        return pluginsDiscovered;
    }

    /**
     * Read the supplied input stream.
     *
     * @param inputStream input stream to be read
     * @return content of the input stream read
     * @throws IOException any IO related exceptions thrown while reading the input stream
     */
    private static String readInputStream(final InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
}
