/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

dependencies {
    // Compile against AWS SDKs, but we do not take a runtime dependency on them
    compileOnly("com.amazonaws", "aws-java-sdk-core", "1.11.840")
    compileOnly("software.amazon.awssdk", "sdk-core", "2.13.76")

    implementation(project(":disco-java-agent-aws:disco-java-agent-aws-api"))
    implementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation("org.mockito", "mockito-core", "1.+")
    testImplementation("com.amazonaws", "aws-java-sdk-dynamodb", "1.11.840")
    testImplementation("com.amazonaws", "aws-java-sdk-sns", "1.11.840")
    testImplementation("com.amazonaws", "aws-java-sdk-sqs", "1.11.840")
    testImplementation("software.amazon.awssdk", "dynamodb", "2.13.76")
}

// For classes which need to be accessed in the context of the application code's classloader, they need to be injected/forced
// into that classloader. They cannot be placed in the bootstrap classloader, nor any isolated/orphaned classloader, since they
// either inherit from, or use, classes from the AWS SDK, which are assumed not to be present on the bootstrap classloader
ext.set("classesToMove", arrayOf(
        "software.amazon.disco.agent.awsv2.DiscoExecutionInterceptor",
        "software.amazon.disco.agent.event.AwsServiceDownstreamRequestEventImpl",
        "software.amazon.disco.agent.event.AwsServiceDownstreamResponseEventImpl"
))