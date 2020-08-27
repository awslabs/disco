## Disco 'AWS' API Package

This package contains classes and interfaces which are extended and implemented by the `disco-java-agent-aws` library
as Disco Events. Consumers of Disco AWS Support can optionally depend on this package to cast Events received from the
event bus to these types and not lose any AWS-SDK-specific data from downcasting.

This package is included in the distributed `disco-java-agent-aws-plugin` JAR. If you are a consumer of the Disco Events
defined in this package produced from the AWS plugin, you can declare a `compileOnly` or `provided` dependency on this
package, as shown in these examples (assuming you've already declared a version with a dependency on the
`disco-toolkit-bom`).

In Gradle's default DSL:

```groovy
compileOnly 'software.amazon.disco:disco-java-agent-aws-api'
```

In Gradle's Kotlin DSL:

```kotlin
compileOnly("software.amazon.disco:disco-java-agent-aws-api")
```

Using Maven:

```xml
<dependency>
  <groupId>software.amazon.disco</groupId>
  <artifactId>disco-java-agent-aws-api</artifactId>
</dependency>
```
