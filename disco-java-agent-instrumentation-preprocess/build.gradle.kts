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

plugins {
    id("io.freefair.lombok") version "5.1.0"
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(project(":disco-java-agent:disco-java-agent-core"))
    compileOnly(project(":disco-java-agent:disco-java-agent-api"))
    implementation(project(":disco-java-agent:disco-java-agent-inject-api"))
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.3")
    testImplementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
}

/**
 * Define a secondary set of tests, for testing the actual interceptions provided by the Installables.
 */
sourceSets {
    create("integtest") {
        java {
            srcDir("src/integtest/java")
        }
    }
}

//create a new empty integ test config - not extending from existing compile or testCompile, since we don't want to
//be able to compile against Core etc.
val integtestImplementation: Configuration by configurations.getting {}

dependencies {
    integtestImplementation("junit", "junit", "4.12")
    integtestImplementation("net.bytebuddy", "byte-buddy-dep", "1.9.12")
    integtestImplementation("org.ow2.asm", "asm", "7.1")
    integtestImplementation("org.apache.logging.log4j", "log4j-core", "2.13.3")
    integtestImplementation(project(":disco-java-agent:disco-java-agent-api"))
    integtestImplementation(project(":disco-java-agent:disco-java-agent-inject-api", "shadow"))
    integtestImplementation(project(":disco-java-agent-instrumentation-preprocess", "shadow"))
}

val ver = project.version

val integtest = task<Test>("integtest") {
    testClassesDirs = sourceSets["integtest"].output.classesDirs

    classpath = sourceSets["integtest"].runtimeClasspath
            .minus(configurations.compileClasspath.get())
            .filter {
                // need to remove disco agent api from classpath because the agent to be loaded already has it as dependency
                file -> !file.endsWith("disco-java-agent-api-"+ver+".jar")
            }
            .plus(sourceSets["integtest"].runtimeClasspath.filter {
                // add back bytebuddy and asm dependencies to the classpath
                file -> file.absolutePath.contains("net.bytebuddy") || file.absolutePath.contains("org.ow2.asm")
            }
    )

    //we need the agent to be built first
    dependsOn(":disco-java-agent:disco-java-agent:build")
    mustRunAfter(tasks["test"])
}

tasks.build {
    dependsOn(integtest)
}