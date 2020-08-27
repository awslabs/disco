/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.instrumentation.preprocess.cli.Driver;
import software.amazon.disco.instrumentation.preprocess.mocks.IntegTestListener;
import software.amazon.disco.instrumentation.preprocess.mocks.IntegTestThread;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This test class statically instruments a jarfile containing only one bare bone class named IntegTestThread that
 * extends Thread using an pluggable disco agent with concurrency support as default.
 * <p>
 * The goal of this integ test is to check whether ThreadEnterEvent and ThreadExitEvent events are published and captured
 * by the TestListener when invoking {@link Thread#start()} of a transformed IntegTestThread instance.
 */
public class ThreadSubclassInterceptorIntegTest {
    private static final String RENAME_SUFFIX = "Redefined";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() throws Exception {
        String srcJarPath = JarUtils.makeTargetJarWithRenamedClasses(Arrays.asList(IntegTestThread.class), "Redefined", temporaryFolder).getAbsolutePath();
        String arg = "verbose:loggerfactory=software.amazon.disco.agent.reflect.logging.StandardOutputLoggerFactory";

        File agentPath = new File("../disco-java-agent/disco-java-agent/build/libs/")
                .listFiles((dir1, name) -> name.startsWith("disco-java-agent-") && name.endsWith(".jar"))[0];

        String[] commandAndArgs = new String[]{
                "-jps", srcJarPath,
                "-ap", agentPath.getAbsolutePath(),
                "-out", temporaryFolder.getRoot().getAbsolutePath(),
                "-arg", arg,
                "--verbose"
        };

        Driver.main(commandAndArgs);

        Injector.addToSystemClasspath(Injector.createInstrumentation(), new File(srcJarPath));
    }

    @Test
    public void testStaticInstrumentationOnThreadSubclassInterceptorWorks() throws Exception {
        TransactionContext.create();
        IntegTestListener listener = new IntegTestListener();
        LinkedHashMap<Class, Integer> eventsRegistry = listener.getEventsRegistry();
        listener.register();

        IntegTestThread original = new IntegTestThread();
        original.start();
        original.join();

        Assert.assertTrue(eventsRegistry.isEmpty());

        Thread transformed = (Thread) Class.forName(IntegTestThread.class.getName() + RENAME_SUFFIX)
                .getDeclaredConstructor()
                .newInstance();

        transformed.start();
        transformed.join();

        Assert.assertEquals(2, eventsRegistry.size());
        Assert.assertTrue(eventsRegistry.containsKey(ThreadEnterEvent.class));
        Assert.assertTrue(eventsRegistry.containsKey(ThreadExitEvent.class));
        Assert.assertEquals(1, eventsRegistry.get(ThreadEnterEvent.class).intValue());
        Assert.assertEquals(1, eventsRegistry.get(ThreadExitEvent.class).intValue());

        // Iterate through the ordered LinkedHashMap to verify if ThreadExitEvent is published after ThreadEnterEvent
        boolean enterEventFound = false;
        for(Class clazz : eventsRegistry.keySet()){
            if(clazz.equals(ThreadEnterEvent.class)){
                enterEventFound = true;
            }else if(clazz.equals(ThreadExitEvent.class)){
                // test will fail if ThreadEnterEvent wasn't published before ThreadExitEvent
                Assert.assertTrue(enterEventFound);
                break;
            }
        }
    }
}
