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
    id("io.freefair.lombok") version "6.3.0"
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

tasks.shadowJar {
    manifest {
        attributes(mapOf(
                "Main-Class" to "software.amazon.disco.instrumentation.preprocess.cli.Driver"
        ))
    }
}