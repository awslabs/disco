package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.concurrent.decorate.DecoratedThread;
import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.EventBus;
import com.amazon.disco.agent.event.Listener;
import com.amazon.disco.agent.event.ThreadEnterEvent;
import com.amazon.disco.agent.event.ThreadExitEvent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testRunAdviceExitSameThread() {
        DecoratedThread d = new DecoratedThread();
        d.before();
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testRunAdviceEnterDifferentThread() {
        MyDecoratedThread d = new MyDecoratedThread();
        d.setThreadId(-1);
        ThreadSubclassInterceptor.RunAdvice.onMethodEnter(d);
        Assert.assertTrue(listener.enter instanceof ThreadEnterEvent);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testRunAdviceExitDifferentThread() {
        MyDecoratedThread d = new MyDecoratedThread();
        d.setThreadId(-1);
        ThreadSubclassInterceptor.RunAdvice.onMethodExit(d);
        Assert.assertNull(listener.enter);
        Assert.assertTrue(listener.exit instanceof ThreadExitEvent);
    }

    @Test
    public void testInstall() {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        Mockito.when(agentBuilder.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        new ThreadSubclassInterceptor().install(agentBuilder);
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

    static class MyDecoratedThread extends DecoratedThread {
        public void setThreadId(long threadId) {
            this.parentThreadId = threadId;
        }
    }
}
