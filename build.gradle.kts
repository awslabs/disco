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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

//common features available to the entire project
//TODO specify the versions of ByteBuddy and ASM in here, since they are used in a few places.
plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}

subprojects {
    version = "0.11.0"

    repositories {
        mavenCentral()
    }

    // Set up creation of shaded Jars
    pluginManager.withPlugin("com.github.johnrengelman.shadow") {
        tasks {
            named<ShadowJar>("shadowJar") {

                //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-web-plugin-x.y.z.jar)
                archiveClassifier.set(null as String?)

                //Must relocate both of these inner dependencies of the Disco agent, to avoid conflicts in your customer's application
                relocate("org.objectweb.asm", "software.amazon.disco.agent.jar.asm")
                relocate("net.bytebuddy", "software.amazon.disco.agent.jar.bytebuddy")
            }

            //once gradle has made its default jar, follow up by producing the shadow/uber jar
            named<DefaultTask>("assemble") {
                dependsOn(named<ShadowJar>("shadowJar"))
            }
            named<ShadowJar>("shadowJar") {
                dependsOn(named<Jar>("jar"))
            }
        }
    }

    plugins.withId("java-library") {
        dependencies {
            add("testImplementation", "junit:junit:4.12")
            add("testImplementation", "org.mockito:mockito-core:3.+")
        }

        configure<JavaPluginConvention> {
            sourceCompatibility = JavaVersion.VERSION_1_8
        }

        // This block only applies to plugin modules, as determined by the existence of a "-plugin" suffix
        if (project.name.endsWith("-plugin")) {
            // Remove "-plugin" suffix to get corresponding library name
            val libraryName = ":" + project.name.subSequence(0, project.name.length - 7)
            val ver = project.version

            // Configure dependencies common to plugins
            dependencies {
                add("runtimeOnly", project(libraryName)) {
                    // By setting the isTransitive flag false, we take only what is described by the above project, and not
                    // its entire closure of transitive dependencies (i.e. all of Core, all of Bytebuddy, etc)
                    // this makes our generated Jar minimal, containing only our source files, and our manifest. All those
                    // other dependencies are expected to be in the base agent, which loads this plugin.
                    isTransitive = false
                }

                // Test target is integ tests for Disco plugins. Some classes in the integ tests also self-test via
                // little unit tests during testrun.
                add("testImplementation", project(":disco-java-agent:disco-java-agent-api"))
                add("testImplementation", "org.mockito:mockito-core:1.+")
            }

            // Configure integ tests, which need a loaded agent, and the loaded plugin
            tasks.named<Test>("test") {
                // explicitly remove the runtime classpath from the tests since they are integ tests, and may not access the
                // dependency we acquired in order to build the plugin, namely the library jar for this plugin which makes reference
                // to byte buddy classes which have NOT been relocated by a shadowJar rule. Discovering those unrelocated classes
                // would not be possible in a real client installation, and would cause plugin loading to fail.
                classpath = classpath.minus(configurations.named<Configuration>("runtimeClasspath").get())

                //load the agent for the tests, and have it discover the plugin
                jvmArgs("-javaagent:../../disco-java-agent/disco-java-agent/build/libs/disco-java-agent-$ver.jar=pluginPath=./build/libs:extraverbose")

                //we do not take any normal compile/runtime dependency on this, but it must be built first since the above jvmArg
                //refers to its built artifact.
                dependsOn(":disco-java-agent:disco-java-agent:build")
                dependsOn("$libraryName:${project.name}:assemble")
            }
        }
    }

    //we publish everything except example subprojects to maven. Projects which desire to be published to maven express the intent
    //by using the "maven-publish" plugin.
    //
    //TODO: apply some continuous integration rules to publish to Maven automatically when e.g. version number increases
    //
    //Publication to local Maven is simply "./gradlew publishToMavenLocal"
    plugins.withId("maven-publish") {
        plugins.apply("signing")

        plugins.withId("java-library") {
            //create a task to publish our sources
            tasks.register<Jar>("sourcesJar") {
                from(project.the<SourceSetContainer>()["main"].allJava)
                archiveClassifier.set("sources")
            }

            //create a task to publish javadoc
            tasks.register<Jar>("javadocJar") {
                from(tasks.named<Javadoc>("javadoc"))
                archiveClassifier.set("javadoc")
            }
        }

        // Disable publishing a bunch of unnecessary Gradle metadata files
        tasks.withType<GenerateModuleMetadata> {
            enabled = false
        }

        //defer maven publish until the assemble task has finished, giving time for shadowJar to complete, if it is present
        tasks.withType<AbstractPublishToMaven> {
            dependsOn(tasks.named<DefaultTask>("assemble"))
        }

        //all our maven publications have similar characteristics, declare as much as possible here at the top level
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {

                    // Define what artifact to publish depending on what plugin is present
                    // If shadow is present, we should publish the shaded JAR
                    // Otherwise, publish the standard JAR from compilation
                    plugins.withId("java-library") {
                        if (plugins.hasPlugin("com.github.johnrengelman.shadow")) {
                            artifact(tasks.named<Jar>("jar").get())
                        } else {
                            from(components["java"])
                        }

                        artifact(tasks["sourcesJar"])
                        artifact(tasks["javadocJar"])
                    }

                    // For publishing the BOM
                    plugins.withId("java-platform") {
                        from(components["javaPlatform"])
                    }

                    groupId = rootProject.name

                    pom {
                        name.set(groupId + ":" + artifactId)
                        description.set("Amazon Disco aspect oriented distributed systems comprehension toolkit")
                        url.set("https://github.com/awslabs/disco")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("connellp")
                                name.set("Paul Connell")
                                email.set("connellp@amazon.com")
                            }
                            developer {
                                id.set("armiros")
                                name.set("William Armiros")
                                email.set("armiros@amazon.com")
                            }
                            developer {
                                id.set("ssirip")
                                name.set("Sai Siripurapu")
                                email.set("ssirip@amazon.com")
                            }
                            developer {
                                id.set("liuhongb")
                                name.set("Hongbo Liu")
                                email.set("liuhongb@amazon.com")
                            }
                            developer {
                                id.set("besmithe")
                                name.set("Ben Smithers")
                                email.set("besmithe@amazon.co.uk")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/awslabs/disco.git")
                            developerConnection.set("scm:git:ssh://github.com/awslabs/disco.git")
                            url.set("https://github.com/awslabs/disco")
                        }
                    }
                }
            }

            repositories {
                maven {
                    url = uri("https://aws.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = "${findProperty("disco.sonatype.username")}"
                        password = "${findProperty("disco.sonatype.password")}"
                    }
                }
            }
        }

        configure<SigningExtension> {
            useGpgCmd()
            sign(the<PublishingExtension>().publications["maven"])
        }
    }
}
