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

package software.amazon.disco.agent;

import software.amazon.disco.agent.inject.Injector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry class for the Disco substrate agent. This class is loaded with the System class loader by the JVM, but we want
 * to be in the bootstrap classloader to perform instrumentation. So all we do here is:
 *
 * 1. Load the shaded JAR containing the proper DiscoBootstrapAgent class and ByteBuddy dependencies to the bootstrap
 * classpath if it's not already there (e.g. by usage of the Inject API)
 * 2. Load the "real" DiscoBootstrapAgent class
 * 3. Jump over to the real agent class to initialize DiSCo, load plugins, etc
 *
 * This class should be kept as minimal as possible to avoid loading classes before we have a chance to instrument them.
 *
 * Adapted under the Apache 2.0 license from the OpenTelemetry Agent:
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/master/javaagent/src/main/java/io/opentelemetry/javaagent/OpenTelemetryAgent.java
 */
public class DiscoAgent {
    private static final Class<?> thisClass = DiscoAgent.class;

    /**
     * Entry point when agent is loaded from the command line via a "-javaagent" argument. This is also the method called when
     * sideloading an agent via disco-java-agent-inject-api, such as is necessary in AWS Lambda installations.
     *
     * @param agentArgs arguments which are passed to disco's core for configuration. Generally speaking this necessitates
     *                  passing a 'pluginPath' argument, so that disco knows where to find its extensions. Without this argument
     *                  the agent performs thread hand-off housekeeping, but is otherwise a no-op. See AgentConfigParser for
     *                  other available options
     * @param instrumentation an Instrumentation instance provided by the Java runtime, to allow bytecode manipulations
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        impl(agentArgs, instrumentation);
    }

    /**
     * Entry point when agent is loaded via JavaVirtualMachine.attach() by another process.
     *
     * @param agentArgs arguments which are passed to disco's core for configuration. Generally speaking this necessitates
     *                  passing a 'pluginPath' argument, so that disco knows where to find its extensions. Without this argument
     *                  the agent performs thread hand-off housekeeping, but is otherwise a no-op. See AgentConfigParser for
     *                  other available options
     * @param instrumentation an Instrumentation instance provided by the Java runtime, to allow bytecode manipulations
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        impl(agentArgs, instrumentation);
    }

    private static void impl(String agentArgs, Instrumentation instrumentation) {
        try {
            // A null classloader represents the bootstrap classloader, meaning this class is already on the bootstrap
            // classpath. This is possible if the DiSCo agent is loaded via the Inject API rather than from the command
            // line, which appends the agent JAR to the bootstrap class path search before invoking premain.
            // https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getClassLoader--
            if (thisClass.getClassLoader() != null) {
                installBootstrapJar(instrumentation);
            }

            Class<?> agentInitializerClass =
                    ClassLoader.getSystemClassLoader()
                            .loadClass("software.amazon.disco.agent.bootstrap.DiscoBootstrapAgent");
            Method startMethod =
                    agentInitializerClass.getMethod("initialize", String.class, Instrumentation.class);
            startMethod.invoke(null, agentArgs, instrumentation);
        } catch (Throwable ex) {
            // Don't rethrow.  We don't have a log manager here, so just print.
            System.err.println("DiSCo(Agent) ERROR: " + thisClass.getName());
            ex.printStackTrace();
        }
    }

    /**
     * This method attempts to retrieve the JAR file that contains this class (and the rest of the DiSCo Java agent),
     * first by using {@link CodeSource}, and second by parsing the JVM arguments, from
     *
     * @param inst an Instrumentation instance provided by the Java runtime, to allow bytecode manipulations
     * @throws IOException if Jar File isn't found
     * @throws URISyntaxException if Jar File is invalid
     */
    private static synchronized void installBootstrapJar(Instrumentation inst)
            throws IOException, URISyntaxException {

        URL javaAgentJarURL;

        // First try Code Source
        CodeSource codeSource = thisClass.getProtectionDomain().getCodeSource();

        if (codeSource != null) {
            javaAgentJarURL = codeSource.getLocation();
            File bootstrapFile = new File(javaAgentJarURL.toURI());

            if (!bootstrapFile.isDirectory()) {
                checkJarManifestPremainClassIsThis(javaAgentJarURL);
                Injector.addToBootstrapClasspath(inst, bootstrapFile);
                return;
            }
        }

        System.out.println("DiSCo(Agent) Could not get bootstrap jar from code source, using -javaagent arg");

        // ManagementFactory indirectly references java.util.logging.LogManager
        // - On Oracle-based JDKs after 1.8
        // - On IBM-based JDKs since at least 1.7
        // This prevents custom log managers from working correctly
        // Use reflection to bypass the loading of the class
        List<String> arguments = getVMArgumentsThroughReflection();

        String agentArgument = null;
        for (String arg : arguments) {
            if (arg.startsWith("-javaagent")) {
                if (agentArgument == null) {
                    agentArgument = arg;
                } else {
                    throw new RuntimeException(
                            "Multiple javaagents specified and code source unavailable, not installing DiSCo agent");
                }
            }
        }

        if (agentArgument == null) {
            throw new RuntimeException(
                    "Could not find javaagent parameter and code source unavailable, not installing DiSCo agent");
        }

        // argument is of the form -javaagent:/path/to/java-agent.jar=optionalargumentstring
        Matcher matcher = Pattern.compile("-javaagent:([^=]+).*").matcher(agentArgument);

        if (!matcher.matches()) {
            throw new RuntimeException("Unable to parse javaagent parameter: " + agentArgument);
        }

        File javaagentFile = new File(matcher.group(1));
        if (!(javaagentFile.exists() || javaagentFile.isFile())) {
            throw new RuntimeException("Unable to find javaagent file: " + javaagentFile);
        }
        javaAgentJarURL = javaagentFile.toURI().toURL();
        checkJarManifestPremainClassIsThis(javaAgentJarURL);
        Injector.addToBootstrapClasspath(inst, javaagentFile);
    }

