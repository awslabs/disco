## Disco's Pluggable Java Agent

This subproject contains a complete pluggable Disco agent. This may be used as-is
as a substrate for plugins, or forked if you need to customize it for your site, organization,
or usage.

## Plugin system

Plugins are JAR files which contain any Disco Installables or Listeners, as well
as optionally one-off initialization code which is not a good fit for being contained
within an Installable or a Listener.

The PluginDiscovery subsystem searches for plugins on the path provided to the "pluginPath" config
parameter, typically passed on the command line when the agent is provided to java.

Each Plugin advertises itself via the MANIFEST file inside the JAR. As an example:

<br/>

```manifest
Disco-Installable-Classes: com.my.org.SomeInterceptor com.my.org.SomeOtherInterceptor com.my.org.SomePackageOfInterceptors
Disco-Listener-Classes: com.my.org.SomeListener com.my.org.SomeOtherListener
Disco-Init-Class: com.my.org.SomeClassWithAnInitMethod
Disco-Classloader: system
```

<br/>
<br/>

| Entry | Meaning |
| --- | --- |
| `Disco-Installable-Classes` | A space-separated list of fully qualified class names which are expected to inherit from either software.amazon.disco.agent.interception.Installable or software.amazon.disco.agent.interception.Package, and have a no-args constructor. Installables will be processed first, across all scanned plugins |
| `Disco-Init-Class` | If any further one-off-initialization is required, a fully qualified class may be provided. If this class provides a method matching the signature "public static void init(void)", that method will be executed. All plugins will have this init() method processed *after* all plugins have had their Installables processed. |
| `Disco-Listener-Classes` | A space-separated list of fully qualified class names which are expected to inherit from Listener and have a no-args constructor. Listener registration for all plugins will occure after one-off initialization for all plugins
| `Disco-Bootstrap-Classloader` | *(Deprecated)* If set to the literal case-insensitive string 'true', this JAR file will be added to the runtime's bootstrap classloader. Any other value, or the absence of this attribute, means the plugin will be loaded via the system classloader like a normal runtime dependency. It is not usually necessary to specify this attribute, unless Installables wish to intercept JDK classes. If the `Disco-Classloader` attribute is present, this attribute will be ignored. |
| `Disco-Classloader` | Accepts three valid values: 'bootstrap', 'system', or 'plugin'. The 'bootstrap' value will add this JAR to the runtime's bootstrap classloader. The 'system' value specifies this JAR should be loaded in via the system classloader like a normal runtime dependency. The 'plugin' option specifies the JAR will be loaded into its own PluginClassLoader with the bootstrap classloader as its parent. The current default classloader Disco loads the JAR into is the system classloader.  
The order of events is that all Installables from all Plugins will be gathered first
and installed first. After which all Plugins have their Init class init() method called (if present),
and finally all Listeners are installed with the EventBus.

### Types of plugins

As indicated by the MANIFEST content above, there are 3 essential 'types' of plugins. Installables, 
Listeners, and 'Arbitrary' one-shot behavior. It's not generally expected that the 3rd type exists in isolation, but
rather the init() method would exist alongside e.g. a suite of Listeners.

#### Installables 

An Installable has one primary method to implement, the install() method, taking a ByteBuddy AgentBuilder as
an argument, which is provided by Disco's core (although you are not obligated to use it, if you know better).
Documenting ByteBuddy is out of scope for this document, but see examples in the software.amazon.disco.agent.concurrency
and software.amazon.disco.agent.web packages for examples, and see the ByteBuddy documentation at http://bytebuddy.net.

The install() method returns the AgentBuilder you wish Disco to continue using for installation. This could be:
1. The same AgentBuilder which was passed in (the usual case.)
1. A new, different AgentBuilder you instantiated yourself (in which case the one provided in the incoming
argument will be ignored and disposed of)
1. null, if you want your Installable to have no effect on this occasion
 
Optionally, Installables can capture command line arguments passed at agent startup,
by implementing the handleArguments() method.

##### Building an Installable plugin

Installables consume the disco-java-agent-plugin-api, which uses ByteBuddy. In order to avoid
conflicts with any possible usage of the same dependency in your client's code, it is necessary to
use JAR shading/shadowing to 'rewrite' the fully qualified domain names of ByteBuddy's classses to make them private.

In the Disco code, we use the Gradle Shadow Plugin to achieve this. See https://imperceptiblethoughts.com/shadow/

In order for a plugin to be compatible with the substrate agent, they need to agree on how that package rewriting takes place.

Throughout Disco, we always rewrite the ByteBuddy and ASM dependencies using the rules in the top level
build.gradle.kts file:

1. Everything under "org.objectweb.asm" is moved to the same path suffix beneath "software.amazon.disco.agent.jar.asm"
1. Everything under "net.bytebuddy" is moved to the same path suffix beneath "software.amazon.disco.agent.jar.bytebuddy"

To use your plugin with *this* substrate agent, you need to apply the same rules to your build plugin JAR files.

#### Listeners

Listeners consume Events, decoupled from the Installables which produce them. Ahead of time, Listeners
do not need to know which Installables might be available, only which Events they are interested in (should those
events occur at all).

Any declared Listeners will be instantiated by reflectively calling their no-args constructor, using the
newInstance() method on the reflectively acquired class. Therefore, the Listeners you provide in your plugin
need to:

1. Be public.
1. Have a public default constructor (taking no arguments)
1. Implement the Listener interface as described in disco-java-agent-api

#### Arbitrary initialization

A very simple mechanism to provide on-shot static initialization if neither above mechanism is suitable,
or to exist side by side with them. If the class expressed in the MANIFEST contains a method init(), with no
arguments or return type, it will be called, once, after Installable processing has completed.

## Installing and configuring the agent

The most reliable way to install any Java Agent, including Disco agents, is at the java command line, using the general syntax of:

`-javaagent:/path/to/agent.jar=argument1:argument2=value:argument3:argument4=value`

Arguments accepted by Disco itself can be found in AgentConfigParser.java, and any Installables you know are
present may accept extra ones if they implement handleArguments()

If you do not control the invocation of java, such as in some managed runtime environments
like AWS Lambda, it may be necessary to 'inject' your agent at runtime after startup. See
the disco-java-agent-inject-api subproject for a discussion of this technique.

## Forking the agent

This agent is a canonical example of a bare/empty agent, suitable for use as a
substrate for plugin discovery, and works out of the box (see the tests in disco-java-agent-web-plugin, for example).

However, your site or organization or special circumstance may mean that you wish to customize it.
For example, if you want to inline the installation of Installables which you always include (instead of
loading them via plugin discovery), or if you need to have your own JAR shading/shadowing rules different from
the ones that Disco is generally built with (see Installables section above).

### Custom ignore rules

The final noteworthy reason, is that Disco contains certain 'ignore' rules by default. The AgentBuilder instances
it provides to Installables have constraints including:

1. Disallowing them to intercept/instrument Disco itself.
1. Disallowing it for the 'unsafe' JDK namespaces of "sun.*", "com.sun.*" and "jdk.*"
1. Disallowing for some popular frameworks which also use aspect orientation, and which we think might collide,
e.g. AspectJ and the Jacoco test coverage agent.

See InterceptionInstaller.java for the full set of these rules.

Of course, you may have a specific need to either suppress these rules, or extend them with ignore rules
for something in your software stack which you have also found to be problematic. In the latter case please let us
know, but generally be aware that in a fork of this agent's code you have the flexibility to add a custome ignore
rule to be passed as an argument to the install() method of a DiscoAgentTemplate instance. See the source code
in DiscoAgent.java.


