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
    testImplementation("javax.servlet", "javax.servlet-api", "3.0.1")
    testImplementation("org.apache.httpcomponents", "httpclient", "4.5.10")
}
tasks.shadowJar {
    manifest {
        attributes(mapOf(
            "Disco-Installable-Classes" to "software.amazon.disco.agent.web.WebSupport",
            "Disco-Classloader" to "bootstrap"
        ))
    }
}
//create a new empty safeTests test config - not extending from existing compile or testCompile, since we don't want to
//be able to compile against Core etc.
val safeTestsImplementation by configurations.creating

dependencies {
    safeTestsImplementation("junit", "junit", "4.12")
}

sourceSets {
    create("safeTests") {
        java {
            srcDir("src/safetests/java")
        }
    }
}

val safeTests = task<Test>("safeTests") {
    description = "Custom task to run safe tests without web library dependency like ApacheClient"
    testClassesDirs = sourceSets["safeTests"].output.classesDirs
    classpath = sourceSets["safeTests"].runtimeClasspath.minus(configurations.compileClasspath.get())
}

 tasks.build {
    dependsOn(safeTests)
}
