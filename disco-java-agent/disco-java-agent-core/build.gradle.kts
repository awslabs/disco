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
    api(project(":disco-java-agent:disco-java-agent-plugin-api"))
    api(project(":disco-java-agent:disco-java-agent-inject-api"))
    api(project(":disco-java-agent:disco-java-agent-api"))
}

tasks.register<Delete>("deleteTestClass"){
    delete(files(
            "build/classes/java/test/software/amazon/disco/agent/plugin/source/ClassToBeInjectedAlpha.class",
            "build/classes/java/test/software/amazon/disco/agent/plugin/source/ClassToBeInjectedBeta.class")
    )

    dependsOn("createTestJar")
}

tasks.register<Zip>("createTestJar") {
    from("build/classes/java/test/software/amazon/disco/agent/plugin/source/ClassToBeInjectedAlpha.class"){
        into("resources/software/amazon/disco/agent/plugin/source")
    }

    from("build/classes/java/test/software/amazon/disco/agent/plugin/source/ClassToBeInjectedBeta.class"){
        into("resources/software/amazon/disco/agent/plugin/source")
    }

    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
    archiveFileName.set("test.jar")
    dependsOn("testClasses")
}

// This Jar will be statically instrumented by the Preprocess sub-project so that existing integ tests authored
// to test concurrency runtime instrumentation can be reused for testing build-time instrumentation.
tasks.register<Zip>("createDiscoCoreIntegTestsJar") {
    from("build/classes/java/integ"){}

    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
    archiveFileName.set("discoCoreIntegTests.jar")
    dependsOn("integtest")
}

tasks.test{
    dependsOn("deleteTestClass")
    jvmArgs("-agentlib:jdwp=transport=dt_socket,address=localhost:1337,server=y,suspend=n")
}

/**
 * Define a secondary set of tests, for testing the actual interceptions provided by the Installables.
 */
sourceSets {
    create("integ") {
        java {
            srcDir("src/integ/java")
        }
    }
}

//create a new empty integ test config - not extending from existing compile or testCompile, since we don't want to
//be able to compile against Core etc.
val integImplementation: Configuration by configurations.getting {}

dependencies {
    integImplementation("junit", "junit", "4.12")
    integImplementation(project(":disco-java-agent:disco-java-agent-api"))
}

val ver = project.version
val standardOutputLoggerFactoryFQN: String by rootProject.extra

val integtest = task<Test>("integtest") {
    testClassesDirs = sourceSets["integ"].output.classesDirs
    //explicitly remove the non-test runtime classpath from these tests since they are integ tests, and may not access the
    //dependencies e.g Core and ByteBuddy and so on during agent loading
    classpath = sourceSets["integ"].runtimeClasspath.minus(configurations.compileClasspath.get())

    //show logging with --info
    //gradle caches outputs, so needs a workaround to ensure stdout is always provided
    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
    }

    //apply the canonical agent which installs core interceptors
    jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,address=localhost:1337,server=y,suspend=n", "-javaagent:../disco-java-agent/build/libs/disco-java-agent-"+ver+".jar"+"=verbose:loggerfactory=${standardOutputLoggerFactoryFQN}")

    //try and coerce the runtime into giving the tests some parallelism to work with. The tests have a retry
    //policy to encourage them to work, but the runtime sometimes provides no threads in the thread pool
    systemProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 24)

    //no point running this if unit tests failed
    mustRunAfter(tasks["test"])

    //we need the agent to be built first
    dependsOn(":disco-java-agent:disco-java-agent:build")
}

//run the integ tests with every build. They can very occasionally fail due to concurrency uncertainty (e.g. requesting
//a parallelStream() is just that - a request. the runtime is under no obligation to actually execute the code in parallel
tasks.build {
    dependsOn(integtest)
    dependsOn(tasks["createDiscoCoreIntegTestsJar"])
}

