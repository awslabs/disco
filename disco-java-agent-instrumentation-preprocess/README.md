# Disco Java Agent Instrumentation Preprocess Library
This folder contains the source code of the instrumentation preprocess library. The library can be invoked directly via the CLI 
or imported as a dependency for additional extensibility. 

# Instrumentation preprocess vs. runtime instrumentation
The purpose of this library is to shift instrumentation overhead from runtime to build-time. This is due to the fact that a
large of number of comparisons take place during service startup as a result of the jvm matching every single class to be loaded
against every single Installable installed. 

By shifting this overhead to build-time, delay in service startup time caused by installing a Disco Agent can be significantly 
reduced to just a few seconds.

# Types of jar files that the tool can instrument
- compiled code of the service itself
- dependencies
- JDK (8 and 9+)
    - for JDK 9, only statically instrumenting java.base.jmod is supported currently

# Usage
## CLI
### An example to use the CLI to instrument the JDK
`java -jar disco-java-agent-instrumentation-preprocess-<version>.jar -ap <path to a disco agent> -jdks <path to rt.jar or java.base.mod> -out <output dir>`

### An example to use the CLI to instrument a list of jar files
`java -cp disco-java-agent-instrumentation-preprocess-<version>.jar <dependency of the jars>(:<additional dependency>)* software.amazon.disco.instrumentation.preprocess.cli.Driver -ap <path to a disco agent> -jps <path to jar file> <additional path to jar file>* -out <output dir>`

>note that -cp is used instead of -jar because it is not possible to append additional entries to the classpath when executing the library jar directly
with -jar. All direct dependencies required by the jars to be transformed have to be supplied via -cp <path one>:<path two>. 
>For more information on how to use the CLI execute the jar with the [--help] flag.

## Import as library
### Simple example
```java
PreprocessConfig config = PreprocessConfig.builder()
    .agentPath(<path to the disco agent>)
    .jdkPath(<path to the jdk file>)
    .outputDir(<output directory>)
    .build();

StaticInstrumentationTransformer.builder()
    .agentLoader(new DiscoAgentLoader())
    .jarLoader(new JarLoader())
    .config(config)
    .build()
    .transform();
```

## Static instrumenting the JDK
### JAVA 8
`java -Xbootclasspath/p <path to instrumented rt.jar> Driver`

### JAVA 9 or higher
`java --patch-module java.base=<path to instrumented java.base.jmod> 
--add-exports java.base/software.amazon.disco.agent.logging=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.concurrent=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.interception=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.interception.templates=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.jar.bytebuddy.agent=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.jar.bytebuddy.agent.builder=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.jar.bytebuddy.matcher=ALL-UNNAMED
--add-exports java.base/software.amazon.disco.agent.jar.bytebuddy.dynamic=ALL-UNNAMED
Driver
`
>Since classes added to a module via --patch-module are module private by default, the list of export statements must be provided as options in order 
>for the java application to successfully startup and concurrency support to work. We are actively investigating better approaches to remove the need 
>for these exports or a way to simply it to a single statement. 

# Development

You may need to install the IntelliJ Lombok plugin for your project in order for the tests to pass.
Instructions can be found on [the plugin's readme](https://github.com/mplushnikov/lombok-intellij-plugin#installation).

