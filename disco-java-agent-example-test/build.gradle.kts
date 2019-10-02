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
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    //pull in the shadow configuration, so we only get the built jar, and not any transitive deps e.g. core
    testRuntime(project(":disco-java-agent-example", "shadow"))

    testCompile("junit", "junit", "4.12")
    testCompile(project(":disco-java-agent:disco-java-agent-api"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    test {
        //by applying the Agent, we enable DiSCo's thread handoff support. commenting out the below line would cause the test to fail
        jvmArgs("-javaagent:../disco-java-agent-example/build/libs/disco-java-agent-example-0.1.jar")
    }
}