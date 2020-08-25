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
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

tasks.shadowJar  {
    manifest {
        attributes(mapOf(
            "Disco-Installable-Classes" to "software.amazon.disco.agent.AWSSupport"
        ))
    }
}

// Defines a new source set for our safety integration test, which verifies that adding the Disco AWS Plugin
// will not break customer's code, even if they don't depend on the AWS SDK
sourceSets {
    create("safetyTest") {
    }
}

// Initializes the safetyTestImplementation configuration needed to declare dependencies
val safetyTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    // Include the API sources in the plugin JAR, since they are referenced directly
    runtimeOnly(project(":disco-java-agent-aws:disco-java-agent-aws-api")) {
        isTransitive = false
    }

    testImplementation("com.amazonaws", "aws-java-sdk-dynamodb", "1.11.840")
    testImplementation("software.amazon.awssdk", "dynamodb", "2.13.76")
    testImplementation("software.amazon.awssdk", "s3", "2.13.76")
    testImplementation("com.github.tomakehurst", "wiremock-jre8", "2.27.0")
    testImplementation(project(":disco-java-agent-aws:disco-java-agent-aws-api"))

    // Only dependency the safety test needs is JUnit
    safetyTestImplementation("junit:junit:4.12")
}

// This task adds the Disco Java Agent and AWS SDK plugin like a normal integ test,
// then runs the test(s) in the safetyTest/ source directory
// Adapted from: https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
val safetyTestTask = task<Test>("safetyTest") {
    description = "Runs class safety tests"
    group = "verification"

    testClassesDirs = sourceSets["safetyTest"].output.classesDirs
    classpath = sourceSets["safetyTest"].runtimeClasspath

    jvmArgs("-javaagent:../../disco-java-agent/disco-java-agent/build/libs/disco-java-agent-${project.version}.jar=pluginPath=./build/libs")

    dependsOn(":disco-java-agent:disco-java-agent:build")
    dependsOn(":disco-java-agent-aws:disco-java-agent-aws-plugin:assemble")

    mustRunAfter("test")
}

tasks.check {
    dependsOn(safetyTestTask)
}
