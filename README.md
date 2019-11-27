[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Actions Status](https://github.com/awslabs/disco/workflows/Java%20CI/badge.svg)](https://github.com/awslabs/disco/actions)

## The Amazon Disco Toolkit

A suite of tools including a framework for creating Java Agents, for aspect-oriented
tooling for distributed systems.

Disco is a sort-of acronym of **Di**stributed **S**ystems **Co**mprehension
and may be styled as DiSCo, Disco or disco. We really don't mind.

Right now, although intended eventually to encompass other tooling in a
similar space, Disco includes a library/framework/toolkit for creating
Java Agents aimed at solving operational tooling for distributed systems 
in service-oriented architectures.

### Pre-release software
| :stop_sign: &nbsp; Disco is pre-release software. As an author of agents or plugins, you may be subject to some churn or rework while we finalize the APIs and features. |
| --- |

Please note that whilst on versions less than semantic version 1.0, Disco is 
pre-release or preview software. Some APIs may be subject to change or removal until
a stable v1.0 release occurs.


### What does it do?

The Disco Java Agent toolkit provides 2 key runtime primitives, as an
extension on top of Java (or other JVM languages such as Kotlin, but Java
is currently the only language with robust tests).

1. An in-process Event Bus, which advertises moments in the lifetime of a
*transaction* or *activity* in service-oriented software. For example, a
service receiving a request via HTTP will begin a timeline of such events
at that time, concluding when it has finally delivered a response to its caller.
1. A Transactionally Scoped data dictionary called the TransactionContext.
By *Transactionally Scoped* we mean that this data store will be created
at the beginning of the activity lifetime as described above, and survive 
until it concludes. Extensions are added to the Java runtime such that this
data store will be consistent during thread handoffs.

These are explored in more detail below.

In future, more languages will be supported.

#### Event Bus

Think about a straw man API on a service-oriented system. Let's say we have
a service CityInfoService, with an API "getInfoForZipCode". It delegates to two downstream
services, WeatherService and TrafficService, whose responses it aggregates into a
response containing weather and traffic information for a given zip code. Since these
two services are orthogonal and not interdependent, it calls them in parallel.

The timeline of a call to getInfoForZipCode might be as follows:

1. At time T in Thread 0, a Servlet.service() call is made upon receipt of the getInfoForZipCode
API being called.
1. At time T+1 in Thread 0, the service submits two tasks to a pooled ExecutorService, to call
the two downstream services.
1. At time T+2 in Thread 1, a call to WeatherService is made.
1. At time T+2 in Thread 2, a call to TrafficService is made.
1. At time T+3 in Thread 1, the response from WeatherService is received.
1. At time T+4 in Thread 2, the response from TrafficService is received.
1. At time T+5 in Thread 0, Threads 1 and 2 are join()'d.
1. Finally, at time T+6 in Thread 0, a response is given to the service's caller.

The disco event bus issues events at all the key moments on this timeline of events.
At the time of each event, the TransactionContext (see below) is available to pass
data between the points at which each event is received and throughout this timeline
of events, the Transaction ID (again, see below) is consistent and unique. If two calls
to getInfoForZipCode were happening concurrently, and each had the same sequence of events
those events would agree on the content of the TransactionContext *within each activity*
with no crosstalk or confusion across the two.

Events are published for the upstream request and response, the downstream requests and responses
and each time a Thread is forked or joined.

#### Transaction Context

Consider when a service activity might call 3 *downstream* services,
parallelized in 3 threads from a thread pool. It may do this, for example,
by dispatching work to an ExecutorService.

One problem we have observed in tooling such as AWS X-Ray, due to deficiencies
in Java, is that these worker threads have no clear concept of 'caller' or 'parent'. 
Using Disco, the Java runtime is extended to ensure that the concept of caller/parent
is passed from thread to thread at the time of thread handoff (e.g. when calling Thread.start(),
or someExecutor.submit(), or when using a Java 8 parallel stream), by the 'forking' thread
giving the 'forked' thread access to its Transaction Context data store.

By default, upon creation, the Transaction Context always contains a randomly 
allocated UUID as a Transaction ID. This can be overridden by plugins or 
client code if desirable, and any other arbitrary data may also be added at
any point in the lifetime of the service activity. Once the data is placed
in the Transaction Context, it becomes available across the activity's family
of threads thereafter.

## What kind of uses does an Agent built on disco have?

Let's start with a simple example of a logger. In our CityInfoService above, it may be
challenging to produce really good 'joined-up' logs due to the concurrent behavior of the service.
When logging the calls to WeatherService and TrafficService, the threads appear
'orphaned'. If you've ever tried to make sense of logs by looking at 'nearby timestamps'
instead of having unambiguous IDs available, this will be a familiar problem.

So the simplest example, is to create a Listener on the disco Event Bus which logs
every event, along with the ID from the Transaction Context. Now without taking any
particular special action in the service's business logic itself, all these lines of log
can be tied together via this ID.

Another common requirement is to pass metadata (perhaps via HTTP headers) from service to 
service, when creating tracing our routing tools in service-oriented architectures.
Incoming request events and outgoing request events provide a facility to inspect, and
manipulate HTTP headers. The AWS X-Ray agent uses this feature to propagate its segments
across service call hops, without user-intervention.

## Creating a Java Agent

There are two basic ways to create a Java Agent using Disco. As a self-contained
artifact, or using a plugin-based system to allow multi-tenancy.

See the subproject disco-java-agent-example as a simple example of building
a self contained agent, along with the associated tests for it in disco-java-agent-example-test

Alternatively, the plugin-based approach may be seen in the disco-java-agent-web-plugin
project, which uses the canonical agent found in the disco-java-agent/disco-java-agent package
as a substrate for plugin discovery.

## How to install a Java Agent once created

As can be see in the build.gradle.kts files of several subprojects (e.g. the tests for
disco-java-agent-example), a single argument needs to be passed to the Java process
at start-up. See AgentConfigParser.java in the Core libraries for details on the command line
arguments for agents, and see PluginDiscovery.java for details on how a substrate agent
may load plugins.

## Using Java Agents on managed runtimes such as Lambda

One complexity for some managed runtimes is that the user does not have complete
authority over the arguments passed to Java. To remedy this, it is possible to 'Inject'
a Disco agent at runtime, using a tiny (1 or 2 lines) amount of boilerplate code.
An example of this is given in the disco-java-agent-example-inject-test project.
If using this method, care must be taken to perform the injection as early as possible
in the target software's lifetime (first line of Main() is ideal, as soon as possible
after that is the next best). Disco works on the basis of extending the Java runtime
via aspect-oriented bytecode manipulation, and some of these treatments are unable to work
if the instrumented class has already been used.

