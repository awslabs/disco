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