## Explanation 
For Plugins which describe a new *interception* treatment (see the javadoc for PluginDiscovery.java)
the interface to implement is contained in this separate small package.

The reason for this is that the Installable class takes a dependency on the ByteBuddy
(and therefore ASM) dependencies of disco. These always have their FQDNs rewritten
via JAR shading/shadowing such that they do not conflict with the same libraries if
they are present in user code.

So this small Interface/API package is separate from the disco-java-agent-api package at large, which
is intended to be publicly consumable, by any plugin and by user code if appropriate.

If user-code were to take an un-shadowed dependency on Installable.java, it could cause
ClassNotFoundErrors at runtime, since there would be two possible versions of Installable.class
on the classpath, one referring to the unshaded version of its dependencies, and the other
correctly shaded.

This package must only be consumed, therefore, by plugins which create Installables, and disco's internals.
It must never be consumed by end-user code.

## Example of use

1. In the disco-java-agent-core library, the interceptors in the software.amazon.disco.agent.concurrent namespace
are implementors of this interface.
1. In the disco-java-agent-web library, its interceptors are also implementors of this interface.

If you build either the disco-java-agent-example-agent or the disco-java-agent-web-plugin projects
and then unpack the resultant JAR files (which have been subject to shadowing/shading) you will see
that Installable.class and implementors of the Installable interface have been altered to depend on
e.g. software.amazon.disco.agent.jar.bytebuddy.* classes, instead of the same classes in their native
namespace of net.bytebuddy.*