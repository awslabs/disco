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
}

val pluginImplementation by configurations.creating {}
configurations {
    testImplementation.extendsFrom(configurations.get("pluginImplementation"))
    compileOnly.extendsFrom(configurations.get("pluginImplementation"))
}

dependencies {
    implementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation("org.mockito", "mockito-core", "1.+")
    pluginImplementation("javax.servlet", "javax.servlet-api", "3.0.1")
    pluginImplementation("org.apache.httpcomponents", "httpclient", "4.5.10")
}

// For classes which need to be accessed in the context of the application code's classloader, they need to be injected/forced
// into that classloader. They cannot be placed in the bootstrap classloader, nor any isolated/orphaned classloader, since they
// either inherit from, or use, classes from the AWS SDK, which are assumed not to be present on the bootstrap classloader
ext.set("classesToMove", arrayOf(
        "software.amazon.disco.agent.web.servlet.HttpServletServiceMethodDelegation",
        "software.amazon.disco.agent.web.apache.event.ApacheEventFactory",
        "software.amazon.disco.agent.web.apache.event.ApacheHttpServiceDownstreamRequestEvent",
        "software.amazon.disco.agent.web.apache.httpclient.ApacheHttpClientMethodDelegation"
))