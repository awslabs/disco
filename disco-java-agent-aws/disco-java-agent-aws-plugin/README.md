## Disco 'AWS' Service Support Plugin

This is a plugin built from the source in the folder above (and the source in API sibling module), including a build 
rule to output a well-formed Disco plugin.

### Manifest generation

The build.gradle.kts file contains a build rule to generate an appropriate MANIFEST

### Dependency shading

Inherited from a top level build.gradle.kts file in the top level project, the ByteBuddy and ASM
dependencies are repackaged in agreement with the expectations of the disco-java-agent.

This plugin includes the `disco-java-agent-aws-api` package in its artifact so that it can be fully standalone.
If you would like to use the AWS API package and this one, ensure you declare a `compileOnly` dependency on it to
avoid the API classes appearing twice on the runtime classpath.

### Integ Tests

The test target in build.gradle.kts is configured to apply the disco-java-agent via an argument given to the 
invocation of java, supplied to which is a pluginPath pointing to the output folder where the built
disco-java-agent-aws-plugin plugin JAR file can be found. Without both of these, the tests will fail.