    private static List<String> getVMArgumentsThroughReflection() {
        try {
            // Try Oracle-based
            Class managementFactoryHelperClass =
                    thisClass.getClassLoader().loadClass("sun.management.ManagementFactoryHelper");

            Class vmManagementClass = thisClass.getClassLoader().loadClass("sun.management.VMManagement");

            Object vmManagement;

            try {
                vmManagement =
                        managementFactoryHelperClass.getDeclaredMethod("getVMManagement").invoke(null);
            } catch (NoSuchMethodException e) {
                // Older vm before getVMManagement() existed
                Field field = managementFactoryHelperClass.getDeclaredField("jvm");
                field.setAccessible(true);
                vmManagement = field.get(null);
                field.setAccessible(false);
            }

            return (List<String>) vmManagementClass.getMethod("getVmArguments").invoke(vmManagement);

        } catch (ReflectiveOperationException e) {
            try { // Try IBM-based.
                Class VMClass = thisClass.getClassLoader().loadClass("com.ibm.oti.vm.VM");
                String[] argArray = (String[]) VMClass.getMethod("getVMArgs").invoke(null);
                return Arrays.asList(argArray);
            } catch (ReflectiveOperationException e1) {
                // Fallback to default
                System.out.println("DiSCo(Agent) WARNING: Unable to get VM args through reflection. " +
                                "A custom java.util.logging.LogManager may not work correctly");

                return ManagementFactory.getRuntimeMXBean().getInputArguments();
            }
        }
    }

    private static boolean checkJarManifestPremainClassIsThis(URL jarUrl) throws IOException {
        URL manifestUrl = new URL("jar:" + jarUrl + "!/META-INF/MANIFEST.MF");
        String premainClassLine = "Premain-Class: " + thisClass.getCanonicalName();
        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(manifestUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(premainClassLine)) {
                    return true;
                }
            }
        }
        throw new RuntimeException(
                "The DiSCo Agent cannot be installed, because class '"
                        + thisClass.getCanonicalName() + "' is located in '" + jarUrl
                        + "'. Make sure you don't have this .class file anywhere, besides in the disco-java-agent JAR");
    }
}
