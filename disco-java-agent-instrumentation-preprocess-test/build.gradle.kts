/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

plugins {
    `java-library`
}

val preprocessProject = ":disco-java-agent-instrumentation-preprocess";
val preprocessorExecutablePath = project(preprocessProject).buildDir.absolutePath + "/libs/disco-java-agent-instrumentation-preprocess-${project.version}.jar"
val preprocessTestInstrumentationTargetProject = ":disco-java-agent-instrumentation-preprocess-test:disco-java-agent-instrumentation" +
        "-preprocess-test-target"
val preprocessTestPluginProject = ":disco-java-agent-instrumentation-preprocess-test:disco-java-agent-instrumentation" +
        "-preprocess-test-plugin"
val discoAgentProject = ":disco-java-agent:disco-java-agent"

dependencies {
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
    testCompileOnly(project(preprocessTestInstrumentationTargetProject))
    testCompileOnly(project(preprocessTestPluginProject))
    testCompileOnly(project(preprocessProject, "shadow"))
}

val discoDir = "${buildDir}/disco"
var discoAgentPath = project(discoAgentProject).buildDir.absolutePath + "/libs/disco-java-agent-${project.version}.jar"
val pluginDir = project(preprocessTestPluginProject).buildDir.absolutePath + "/libs"
val outDir = "${discoDir}/static-instrumentation"
val pluginDirWithConfigFile = "${discoDir}/plugins"

// create a 'disco' dir where Disco plugins and the config override file will reside in
val setupPluginsDir = task<Copy>("setupPluginsDir"){
    from(pluginDir)
    into(pluginDirWithConfigFile)

    dependsOn("${preprocessTestPluginProject}:build")
}

// create the config file under the same path supplied to the agent via the 'pluginsPath' arg.
val configFilePath = "${pluginDirWithConfigFile}/disco.config"
val createConfigFile = task("CreateConfigFile"){
    doLast{
        var file = file(configFilePath)
        file.writeText("runtimeonly=true")
    }

    dependsOn(setupPluginsDir)
}

// statically instrument the integ-test source package using the integ-test plugin and save the result to a temp folder
val testDependenciesPreprocess = tasks.register<JavaExec>("testDependenciesPreprocess") {
    val preprocessTestInstrumentationTargetPath = project(preprocessTestInstrumentationTargetProject).buildDir.absolutePath +
            "/libs/disco-java-agent-instrumentation-preprocess-test-target-${project.version}.jar"

    main = "software.amazon.disco.instrumentation.preprocess.cli.Driver";
    classpath = files(preprocessorExecutablePath)
    args = listOf(
        "-ap", discoAgentPath,
        "-sps", preprocessTestInstrumentationTargetPath,
        "-out", outDir,
        "-arg", "verbose:loggerfactory=software.amazon.disco.agent.reflect.logging.StandardOutputLoggerFactory:pluginPath=$pluginDir",
        "--verbose"
    )

    //we need the agent and plugins to be built first
    dependsOn("$discoCoreProject:build")
    dependsOn("${discoAgentProject}:build")
    dependsOn("$preprocessTestInstrumentationTargetProject:build")
    dependsOn("$preprocessTestPluginProject:build")
    dependsOn(setupPluginsDir)
}

// invoke the tests with the 'runtimeonly' agent arg which will set the agent to a 'dependency provider' only state to avoid duplicate instrumentations of statically instrumented classes
tasks.test {
    // add the instrumented jar to the classpath
    classpath = sourceSets["test"].runtimeClasspath
        .plus(layout.files("$outDir/disco-java-agent-instrumentation-preprocess-test-target-${project.version}.jar"))

    // attach the Disco agent with the "runtimeonly" arg
    jvmArgs("-javaagent:${discoAgentPath}=pluginPath=${pluginDir}:loggerfactory=software.amazon.disco.agent.reflect.logging.StandardOutputLoggerFactory:runtimeonly")

    dependsOn(testDependenciesPreprocess)
    dependsOn(createConfigFile)
}

