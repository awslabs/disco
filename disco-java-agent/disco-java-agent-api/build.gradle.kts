plugins {
    java
}

version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("org.mockito", "mockito-core", "1.+")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}