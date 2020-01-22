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
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
    java
    `maven-publish`
}

//This is not a subproject that contains code, nor produces any artifacts. Disable the jar task
//to prevent a useless empty jar file being produced as a build side effect.
tasks {
    named<Jar>("jar") {
        setEnabled(false)
    }
}

subprojects {
    apply<JavaPlugin>()

    version = "0.9.1"

    repositories {
        mavenCentral()
    }

    dependencies {
        testCompile("org.mockito", "mockito-core", "1.+")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
        pluginManager.withPlugin("com.github.johnrengelman.shadow") {
            named<ShadowJar>("shadowJar") {

                //suppress the "-all" suffix on the jar name, simply replace the default built jar instead (disco-java-agent-web-plugin-x.y.z.jar)
                archiveClassifier.set(null as String?)
    
                //Must relocate both of these inner dependencies of the Disco agent, to avoid conflicts in your customer's application
                relocate("org.objectweb.asm", "software.amazon.disco.agent.jar.asm")
                relocate("net.bytebuddy", "software.amazon.disco.agent.jar.bytebuddy")
            }

            //once gradle has made its default jar, follow up by producing the shadow/uber jar
            assemble {
                dependsOn(named<ShadowJar>("shadowJar"))
            }
            named<ShadowJar>("shadowJar") {
                dependsOn(jar)
            }
        }
    }

    //we publish everything except example subprojects to maven. Projects which desire to be published to maven express the intent
    //via a property called simply 'maven' in their gradle.properties file (if it exists at all).
    //Each package to be published still needs a small amount of boilerplate to express whether is is a 'normal'
    //library which expresses its dependencies, or a shadowed library which includes and hides them. For a normal one
    //e.g. Core or Web, that boilerplate may look like:
    // configure<PublishingExtension> {
    //     publications {
    //         named<MavenPublication>("maven") {
    //             from(components["java"])
    //         }
    //     }
    // }
    //
    // whereas a shadowed artifact would be declared along the lines of:
    //
    // configure<PublishingExtension> {
    //     publications {
    //         named<MavenPublication>("maven") {
    //             artifact(tasks.jar.get())
    //         }
    //     }
    // }
    //
    //which declares the jar and the jar alone as the artifact, suppressing the default behaviour of gathering dependency info
    //
    //TODO: find a way to express this just once in the block below, probably by inspecting the existence or absence
    //of the shadow plugin, or the ShadowJar task. So far attempts to consolidate this logic have not succeeded, hence
    //the current need for the above boilerplate.
    //
    //TODO: apply some continuous integration rules to publish to Maven automatically when e.g. version number increases
    //
    //Publication to local Maven is simply "./gradlew publishToMavenLocal"
    if (hasProperty("maven")) {
        apply(plugin = "maven-publish")

        //create a task to publish our sources
        tasks.register<Jar>("sourcesJar") {
            from(sourceSets.main.get().allJava)
            archiveClassifier.set("sources")
        }

        //create a task to publish javadoc
        tasks.register<Jar>("javadocJar") {
            from(tasks.javadoc)
            archiveClassifier.set("javadoc")
        }

        //defer maven publish until the assemble task has finished, giving time for shadowJar to complete, if it is present
        tasks.withType<AbstractPublishToMaven> {
            dependsOn(tasks.assemble)
        }

        //all our maven publications have similar characteristics, declare as much as possible here at the top level
        configure<PublishingExtension> {
            repositories {
                maven {
                }
            }
            publications {
                create<MavenPublication>("maven") {
                    artifact(tasks["sourcesJar"])
                    artifact(tasks["javadocJar"])

                    groupId = "software.amazon.disco"

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
                        }
                        scm {
                            connection.set("scm:git:git://github.com/awslabs/disco.git")
                            developerConnection.set("scm:git:ssh://github.com/awslabs/disco.git")
                            url.set("https://github.com/awslabs/disco")
                        }
                    }
                }
            }
        }
    }
}

