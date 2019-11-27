## Disco 'Web' Service Support Plugin

This is a plugin built from the source in the folder above, including a build rule
to output a well-formed Disco plugin.

### Manifest generation

The build.gradle.kts file contains a build rule to generate an appropriate MANIFEST

### Dependency shading

Inherited from a top level build.gradle.kts file in the top level project, the ByteBuddy and ASM
dependencies are repackaged in agreement with the expectations of the disco-java-agent.

### Integ Tests

The test target in build.gradle.kts is configured to apply the disco-java-agent via an argument given to the 
invocation of java, supplied to which is a pluginPath pointing to the output folder where the built
disco-java-agent-web-plugin plugin JAR file can be found. Without both of these, the tests will fail.