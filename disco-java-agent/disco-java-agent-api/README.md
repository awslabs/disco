## Disco Java Agent Public API

The public API is available (unlike e.g. the disco-java-agent-plugin-api) for end-user code, as well
as also being consumed by plugins of all varieties, and the core libraries themselves. It's the lowest-down
component in all of Disco. It has no dependencies on any third party offerings, and is completely self-contained.

### Events

As introduced in the top-level README, the Events, published by interceptor/Installable plugins,
and consumed by Listener plugins, are a fundamental pillar of Disco's runtime.

All Events inherit from a simple base Event interface, containing only two methods.

On top of this is a hierarchy of events which we curate inside of Disco, encompassing network events,
concurrency events, transaction lifetime events, and service call events. These are intended to be a
'good' set of events on top of which builders can get started. Owners of plugins are free to implement their
own, so long as both producers (Installables) and consumers (Listeners) speak a common language.

#### The 'Origin' field

Each Event supplies an Origin field, which is something like 'Concurrency' for Thread events, something like "Web"
or "Apache" for HTTP events, specified by the author/owner of the Installable which is producing the Event.

These are not a strong contract between producer and consumer. They may be considered a 'hint' or a piece of 'info'
at best. They will probably be removed in a future release (post 1.0) of Disco, and should likely not be relied upon
for business logic. Consider this a piece of pre-release tech debt.

### Event Bus Listeners

Installables generally produce Events, which are published to the Event bus. On the other side of the
equation, registered "Listeners" receive all published Events, and are free to ignore or take action
accordingly.

A Disco Event Bus Listener must provide two implemented methods.

1. void listen(Event e) is called for every Listener for every Event. Perform instanceof checks to short-circuit
events of no interest, and filter more finely based on event content via their public APIs.
2. int getPriority() is used to declare an orderliness of Listeners. In the general case, Listeners should be
orthogonal and unaware of each other, so simply returning 0 is sufficient. If a product employs multiple listeners
which collaborate somehow, this may be used to guarantee orderliness. The extremes of Java's Integer range are 
unavailable for use as they are reserved for any system/internal Listeners which may wish to be guaranteeably before
or after any user Listeners.

An Event is not done being published until all Listeners have returned from their listen() methods, since this
is an in-process synchronous/realtime bus. Listeners must take care to process their listen() method as fast as possible.
If heavy processing is required, consider building abstractions on top of the Listener, to defer work for later, or
to throttle/drop work when under duress.

### Reflective Agent API

It's sometimes desirable to be able to interact with Disco's runtime, if a Disco agent is present.

For example, in client service code there may be a particular API where the service owners
can elect a semantically meaningful value for the TransactionContext Transaction ID, instead of
using the default ID. If this service owner had a logging Agent built on disco, that might be
a good tool for 'joining up' lines of log coming from various threads, but being able to have each
line of log use a more suitable ID would be a benefit. In this case, client code may call:

```java
import software.amazon.disco.agent.reflect.TransactionContext;

//...

    TransactionContext.set(myBetterId);
```

Internally this will cause a reflective call into the Agent *if present* and perform the required action
of setting the Transaction ID.

If the agent is *not present* this safely degrades to a no-op. Thus, code like this may be interspersed
in client code, without needing any surround "if (agentIsPresent)" checks, and can be used with relative impunity
even if there is e.g. a production stage which has no Disco agent present.

You can also use the EventBus API to publish Events, or attach Listeners to the EventBus.

Think of the 'reflect' APIs as a small 'curated' set of what is available via the full disco-java-agent-core APIs.

Note that it is not safe for end-user code to consume 'core' directly. Built agents and their plugins have
rewritten the package namespaces of dependent classes using JAR shading/shadowing techniques. Consuming the core
library without doing so will produce runtime conflicts between compile time and runtime class definitions. Furthermore
even if you took steps to solve this by shading/shadowing the core libraries, you would now be dependent on the
existence of a Disco agent at all times. Using the reflect APIs means you can forget about checking for the Agent's
presence or absence.

See the Javadoc inside the 'reflect' package for the full API.

### Logging callbacks

Disco has a strict 'no dependencies' policy. It is not up to Disco to demand that you use one logging framework
or another. So by default, Disco logs to a Null implementation of a 'Logger' interface. If you wish to see the 
logs produced internally by Disco, you may supply a LoggerFactory, which factories such Loggers.

There is a "STDOUT" example of a LoggerFactory in the "reflect.logging" package within this project.

You may install an available LoggerFactory two ways:

1. By passing the "loggerfactory" command line argument to the Agent when loading it (see AgentConfigParser.java)
1. By calling the Logger.installLoggerFactory() method directly from your code. 
