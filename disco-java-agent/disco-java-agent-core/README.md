## Disco Agent Core Library

The bulk of Disco's actual code resides in this Core library. It contains the following:
 
1. Runtime support for both the Event Bus and the Transaction Context - the two fundamental pillars of Disco Agents.
1. The scaffold and utilities for creating a Disco Agent, the starting point of which is the DiscoAgentTemplate class.
1. Default ignore rules for the interceptions provided by Installables.
1. Lightweight configuration, controlled currently by the Agent command line.
1. The Plugin Discovery subsystem for non-standalone decoupled Agents.
1. All of Disco's concurrency support, for ensuring Transaction Context coherency when multithreading is
used - effectively building an extension to the Java runtime at runtime.

Public APIs in the Core libraries are available for Agent and Plugin authors to use. However the library
is not intended for end-user code to consume. Agents and Plugins are designed to manipulate the package namespaces
of Disco's dependencies as a post-build step using JAR shading/shadowing. If client code were to consume Core
directly, runtime errors may be caused since the JVM's runtime view of software.amazon.disco.<something> may differ
from the compile-time view of it.

## Documentation and Examples

Please see the Javadoc produced by this project for a full explanation of the APIs
available to Agent and Plugin authors and see also the abundant example code and tests
elsewhere in the Disco GitHub repo.

The disco-java-agent/disco-java-agent and disco-java-agent-example projects
are both examples of Agents which use the relevant parts of Core for Agents; and
the disco-java-agent-web project relies on the essential portions of interest to a Plugin author.

## Tests

The code is well supported by tests, including the integration tests for the Concurrency interceptors/Installables.

However, as noted in the top-level README, there is some unfortunate unreliability around these tests since their
preconditions cannot easily be satisfied in user code.