// invoke the tests with the config override file present under the supplied 'pluginPath' directory. Test cases executed in this task are sensitive to excessive instrumentation
// where a statically instrumented class being instrumented again at runtime would trigger test failures due to more than expected number of events published.
val configFileOverrideTest = task<Test>("configFileOverrideTest"){
    // use the same classpath and tests as the 'test' target
    classpath = sourceSets["test"].runtimeClasspath
        .plus(layout.files("$outDir/disco-java-agent-instrumentation-preprocess-test-target-${project.version}.jar"))
    testClassesDirs = sourceSets["test"].output.classesDirs

    // attach the Disco agent WITHOUT the 'runtimeonly' flag since 'AgentConfigParser' will simply override the 'runtimeonly' flag to true after parsing the 'disco.config' file
    // generated prior to this task.
    jvmArgs("-javaagent:${discoAgentPath}=pluginPath=${pluginDirWithConfigFile}:loggerfactory=software.amazon.disco.agent.reflect.logging.StandardOutputLoggerFactory")

    dependsOn(testDependenciesPreprocess)
    dependsOn(createConfigFile)
}

// create a new srcset for JDK static instrumentation specific tests
sourceSets {
    create("jdkTest") {
        java {
            srcDir("src/jdkTest/java")
        }
    }
}

// extend existing test configuration to get dependencies inherited from parent build script such as JUnit
val jdkTestImplementation: Configuration by configurations.getting { extendsFrom(configurations.testImplementation.get()) }
val jdkTestCompileOnly: Configuration by configurations.getting {}

dependencies {
    jdkTestImplementation
    jdkTestCompileOnly(project(preprocessProject, "shadow"))
}

val discoCoreProject = ":disco-java-agent:disco-java-agent-core"
val pluginsDir = project.buildDir.absolutePath + "/libs"
val outputDir = project.buildDir.absolutePath + "/static-instrumentation"

// task to statically instrument all required sources to run jdk(8 and 9+) integ tests with BTI enabled.
val preprocess = tasks.register<JavaExec>("preprocess") {
    main = "software.amazon.disco.instrumentation.preprocess.cli.Driver";
    classpath = files(preprocessorExecutablePath)
    args = listOf(
        "-ap", discoAgentPath,
        "-jdks", System.getProperty("java.home"),
        "-sps", project(discoCoreProject).buildDir.absolutePath + "/tmp/discoCoreIntegTests.jar",
        "-out", outputDir,
        "-arg", "verbose:loggerfactory=software.amazon.disco.agent.reflect.logging.StandardOutputLoggerFactory:pluginPath=$pluginDir",
        "--verbose"
    )

    dependsOn(tasks.test)
    dependsOn("${discoCoreProject}:build")
    dependsOn("${discoAgentProject}:build")
}

// JDK 11 static instrumentation integ test
val jdkTest = task<Test>("jdkTest") {
    // append Disco core integ test classes to 'testClassesDirs'
    testClassesDirs = sourceSets["jdkTest"].output.classesDirs
        .plus(layout.files(project(discoCoreProject).buildDir.absolutePath + "/classes/java/integ"))

    // append the statically instrumented 'discoCoreIntegTests.jar' to the runtime classpath.
    // This instrumented Jar also contains 'Thread' and 'ExecutorService' subclasses
    classpath = sourceSets["jdkTest"].runtimeClasspath
        .plus(layout.files(outputDir + "/discoCoreIntegTests.jar"))

    // attach the Disco agent in 'runtimeonly' mode
    jvmArgs("-javaagent:${discoAgentPath}=pluginPath=${pluginDir}:runtimeonly")

    if (JavaVersion.current().isJava9Compatible) {
        // configure module patching and create read link from java.base to all unnamed modules
        jvmArgs("--patch-module=java.base=${outputDir}/jdk/InstrumentedJDK.jar")
        jvmArgs("--add-reads=java.base=ALL-UNNAMED")
        jvmArgs("--add-exports=java.base/software.amazon.disco.agent.concurrent.preprocess=ALL-UNNAMED")
    } else {
        // prepend the instrumented JDK artifact to the bootstrap class path
        jvmArgs("-Xbootclasspath/p:${outputDir}/jdk/InstrumentedJDK.jar")
    }

    dependsOn(preprocess)
}

tasks.build {
    dependsOn(jdkTest)
    dependsOn(configFileOverrideTest)
}