/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.application.example;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.event.HttpServletNetworkRequestEvent;
import software.amazon.disco.agent.event.HttpServletNetworkResponseEvent;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import software.amazon.disco.agent.event.TransactionBeginEvent;
import software.amazon.disco.agent.event.TransactionEndEvent;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.reflect.ReflectiveCall;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class InjectorTest {
    // Ensure not to mistake disco-java-agent-x.y-sources.jar or disco-java-agent-x.y-javadoc.jar for the agent JAR
    private static final Pattern agentJarNamePattern = Pattern.compile("disco-java-agent-[^-]+\\.jar");

    @BeforeClass
    public static void beforeClass() {
        Instrumentation instrumentation = Injector.createInstrumentation();

        // Find the agent JAR file
        File[] agentJarFiles = new File("../disco-java-agent/build/libs/")
            .listFiles((dir1, name) -> agentJarNamePattern.matcher(name).matches());
        assertNotNull("Failed to list agent JAR files", agentJarFiles);
        assertEquals("Found zero, or more than one agent JAR file", agentJarFiles.length, 1);
        File agentJarFile = agentJarFiles[0];

        // Find the plugin directory
        File pluginDir = new File("../../disco-java-agent-web/disco-java-agent-web-plugin/build/libs/");
        assertTrue("Failed to find plugin directory", pluginDir.isDirectory());

        assertFalse(ReflectiveCall.isAgentPresent());

        // loadAgent() will internally reset the cached result of 'isAgentPresent()' by invoking 'ReflectiveCall.resetCache()'.
        Injector.loadAgent(instrumentation, agentJarFile.getPath(), "pluginpath=" + pluginDir.getAbsolutePath());
        assertTrue(ReflectiveCall.isAgentPresent());
    }

    /**
     * Just a small test to create a Thread, assert that some context survives the hand off into the child thread
     */
    @Test
    public void testAgentThreadInterception() throws Exception {
        TestListener listener = new TestListener();
        listener.register();

        TransactionContext.clear();
        TransactionContext.set("tx_value");
        AtomicBoolean correctValue = new AtomicBoolean();
        Thread t = new Thread(() -> {
            correctValue.set(TransactionContext.get().equals("tx_value"));
        });
        t.start();
        t.join();
        assertTrue(correctValue.get());

        assertEquals(2, listener.events.size());
        assertTrue(listener.events.get(0) instanceof ThreadEnterEvent);
        assertTrue(listener.events.get(1) instanceof ThreadExitEvent);
        listener.remove();
    }

    /**
     * Test that our agent installed the Servlet support from the web package.
     */
    @Test
    public void testServletInterception() throws Exception {
        TestListener listener = new TestListener();
        listener.register();

        TestServlet servlet = new TestServlet();

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURL()).thenReturn(new StringBuffer("URI"));
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        servlet.service(req, resp);
        assertTrue(servlet.called);

        assertEquals(4, listener.events.size());
        assertTrue(listener.events.get(0) instanceof TransactionBeginEvent);
        assertTrue(listener.events.get(1) instanceof HttpServletNetworkRequestEvent);
        assertTrue(listener.events.get(2) instanceof HttpServletNetworkResponseEvent);
        assertTrue(listener.events.get(3) instanceof TransactionEndEvent);

        listener.remove();
    }
}
