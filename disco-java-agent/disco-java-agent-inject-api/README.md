## What is it?

As mentioned in the top level README, when you are deploying to managed runtimes
such as AWS Lambda, it may not be possible to control the JVM arguments when invoking java.

For this circumstance, this small library is available for end-user code, containing a small
suite of utilities which can be used to inject an agent into an already-running application.

## API

For most practical purposes the loadAgent(String agentJarPath, String agentArgs) API is sufficient.

Calling this method, with a fully qualified path name of the Agent JAR, along with any
command line parameters to be passed to it, will cause the Agent to be injected, all
plugins loaded (if any), and all Installables installed if it still possible to do so.

## Caution

Disco Installables typically work on the basis of intercepting the bytecode of classes
in order to add extra aspect-oriented code to them. In some cases it is not possible to perform
this transformation if the class has already been loaded (see the restrictions listed at https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/Instrumentation.html#redefineClasses(java.lang.instrument.ClassDefinition...))

Care should be taken in client code to use the Inject API as soon as possible in the lifetime of the running code,
as close to the 'top of Main()' as possible basically.

## How it works

Using the VirtualMachine.attach() mechanism, the running application effectively attaches to itself, and then
uses the loadAgent() method of the VirtualMachine to install the agent. This library performs some extra work
when necessary to add discovered classes to specific classloaders, and to configure ByteBuddy to discover them.

https://docs.oracle.com/javase/7/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html

