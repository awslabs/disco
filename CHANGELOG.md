# Change Log

## Version 0.13.0 - 03/09/2023

### Major new features

* Introduced parallel instrumentation preprocessing to lower time spent preprocessing
* Introduced instrumentation preprocessing artifact caching to enable configurable artifact caching strategies to prevent redundant preprocessing
* Added support for JDK-17 by enabling building patched copy of ByteBuddy dependency

### Minor new features

* Deprecated HeaderRetrievable in favor of Activity/Downstream x Request/Response events and added corresponding Apache HttpClient downstream Request/Response header events
* Implemented Killswitch file mechanism
* Added additional logging to indicate when Signed Jars are preprocessed
* Extended ServiceEvent API to support Event IDs
* Implemented AgentConfig override via properties file

### Bug fixes

* Fixed mvn mysql-connector-java coordinates
* Fixed (Scheduled)ThreadPoolExecutor task removal
* Removed thread id comparison on thread enter to allow TX propagation during async workflows

## Version 0.12.0 - 04/25/2022

### Major new features

* Introduced instrumentation preprocess feature under disco-java-agent-instrumentation-preprocess. Instrumentation preprocess addresses issues caused by runtime instrumentation overhead such as longer startup time by instrumenting the JDK and all the dependencies at build-time instead.
* Added `PluginClassLoader`. This is the new default class loader for loading Disco plugins.
* Added a new plugin to support transaction context (TX) propagation for Kotlin coroutines.

### Minor new features

* Implemented `TrieNameMatcher` to match class name using Trie data structure.
* Generated transaction ID using ThreadLocalRandom. The implementation is adapted from X-Ray SDK FastIdGenerator.
* Provided timing metric to measure how long it take for Disco agent to start.
* Disposed of Thread, ForkJoinPool, and ForkJoinTask interceptors after they are applied once. This is an optimization to bypass class matching for these named classes.

### Bug fixes

* Added check to prevent Disco agent to be loaded more than once.
* Cleaned up ThreadLocal transaction context when the thread ends or is pushed back into the pool.
* Fixed NPE when Thread is instantiated without a target.
* Fixed ClassCircularityError when running with security manager. This is a known problem with ByteBuddy, since the [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4557) also encountered it.
* Handled exceptions in Disco EventBus when listening to incoming events.
* Upgraded to the latest `net.bytebuddy:byte-buddy-dep-1.12.6` and `org.ow2.asm:asm-9.2`.

## Version 0.11.0 - 04/07/2021

* Added additional SQL interception support for prepared statements and calls [PR #16](https://github.com/awslabs/disco/pull/16)

## Version 0.10.0 - 08/25/2020

* Added SQL interception package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added AWS interception package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added instrumentation preprocess package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added Bill of Materials [PR #10](https://github.com/awslabs/disco/pull/10)
* Added installable `Package` class for collections of installables [PR #10](https://github.com/awslabs/disco/pull/10)
* Added concurrency support for `ScheduledThreadPoolExecutor` [PR #10](https://github.com/awslabs/disco/pull/10)
* Added Service downstream cancellation events [PR #10](https://github.com/awslabs/disco/pull/10)
* Added `HeaderReplaceable` interface for event classes [PR #10](https://github.com/awslabs/disco/pull/10)
* Added `removeMetadata` method for Transaction Context [PR #10](https://github.com/awslabs/disco/pull/10)
* Added support in core package for preprocess build tool [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed deprecated reflective access in `ForkJoinTask` tests [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed null pointer issues in `HttpResponseEvent` and `TransactionContext` [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed `ExecutorService` to use re-entrancy check [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed flaky TX tests [PR #10](https://github.com/awslabs/disco/pull/10)
* Ensure transaction context is propagated for nested executor submissions [PR #10](https://github.com/awslabs/disco/pull/10)
* Deprecated `MethodHandleWrapper` class [PR #10](https://github.com/awslabs/disco/pull/10)
* Upgraded ByteBuddy to 1.10.14 and ASM to 8.0.1 [PR #10](https://github.com/awslabs/disco/pull/10)
* Upgraded to Gradle 6.6 [PR #10](https://github.com/awslabs/disco/pull/10)

## Version 0.9.1 - 12/2/2019

* Initial commit of DiSCo Toolkit
