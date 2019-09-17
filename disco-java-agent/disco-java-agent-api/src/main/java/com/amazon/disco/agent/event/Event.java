package com.amazon.disco.agent.event;

/**
 * An Event published to the AlphaOne event bus
 */
public interface Event {
    /**
     * Getter for the Event's origin. This is named for the AlphaOne support package name e.g. 'Coral'
     * @return the origin of this event
     */
    String getOrigin();

    /**
     * Retrieve a data value from this event, given a named key
     * @param key the name of the data to retrieve
     * @return the value of the data, or null if absent
     */
    Object getData(String key);
}
