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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class InjectorTest {
    @BeforeClass
    public static void beforeClass() {
        Instrumentation instrumentation = Injector.createInstrumentation();

        File pluginDir = new File("../../disco-java-agent-web/disco-java-agent-web-plugin/build/libs/");
        File agentPath = new File("../disco-java-agent/build/libs/")
            .listFiles((dir1, name) -> name.startsWith("disco-java-agent-") && name.endsWith(".jar"))[0];

        assertFalse(ReflectiveCall.isAgentPresent());

        // loadAgent() will internally reset the cached result of 'isAgentPresent()' by invoking 'ReflectiveCall.resetCache()'.
        Injector.loadAgent(instrumentation, agentPath.getPath(), "pluginpath=" + pluginDir.getAbsolutePath());

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
