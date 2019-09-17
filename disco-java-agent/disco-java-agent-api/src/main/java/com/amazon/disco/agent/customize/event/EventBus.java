package com.amazon.disco.agent.customize.event;

import com.amazon.disco.agent.customize.ReflectiveCall;
import com.amazon.disco.agent.customize.logging.Logger;
import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.Listener;

public class EventBus {
    static final String EVENTBUS_CLASS = ".event.EventBus";
    /**
     * Publish an event which will be broadcast to all listeners
     * @param e the event to publish
     */
    static public void publish(Event e)  {
        Logger.info("Publishing event " + e + " from " + e.getOrigin());
        ReflectiveCall.returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("publish")
            .withArgTypes(Event.class)
            .call(e);
    }

    /**
     * Add a listener to the EventBus
     * @param l the listener to add
     */
    static public void addListener(Listener l) {
        Logger.info("Adding listener to event bus " + l + " with priority " + l.getPriority());
        ReflectiveCall
            .returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("addListener")
            .withArgTypes(Listener.class)
            .call(l);
    }

    /**
     * Remove a listener from the EventBus
     * @param l the listener to remove. It is safe to remove a listener not currently added.
     */
    static public void removeListener(Listener l) {
        Logger.info("Removing listener from event bus " + l + " with priority " + l.getPriority());
        ReflectiveCall
            .returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("removeListener")
            .withArgTypes(Listener.class)
            .call(l);
    }

    /**
     * Remove all listeners from the EventBus, returning it to its initial state
     */
    static public void removeAllListeners() {
        Logger.info("Removing all listeners from event bus");
        ReflectiveCall
            .returningVoid()
            .ofClass(EVENTBUS_CLASS)
            .ofMethod("removeAllListeners")
            .call();
    }

    /**
     * Test if a given listener is currently registered with the EventBus
     * @param l the listener to test for
     * @return true if the listener is presently registered to receive events
     */
    static public boolean isListenerPresent(Listener l) {
        Logger.info("Checking if Listener " + l + " is present ");
        Boolean returnValue =  ReflectiveCall
                .returning(Boolean.class)
                .ofClass(EVENTBUS_CLASS)
                .ofMethod("isListenerPresent")
                .withArgTypes(Listener.class)
                .call(l);

        return returnValue == null  ? false : returnValue;
    }
}
