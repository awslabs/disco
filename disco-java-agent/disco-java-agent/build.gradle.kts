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
    implementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
    testImplementation(project(":disco-java-agent:disco-java-agent-inject-api"))
}

//prevent tests from importing core classes directly - only allow them to use the public api
configurations.testImplementation {
    exclude(module="disco-java-agent-core")
}

tasks.shadowJar  {
    //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (e.g. disco-java-agent-0.1.jar)
    archiveClassifier.set(null as String?)

    manifest {
        attributes(mapOf(
                "Premain-Class" to "software.amazon.disco.agent.DiscoAgent",
                "Agent-Class" to "software.amazon.disco.agent.DiscoAgent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true",
                "Boot-Class-Path" to archiveFileName.get()
        ))
    }
}

tasks.named<Test>("test") {
    //load the agent TWICE for the tests for agent deduplication (and allow debugging on the usual socket)
    //we also communicate the full path to the agent in a System variable
    val ver = project.version
    var agentPath = "./build/libs/disco-java-agent-$ver.jar"
    val agentArg = "-javaagent:$agentPath=extraverbose"
    jvmArgs("-agentlib:jdwp=transport=dt_socket,address=localhost:1337,server=y,suspend=n",
            "$agentArg:loggerfactory=software.amazon.disco.agent.TestLoggerFactory",
            agentArg,
            "-DdiscoAgentPath=$agentPath"
    )
}