## License

This library is licensed under the Apache 2.0 Software License.

## Building Disco

The simplest way to start is to run the following command in the root directory.
``./gradlew build``

This will build all the code, and run all the tests (functional tests and integration tests).

The final tests which are executed are tests for the 'thread handoff' TransactionContext
propogation mentioned elsewhere in this README, which deserve a mention. Some of the tests are naturally
'flaky'. This is true because when we request, for example, someCollection.parallelStream(), and then perform
work, the test is not in control of *whether that will actually be parallel or actually be serial*. The
Java runtime is in charge and is not easily manipulated. If the Java runtime elects not to parallelize,
our test becomes meaningless - we cannot assert that disco's thread hand-off behavior is
correct if no thread hand-off occurred at all. So these tests are designed to retry. To be clear they don't
stubbornly "retry until they succeed". They retry until *preconditions are met which they do not control*.

Unfortunately this can mean that they *sometimes* fail and require restarting. We don't know a better way, sorry.

### Including Disco as a dependency in your product

Until we publish to Maven, you can run ``./gradlew publishToMavenLocal``, and consume from your local Maven cache
in your Maven or Gradle builds with e.g:

<br/>

#### Using Maven coordinates
```xml
<dependency>
  <groupId>software.amazon.disco</groupId>
  <artifactId>disco-java-agent-api</artifactId>
  <version>0.9.1</version>
</dependency>
```

#### Using Gradle's default DSL
```groovy
repositories {
  mavenLocal()
}

compile 'software.amazon.disco:disco-java-agent-api:0.9.1'
```

#### Using Gradle's Kotlin DSL
```kotlin
repositories {
  mavenLocal()
}

compile("software.amazon.disco", "disco-java-agent-api", "0.9.1")
```

### Troubleshooting

If you receive the following error
```
Could not determine the dependencies of task ':disco-java-agent-example:shadowJar'.
> Could not resolve all files for configuration ':disco-java-agent-example:runtimeClasspath'.
   > path may not be null or empty string. path='null'
```

Please ensure that the default Java binary that is ran is Java 8. As a workaround, you may specify the `JAVA_HOME` to point to another version of Java.

Example: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home`

## What's with all the subprojects?

Disco is quite componentized, added to which there are quite a few projects which serve as examples and tests.

Browse through the READMEs included with the subprojects to make sense of it all, but in summary there are
a few layers and families of projects in here:

1. Public API contained in disco-java-agent-api
1. Special API for implementors of interception plugins, in disco-java-agent-plugin-api
1. Core library, for consumption by plugin authors and agent authors (not client code) in disco-java-agent-core
1. Canonical Pluggable agent, capable of plugin discovery, in disco-java-agent/disco-java-agent
1. A facility to 'Inject' a Disco Agent into managed runtimes like AWS Lambda
1. A Plugin to support Servlets and Apache HTTP clients, in disco-java-agent-web-plugin
1. Example code in anything with '-example' in the project name.
1. Tests in anything with '-test' in the project name.

Subprojects themselves also encapsulate their own tests.