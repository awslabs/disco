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

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
    testImplementation(project(":disco-java-agent:disco-java-agent-inject-api"))
}

//prevent tests from importing core classes directly - only allow them to use the public api
configurations.testImplementation {
    exclude(module="disco-java-agent-core")
}

fun debuggerJvmArg(suspend: String): String {
    return "-agentlib:jdwp=transport=dt_socket,address=localhost:1337,server=y,suspend=$suspend"
}

tasks.shadowJar  {
    //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (e.g. disco-java-agent-0.1.jar)
    archiveClassifier.set(null as String?)

    manifest {
        attributes(mapOf(
                "Premain-Class" to "software.amazon.disco.agent.DiscoAgent",
                "Agent-Class" to "software.amazon.disco.agent.DiscoAgent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true",
                "Boot-Class-Path" to archiveFileName.get()
        ))
    }

    // Verify JAR contents don't contain any unshaded artifacts
    doLast {
        val jarFile = zipTree(archiveFile.get().asFile)
        jarFile.visit {
            if (this.path.endsWith(".class") && !this.path.startsWith("software/amazon/disco")) {
                throw GradleException("Unshaded class " + this.path + " found in shaded JAR")
            }
        }
    }
}

fun getJarPath(jar: Jar): String {
    return jar.archiveFile.get().asFile.path
}

val agentPath = getJarPath(tasks["shadowJar"] as Jar)

tasks.named<Test>("test") {
    //load the agent TWICE for the tests for agent deduplication (and allow debugging on the usual socket)
    //we also communicate the full path to the agent in a System variable
    val agentArg = "-javaagent:$agentPath=extraverbose"
    jvmArgs(debuggerJvmArg("n"),
            "$agentArg:loggerfactory=software.amazon.disco.agent.TestLoggerFactory",
            agentArg,
            "-DdiscoAgentPath=$agentPath"
    )
    dependsOn(tasks["shadowJar"])
}

sourceSets {
    create("killswitch-test") {
        java {
            srcDir("src/killswitch-test/java")
        }
    }
}

val killswitchTestImplementation: Configuration by configurations.getting {}
dependencies {
    killswitchTestImplementation("junit", "junit", "4.12")
    killswitchTestImplementation(project(":disco-java-agent:disco-java-agent-api"))
}

val standardOutputLoggerFactoryFQN: String by rootProject.extra
val killswitchTest = task<Test>("killswitchTest") {
    testClassesDirs = sourceSets["killswitch-test"].output.classesDirs
    classpath = sourceSets["killswitch-test"].runtimeClasspath
    jvmArgs = listOf(debuggerJvmArg("n"), "-javaagent:$agentPath=verbose:loggerfactory=${standardOutputLoggerFactoryFQN}")

    doFirst {
        val killswitchFile = File(project.buildDir.absolutePath + "/libs/disco.kill")
        killswitchFile.createNewFile()
    }

    dependsOn(tasks["shadowJar"])
}

val deleteKillswitch = task("deleteKillswitch") {
    doFirst {
        File( project.buildDir.absolutePath + "/libs/disco.kill").delete()
    }
    mustRunAfter(killswitchTest)
}

sourceSets {
    create("installation-error-test") {
        java {
            srcDir("src/installation-error-test/java")
        }
    }
}

val installationErrorTestJar = tasks.register<Jar>("installationErrorTestJar") {
    archiveClassifier.set("installation-error-test")
    // We release build/libs/*.jar, so to prevent the release of this test JAR we put it in build/test-libs/
    destinationDirectory.set(project.buildDir.resolve("test-libs"))
    from(sourceSets["installation-error-test"].output.classesDirs)
    manifest {
        attributes(mapOf(
            // For simplicity of project structure, we package a trivial application and our "spoiler agent" into the same JAR.
            "Premain-Class" to "software.amazon.disco.agent.SpoilerAgent",
            "Main-Class" to "software.amazon.disco.agent.TrivialApplication",
            "Boot-Class-Path" to archiveFileName.get()
        ))
    }
}

val javaHome: String = System.getenv("JAVA_HOME") ?: "/usr"
val javaPath: String = "$javaHome/bin/java"

fun runJava(javaArgs: List<String>): Pair<Int, String> {
    val commandArgs = listOf(javaPath) + javaArgs
    val process = ProcessBuilder(commandArgs).start()
    return Pair(process.waitFor(), process.errorStream.bufferedReader().readText())
}

// Verify that Disco agent terminates the application process if it fails to instrument ScheduledFutureTask.
// SpoilerAgent causes this class to be loaded, which prevents its instrumentation by Disco agent,
// and thus if it's loaded before Disco agent, we should see the application fail (nonzero exit code).
val installationErrorTest = task("installationErrorTest") {
    doFirst {
        val testAgentPath = getJarPath(installationErrorTestJar.get())
        val agentArg = "-javaagent:$agentPath"
        val testAgentArg = "-javaagent:$testAgentPath"
        val testCases = listOf(
            // Pairs of (Java arguments, whether to expect success)
            Pair(listOf(agentArg, "-jar", testAgentPath), true),
            Pair(listOf(agentArg, testAgentArg, "-jar", testAgentPath), true),
            Pair(listOf(testAgentArg, agentArg, "-jar", testAgentPath), false)
        )
        testCases.forEach {
            val (javaArgs, expectSuccess) = it
            val (exitCode, stderr) = runJava(javaArgs)
            val success = exitCode == 0
            if (success != expectSuccess) {
                throw AssertionError(
                    "Test failed: javaArgs=$javaArgs, expectSuccess=$expectSuccess, success=$success, stderr=$stderr")
            }
        }
    }
    dependsOn(tasks["shadowJar"])
    dependsOn(installationErrorTestJar)
}

tasks.build {
    dependsOn(tasks["shadowJar"]) // We want to build the agent, regardless of tests
    dependsOn(killswitchTest)
    dependsOn(deleteKillswitch)
    dependsOn(installationErrorTest)
}
