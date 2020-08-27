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
    // Compile against AWS SDK V2, but we do not take a runtime dependency on it
    compileOnly("software.amazon.awssdk", "sdk-core", "2.13.76")

    implementation(project(":disco-java-agent-aws:disco-java-agent-aws-api"))
    implementation(project(":disco-java-agent:disco-java-agent-core"))
    testImplementation("org.mockito", "mockito-core", "1.+")
    testImplementation("com.amazonaws", "aws-java-sdk-dynamodb", "1.11.840")
    testImplementation("com.amazonaws", "aws-java-sdk-sns", "1.11.840")
    testImplementation("com.amazonaws", "aws-java-sdk-sqs", "1.11.840")
    testImplementation("software.amazon.awssdk", "dynamodb", "2.13.76")
}
