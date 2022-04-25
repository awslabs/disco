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
    id("com.github.johnrengelman.shadow")
}

tasks.shadowJar {
    manifest {
        attributes(mapOf(
                "Manifest-Version" to 1.0,
                "Disco-Installable-Classes" to
                        "software.amazon.disco.instrumentation.preprocess.IntegTestPluginSupport",
                "Disco-Bootstrap-Classloader" to true
        ))
    }

    val classesToMove = arrayOf(
            "software.amazon.disco.instrumentation.preprocess.IntegTestDelegation",
            "software.amazon.disco.instrumentation.preprocess.IntegTestDelegationVoid",
            "software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall"
    )

    classesToMove.forEach {
        // copy these compiled classes to destination dir while maintaining their namespaces.
        val name = it.replace('.', '/')
        from("build/classes/java/main/$name.class") {
            into("resources/${name.substringBeforeLast('/')}")
        }

        // exclude the original ones from this jar
        exclude("$name.class")
    }
}
dependencies {
    compileOnly(project(":disco-java-agent:disco-java-agent-api"))
    compileOnly(project(":disco-java-agent:disco-java-agent-core"))
    compileOnly(project(":disco-java-agent:disco-java-agent-plugin-api"))
}

