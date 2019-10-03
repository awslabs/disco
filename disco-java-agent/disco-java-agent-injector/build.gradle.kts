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
    java
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    compile(project(":disco-java-agent:disco-java-agent-inject-api"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.shadowJar  {
    //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-injector-0.1.jar)
    archiveClassifier.set(null as String?)

    //TODO would be good to have some include/exclude rules here to really fine-tune what makes it into the injector
    //agent JAR file. Since it will all be added to the application classpath, it would be courteous to users to minimize.
    //Currently it will contain the Agent.class itself, along with the disco-java-agent-api contents in full
    //An alternative would be for the Agent JAR to only include itself, and confer responsibility onto the client, that
    //they must also have a runtime dependency on the disco-java-agent-api component.

    manifest {
        attributes(mapOf(
                "Premain-Class" to "com.amazon.disco.agent.injector.Agent",
                "Agent-Class" to "com.amazon.disco.agent.injector.Agent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
        ))
    }
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