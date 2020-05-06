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
    api(project(":disco-java-agent:disco-java-agent-plugin-api"))
    api(project(":disco-java-agent:disco-java-agent-inject-api"))
    api(project(":disco-java-agent:disco-java-agent-api"))
}

configure<PublishingExtension> {
    publications {
        named<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

/**
 * Define a secondary set of tests, for testing the actual interceptions provided by the Installables.
 */
sourceSets {
    create("integtest") {
        java {
            srcDir("src/integ/java")
        }
    }
}

//create a new empty integ test config - not extending from existing compile or testCompile, since we don't want to
//be able to compile against Core etc.
val integtestImplementation: Configuration by configurations.getting {}

dependencies {
    integtestImplementation("junit", "junit", "4.12")
    integtestImplementation(project(":disco-java-agent:disco-java-agent-api"))
}

val ver = project.version

val integtest = task<Test>("integtest") {
    testClassesDirs = sourceSets["integtest"].output.classesDirs
    //explicitly remove the non-test runtime classpath from these tests since they are integ tests, and may not access the
    //dependencies e.g Core and ByteBuddy and so on during agent loading
    classpath = sourceSets["integtest"].runtimeClasspath.minus(configurations.compileClasspath.get())

    //show logging with --info
    //gradle caches outputs, so needs a workaround to ensure stdout is always provided
    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
    }

    //apply the canonical agent which installs core interceptors
    jvmArgs = listOf("-javaagent:../disco-java-agent/build/libs/disco-java-agent-"+ver+".jar")

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
}

