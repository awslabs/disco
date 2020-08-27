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

dependencies {
    //TODO update BB and ASM to latest
    api("net.bytebuddy", "byte-buddy-dep", "1.10.14")
    implementation("org.ow2.asm", "asm", "8.0.1")
    implementation("org.ow2.asm", "asm-commons", "8.0.1")
    implementation("org.ow2.asm", "asm-tree", "8.0.1")
}
