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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    //prevent Core being accidentally available to tests at compile time
    compileOnly(project(":disco-java-agent:disco-java-agent-core"))
    runtime(project(":disco-java-agent:disco-java-agent-core"))

    testCompile("junit", "junit", "4.12")
    testCompile(project(":disco-java-agent:disco-java-agent-api"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<ShadowJar>  {
    //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-example-0.1.jar)
    archiveClassifier.set(null as String?)

    manifest {
        attributes(mapOf(
                "Premain-Class" to "com.amazon.disco.agent.example.Agent",
                "Agent-Class" to "com.amazon.disco.agent.example.Agent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true",
                "Boot-Class-Path" to archiveFileName.get()
        ))
    }

    //Must relocate both of these inner dependencies of the Disco agent, to avoid conflicts in your customer's application
    relocate ("org.objectweb.asm", "com.amazon.disco.agent.jar.asm")
    relocate ("net.bytebuddy", "com.amazon.disco.agent.jar.bytebuddy")
}

tasks {
    //once gradle has made its default jar, follow up by producing the shadow/uber jar
    assemble {
        dependsOn(shadowJar)
    }

    test {
        //by applying the Agent, we enable DiSCo's thread handoff support. commenting out the below line would cause the test to fail
        jvmArgs("-javaagent:"+jar.get().archiveFile.get().asFile)
    }
}