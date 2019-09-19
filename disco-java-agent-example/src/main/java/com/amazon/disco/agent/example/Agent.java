package com.amazon.disco.agent.example;

import com.amazon.disco.agent.DiscoAgentTemplate;
import com.amazon.disco.agent.concurrent.ConcurrencySupport;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;

public class Agent {
    /**
     * The agent is loaded by a -javaagent command line parameter, which will treat 'premain' as its
     * entrypoint, in the class referenced by the Premain-Class attribute in the manifest - which should be this one.
     *
     * @param agentArgs - any arguments passed as part of the -javaagent argument string
     * @param instrumentation - the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        //install only the Concurrency support, just as the most simplistic test.
        new DiscoAgentTemplate(agentArgs).install(instrumentation, new HashSet<>(new ConcurrencySupport().get()));
    }
}
