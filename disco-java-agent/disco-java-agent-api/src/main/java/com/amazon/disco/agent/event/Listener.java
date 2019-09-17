package com.amazon.disco.agent.event;

/**
 * Implement this class to supply a listener to the EventBus
 */
public interface Listener {
    /**
     * All Listeners have a priority. Events will be published to Listeners in priority order, but within the same
     * priority the order of dispatch is not defined.
     *
     * The extremes of high and low values are System priorities, which cannot be used by 3rd parties
     * You may not use Integer.MIN_VALUE or Integer.MAX_VALUE
     *
     * @return the priority of this Listener
     */
    int getPriority();

    /**
     * The receiver of the Event at the time the Event is published
     * @param e the Event being listened to
     */
    void listen(Event e);
}

