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
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":disco-java-agent:disco-java-agent-api"))
    implementation("javax.servlet", "javax.servlet-api", "3.0.1")

    testImplementation(project(":disco-java-agent:disco-java-agent-inject-api", "shadow"))
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
}

//make sure the agents and plugins we use have been built, without taking any real dependencies on them
//which would add them to classpaths and so on
tasks {
    compileJava {
        dependsOn(":disco-java-agent-web:disco-java-agent-web-plugin:build")
        dependsOn(":disco-java-agent:disco-java-agent:build")
    }
}
