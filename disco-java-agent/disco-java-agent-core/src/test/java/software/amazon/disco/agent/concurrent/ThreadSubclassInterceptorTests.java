/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.concurrent.decorate.DecoratedThread;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;

import static software.amazon.disco.agent.concurrent.TransactionContext.TRANSACTION_OWNING_THREAD_KEY;

public class ThreadSubclassInterceptorTests {
    MyListener listener;

    @Before
    public void before() {
        EventBus.addListener(listener = new MyListener());
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(listener);
    }

    @Test
    public void testThreadNotMatches() {
        Assert.assertFalse(ThreadSubclassInterceptor.createThreadSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(Thread.class)
        ));
    }

    @Test
    public void testSubclassesUnderJavaLangRefNotMatches() throws ClassNotFoundException {
        Assert.assertFalse(ThreadSubclassInterceptor.createThreadSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(Class.forName("java.lang.ref.Reference$ReferenceHandler"))
        ));

        Assert.assertFalse(ThreadSubclassInterceptor.createThreadSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(Class.forName("java.lang.ref.Finalizer$FinalizerThread"))
        ));
    }

    @Test
    public void testThreadSubclassMatches() {
        Assert.assertTrue(ThreadSubclassInterceptor.createThreadSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(TestThread.class)
        ));
    }

    @Test
    public void testThreadSubclassRunMethodMatches() throws Exception {
        Assert.assertTrue(ThreadSubclassInterceptor.createRunMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(TestThread.class.getDeclaredMethod("run"))
        ));
    }

    @Test
    public void testThreadSubclassStartMethodMatches() throws Exception {
        Assert.assertTrue(ThreadSubclassInterceptor.createStartMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(Thread.class.getDeclaredMethod("start"))
        ));
    }

    @Test
    public void testStartAdviceSafe() {
        ThreadSubclassInterceptor.StartAdvice.onMethodEnter(new DecoratedThread());
    }

    @Test
    public void testStartAdviceDecorates() {
        DecoratedThread dt = ThreadSubclassInterceptor.StartAdvice.methodEnter();
        Assert.assertNotNull(dt);
    }

    @Test
    public void testRunAdviceEnterSameThread() {
        DecoratedThread d = new DecoratedThread();
        d.before();
        Assert.assertNotNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testRunAdviceExitSameThread() {
        DecoratedThread d = new DecoratedThread();
        d.after();
        Assert.assertNull(listener.enter);
        Assert.assertNotNull(listener.exit);
    }

    @Test
    public void testRunAdviceEnterDifferentThread() {
        TransactionContext.putMetadata(TRANSACTION_OWNING_THREAD_KEY, -1);
        DecoratedThread d = new DecoratedThread();
        ThreadSubclassInterceptor.RunAdvice.onMethodEnter(d);
        Assert.assertTrue(listener.enter instanceof ThreadEnterEvent);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testRunAdviceExitDifferentThread() {
        TransactionContext.putMetadata(TRANSACTION_OWNING_THREAD_KEY, -1);
        DecoratedThread d = new DecoratedThread();
        ThreadSubclassInterceptor.RunAdvice.onMethodExit(d);
        Assert.assertNull(listener.enter);
        Assert.assertTrue(listener.exit instanceof ThreadExitEvent);
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ThreadSubclassInterceptor());
    }

    static class TestThread extends Thread {
        @Override
        public void run() {

        }
    }

    static class MyListener implements Listener {
        Event enter = null;
        Event exit = null;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ThreadEnterEvent) {
                enter = e;
            } else if (e instanceof ThreadExitEvent) {
                exit = e;
            }
        }
    }
}
