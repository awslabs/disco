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
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
}

tasks.shadowJar  {
    manifest {
        attributes(mapOf(
            "Disco-Installable-Classes" to "software.amazon.disco.agent.coroutines.CoroutinesSupport",
            "Disco-Classloader" to "plugin"
        ))
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.5.+")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    doLast {
        delete(rootProject.buildDir)
    }
}