package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.Utils;
import com.amazon.disco.agent.config.AgentConfig;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Class to control installation of interceptions/advice on target methods.
 */
public class InterceptionInstaller {
    private static final InterceptionInstaller INSTANCE = new InterceptionInstaller();
    private static final Logger log = LogManager.getLogger(InterceptionInstaller.class);

    private Set<Class> alreadyInstalled = new HashSet();

    /**
     * Private constructor for singleton semantics
     */
    private InterceptionInstaller() {

    }

    /**
     * Singleton access
     * @return the InterceptionInstaller singleton
     */
    public static InterceptionInstaller getInstance() {
        return INSTANCE;
    }

    /**
     * Sets up the interceptions as configured by the Agent, passing in a list of Installables.
     * @param instrumentation - the Instrumentation instance, as passed to 'premain'
     * @param installables - the collection of Installable hooks passed in from the Agent
     * @param config - the command line config passed into the agent.
     */
    public void install(Instrumentation instrumentation, Set<Installable> installables, AgentConfig config) {
        ElementMatcher<? super TypeDescription> ignoreMatcher = createIgnoreMatcher();

        File bootstrapTemp = Utils.getInstance().getTempFolder();
        //if the folder has remnants from a previous run, remove them
        for (File file : bootstrapTemp.listFiles()) {
            file.delete();
        }

        for (Installable installable: installables) {
            //We create a new Agent for each Installable, otherwise their matching rules can
            //compete with each other.
            AgentBuilder agentBuilder = new AgentBuilder.Default()
                    .ignore(ignoreMatcher)
                    .enableBootstrapInjection(instrumentation, bootstrapTemp)
                    ;

            //The Interception listener is expensive during class loading, and limited value most of the time
            if (config.isExtraverbose()) {
                agentBuilder = agentBuilder.with(InterceptionListener.INSTANCE);
            }

            log.info("AlphaOne(Core) attempting to install "+installable.getClass().getName());
            if (alreadyInstalled.contains(installable.getClass())) {
                log.info("AlphaOne(Core)" + installable.getClass().getName() + " already installed; skipping.");
                continue;
            }

            alreadyInstalled.add(installable.getClass());
            agentBuilder = installable.install(agentBuilder);

            if (agentBuilder != null) {
                agentBuilder.installOn(instrumentation);
            }
        }
    }

    /**
     * Create a matcher to ignore low-level and otherwise problematic namespaces.
     *
     * @return - a matcher suitable for passing to AgentBuilder#ignore
     */
    public static ElementMatcher.Junction<? super TypeDescription> createIgnoreMatcher() {
        ElementMatcher.Junction<? super TypeDescription> excludedNamespaces =
            //low-level pieces of the JDK and runtime
            nameStartsWith("sun.")
            .or(nameStartsWith("com.sun."))
            .or(nameStartsWith("jdk."))

            //3rd party libraries
            .or(nameStartsWith("org.jacoco."))
            .or(nameStartsWith("org.junit."))
            .or(nameStartsWith("org.springframework."))
            .or(nameStartsWith("org.aspectj."))

            //internal libraries
            .or(nameStartsWith("amazon.actiontrace."))
            .or(nameStartsWith("amazon.odin."))
            .or(nameStartsWith("com.amazon.profiler."))
            .or(nameStartsWith("com.amazon.aaa."))
            .or(nameStartsWith("com.amazon.coral.spring."))
            .or(nameStartsWith("com.amazon.coral.reference."))
            .or(nameStartsWith("com.amazon.coral.reflect."))

            //alphaone itself and its internals
            .or(nameStartsWith("com.amazon.alphaone.agent.")
                .and(not(nameStartsWith("com.amazon.alphaone.agent.tester."))));

        return excludedNamespaces;

    }
}