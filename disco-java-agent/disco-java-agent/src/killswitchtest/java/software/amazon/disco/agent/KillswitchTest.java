package software.amazon.disco.agent;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.util.ArrayList;
import java.util.List;

public class KillswitchTest {

    @Test
    public void testKillswitch() throws Exception {
        TransactionContext.create();
        TestListener listener = new TestListener();
        EventBus.addListener(listener);

        //create and run a Thread, which if the Agent was not killswitched would yield some events from the Thread interception
        Thread t = new Thread(()->{});
        t.start();
        t.join();

        Assert.assertEquals(0, listener.events.size());
    }

    static class TestListener implements Listener {
        List<Event> events = new ArrayList<>();
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            events.add(e);
        }
    }
}
