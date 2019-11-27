## Disco 'Web' Service Support

Serving as both an example of how to author a Disco library/plugin, and also as a usable
Event producer for popular frameworks used in service oriented software, this subproject is layed out as follows:

1. In this folder, the Installables to intercept Servlet and Apache Client interactions, and issue appropriate Event Bus Events.
1. In the disco-java-agent-web-plugin subfolder, a proper Disco plugin, bundled as a plugin JAR file with Manifest.

### Feature status

| Feature | Status | Event generated |
| --- | --- | --- |
| Servlet requests | :heavy_check_mark: | HttpServletNetworkRequestEvent |
| Servlet response | :heavy_check_mark: | HttpServletNetworkResponseEvent |
| Apache Client request | :heavy_check_mark: | HttpServiceDownstreamRequestEvent |
| Apache Client response | :heavy_check_mark: | HttpServiceDownstreamResponseEvent |
| Apache Async Client request | :heavy_multiplication_x: | N/A | 
| Apache Async Client response | :heavy_multiplication_x: | N/A | 

| Event | Feature | Status |
| --- | --- | --- |
| HttpServiceDownstreamRequestEvent | Header insertion | :heavy_check_mark: |

## Package description

WebSupport.java implements the Package interface, and is a way for a standalone agent to gather all
the Installables that the library defines, without having to know the quantity of them, or their particular
class names.

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
built disco-java-agent-web-plugin plugin.
