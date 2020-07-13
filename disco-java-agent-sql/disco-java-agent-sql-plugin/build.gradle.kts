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
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // TODO: Refactor this block and other common plugin build logic to top-level build.gradle after deciding
    // a safe way to check whether a subproject represents a plugin
    runtimeOnly(project(":disco-java-agent-sql")) {
        //by setting this flag false, we take only what is described by the above project, and not its entire
        //closure of transitive dependencies (i.e. all of Core, all of Bytebuddy, etc)
        //this makes our generated Jar minimal, containing only our source files, and our manifest. All those other
        //dependencies are expected to be in the base agent, which loads this plugin.
        //Ideally we would have a test for this which inspects the final Jar's content, but it can be reviewed manually
        //on the command line with "jar -tf disco-java-agent-sql-plugin.jar"
        isTransitive = false
    }

    //Test target is integ tests for this plugin. Some classes in the integ tests also self-test via little unit tests during this
    //testrun.
    testImplementation(project(":disco-java-agent:disco-java-agent-api"))
    testImplementation("org.mockito", "mockito-core", "1.+")
}

tasks.shadowJar  {
    manifest {
        attributes(mapOf(
            "Disco-Installable-Classes" to "software.amazon.disco.agent.sql.SQLSupport"
        ))
    }
}

val ver = project.version

//integ testing needs a loaded agent, and the loaded plugin
tasks.test {
    //explicitly remove the runtime classpath from the tests since they are integ tests, and may not access the
    //dependency we acquired in order to build the plugin, namely the disco-java-agent-sql jar which makes reference
    //to byte buddy classes which have NOT been relocated by a shadowJar rule. Discovering those unrelocated classes
    //would not be possible in a real client installation, and would cause plugin loading to fail.
    classpath = classpath.minus(configurations.runtimeClasspath.get())

    //load the agent for the tests, and have it discover the web plugin
    jvmArgs("-javaagent:../../disco-java-agent/disco-java-agent/build/libs/disco-java-agent-"+ver+".jar=pluginPath=./build/libs:extraverbose")

    //we do not take any normal compile/runtime dependency on this, but it must be built first since the above jvmArg
    //refers to its built artifact.
    dependsOn(":disco-java-agent:disco-java-agent:build")
    dependsOn(":disco-java-agent-sql:disco-java-agent-sql-plugin:assemble")
}

configure<PublishingExtension> {
    publications {
        named<MavenPublication>("maven") {
            artifact(tasks.jar.get())
        }
    }
}
