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

group = "disco"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    //TODO update BB and ASM to latest
    compile("net.bytebuddy", "byte-buddy-dep", "1.9.12")
    //don't need these for this module but needed for Core?
    //compile("net.bytebuddy", "byte-buddy-agent", "1.9.12")
    //compile("org.ow2.asm", "asm", "7.1")

    //this package has no tests, just interfaces
    //testCompile("org.mockito", "mockito-core", "1.+")
    //testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}