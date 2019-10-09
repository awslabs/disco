[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## DiSCo Toolkit (Distributed Systems Comprehension)

A suite of tools including a framework for creating Java Agents, for aspect-oriented tooling for distributed systems.

## License

This library is licensed under the Apache 2.0 Software License.

## Building DiSCo
Run the following command in the root directory.
``./gradlew build``

** If you receive the following error **
```
Could not determine the dependencies of task ':disco-java-agent-example:shadowJar'.
> Could not resolve all files for configuration ':disco-java-agent-example:runtimeClasspath'.
   > path may not be null or empty string. path='null'
```

Please ensure that the default Java binary that is ran is Java 8. As a workaround, you may specify the `JAVA_HOME` to point to another version of Java.

Example: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home`