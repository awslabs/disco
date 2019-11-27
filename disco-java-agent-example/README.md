## Disco Java Agent Example

Whilst the disco-java-agent/disco-java-agent subproject is a canonical example
of a Pluggable agent (and this is the recommended way to build on Disco), it is also
possible to build self-contained standalone agents. This is an example of such an agent,
containing all the Core and Web Installables.

## Reasons to prefer Pluggable Agents

When building a standalone agent, typically the Disco runtime is included in the
final shaded/shadowed Jar. In a situation where you wish to have more than one
Disco product (e.g. a Logger and a Throttler, for example), having two standalone
agents means that you incur the cost of all the Installables in both agents twice, as well
as the two JARs having duplicates of the Disco, ByteBuddy and ASM runtimes which might
have unpredictable consequences.

## Building a standalone Agent

Three post-build steps are necessary for a standalone Agent

1. Shading/shadowing all dependencies
1. Packaging the agent code, and the dependencies' code into a final fat JAR file.
1. Producing an appropriate MANIFEST file according to https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html

The Shading and packaging can be seen in the rules inherited from build.gradle.kts in the root project.
The MANIFEST creation is in the build.gradle.kts in this package.

## Tests

This standalone Agent is (lightly) tested via the disco-java-agent-example-test package's Gradle 'test' target.
The test related to Threading succeeds due to the presence of the Core Concurrency interceptors alongside the
appropriate MANIFEST attributes. The Servlet interception test succeeds as a result of the presence of the Web Package.