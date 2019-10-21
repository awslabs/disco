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
    id("com.github.johnrengelman.shadow")
}

dependencies {
    runtimeOnly(project(":disco-java-agent-web")) {
        //by setting this flag false, we take only what is described by the above project, and not its entire
        //closure of transitive dependencies (i.e. all of Core, all of Bytebuddy, etc)
        //this makes our generated Jar minimal, containing only our source files, and our manifest. All those other
        //dependencies are expected to be in the base agent, which loads this plugin.
        //Ideally we would have a test for this which inspects the final Jar's content, but it can be reviewed manually
        //on the command line with "jar -tf disco-java-agent-web-plugin.jar"
        isTransitive = false
    }

    //Test target is integ tests for this plugin. Some classes in the integ tests also self-test via little unit tests during this
    //testrun.
    testCompile(project(":disco-java-agent:disco-java-agent-api"))
    testCompile("junit", "junit", "4.12")
    testCompile("org.mockito", "mockito-core", "1.+")
    testCompile("javax.servlet", "javax.servlet-api", "3.0.1")
}

tasks.shadowJar  {
    manifest {
        attributes(mapOf(
            //this has to align with the contents of WebSupport.java. Would be good to find a way to avoid this duplication
            "Disco-Installable-Classes" to "com.amazon.disco.agent.web.servlet.HttpServletServiceInterceptor"
        ))
    }
}

//integ testing needs a loaded agent, and the loaded plugin
tasks.test {
    //explicitly remove the runtime classpath from the tests since they are integ tests, and may not access the
    //dependency we acquired in order to build the plugin, namely the disco-java-agent-web jar which makes reference
    //to byte buddy classes which have NOT been relocated by a shadowJar rule. Discovering those unrelocated classes
    //would not be possible in a real client installation, and would cause plugin loading to fail.
    classpath = classpath.minus(configurations.runtimeClasspath.get())

    //load the agent for the tests, and have it discover the web plugin
    jvmArgs("-javaagent:../../disco-java-agent/disco-java-agent/build/libs/disco-java-agent-0.1.jar=pluginPath=./build/libs:extraverbose")

    //we do not take any normal compile/runtime dependency on this, but it must be built first since the above jvmArg
    //refers to its built artifact.
    dependsOn(":disco-java-agent:disco-java-agent:build")
}

configure<PublishingExtension> {
    publications {
        named<MavenPublication>("maven") {
            artifact(tasks.jar.get())
        }
    }
}
