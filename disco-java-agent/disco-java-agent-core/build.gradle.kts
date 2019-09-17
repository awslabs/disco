plugins {
    java
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    compile(project(":disco-java-agent-plugin-api"))
    compile(project(":disco-java-agent-api"))

    testCompile("org.mockito", "mockito-core", "1.+")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}