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

dependencies {
    //pull in the shadow configuration, so we only get the built jar, and not any transitive deps e.g. core
    //using the compileOnly dependency set so that it is invisible to tests
    compileOnly(project(":disco-java-agent-example", "shadow"))
    compileOnly(project(":disco-java-agent:disco-java-agent", "shadow"))
    compileOnly(project(":disco-java-agent-web:disco-java-agent-web-plugin", "shadow"))
    
    testCompile(project(":disco-java-agent:disco-java-agent-api"))
    testCompile("junit", "junit", "4.12")
    testCompile("javax.servlet", "javax.servlet-api", "3.0.1")
}

// Test 2 ways. Once using the example 'monolithic' agent defined in disco-java-agent-example, and then again using the 
// decoupled agent via the Plugin Discovery subsystem
val testViaPlugin = task<Test>("testViaPlugin") {
    jvmArgs("-javaagent:../disco-java-agent/disco-java-agent/build/libs/disco-java-agent-0.1.jar=pluginPath=../disco-java-agent-web/disco-java-agent-web-plugin/build/libs")
}

tasks {
    test {
        jvmArgs("-javaagent:../disco-java-agent-example/build/libs/disco-java-agent-example-0.1.jar")
    }
    check {
        dependsOn(testViaPlugin)
    }
}


