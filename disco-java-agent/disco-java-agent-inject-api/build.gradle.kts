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
    id("com.github.johnrengelman.shadow")
}

dependencies {
    //we use the ByteBuddyAgent for an install-after-startup injection strategy, but do not want to inadvertently
    //pull all of BB into the client's code.
    compile("net.bytebuddy", "byte-buddy-agent", "1.9.12") {
        isTransitive = false
    }

    testCompile("net.bytebuddy", "byte-buddy-dep", "1.9.12")
    testCompile("junit", "junit", "4.12")
}

tasks.shadowJar  {
    //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-inject-api-0.1.jar)
    archiveClassifier.set(null as String?)

    //Must relocate both of these inner dependencies of the Disco agent, to avoid conflicts in your customer's application
    relocate("org.objectweb.asm", "com.amazon.disco.agent.jar.asm")
    relocate("net.bytebuddy", "com.amazon.disco.agent.jar.bytebuddy")
}

tasks {
    //once gradle has made its default jar, follow up by producing the shadow/uber jar
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        dependsOn(jar)
    }
}