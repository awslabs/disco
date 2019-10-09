import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

//common features available to the entire project
//TODO specify the versions of ByteBuddy and ASM in here, since they are used in a few places.
//TODO think about maven-publish, and what we need to include in every single pom
//TODO encapsulate the 'build shadowJar after normal jar' task rule which is duplicated for all agents and plugins at the moment
plugins {
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
    java
}

subprojects {
    apply<JavaPlugin>()

    version = "0.1"

    repositories {
        mavenCentral()
    }

    dependencies {
        testCompile("org.mockito", "mockito-core", "1.+")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
        pluginManager.withPlugin("com.github.johnrengelman.shadow") {
            named<ShadowJar>("shadowJar") {
                //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-web-plugin-0.1.jar)
                archiveClassifier.set(null as String?)
    
                //Must relocate both of these inner dependencies of the Disco agent, to avoid conflicts in your customer's application
                relocate("org.objectweb.asm", "com.amazon.disco.agent.jar.asm")
                relocate("net.bytebuddy", "com.amazon.disco.agent.jar.bytebuddy")
            }
        }
    }
}