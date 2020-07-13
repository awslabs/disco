## Disco 'SQL' Service Support

Serving as both an example of how to author a Disco library/plugin, and also as a usable
Event producer for popular frameworks used in service oriented software, this subproject is laid out as follows:

1. In this folder, the Installables to intercept SQL interactions, and issue appropriate Event Bus Events.
1. In the disco-java-agent-sql-plugin subfolder, a proper Disco plugin, bundled as a plugin JAR file with Manifest.

## Package description

TODO

## Why the separation into two projects?

Disco supports two models of development:

1. Standalone, self-contained agents.
1. Pluggable, extensible agents.

If you only want to support the first of these, there's no need to produce the plugin MANIFEST or
to shade/shadow your JAR file. The final build of the Agent itself would do that all in a single step.

On the other hand, if you only wanted to support the Plugin model you could simply have all your source
code *and* your MANIFEST or MANIFEST-generation in a single project.

For the purposes of this library, especially since it serves as an example, we support both mechanisms,
hence the factoring into a lib project and a plugin JAR project.

You can see tests for both 'flavours' in the disco-java-agent-example-test project. Inside the
build.gradle.kts file there are two test targets

1. The familiar default 'test' target, which tests via the standalone disco-java-agent-example agent
1. An extra 'testViaPlugin' test target, which tests using the disco-java-agent canonical agent, and the
built disco-java-agent-sql-plugin plugin.
