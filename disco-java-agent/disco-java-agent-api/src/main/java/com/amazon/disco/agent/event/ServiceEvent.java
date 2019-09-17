package com.amazon.disco.agent.event;


/**
 * An event issued to the event bus on a service activity or downstream call
 */
public interface ServiceEvent extends Event {
    /**
     * The type of this ServiceEvent
     */
    enum Type {
        /**
         * If the event is 'my service is handling a request'
         */
        ACTIVITY,

        /**
         * If the event is 'I am making a request of a dependency'
         */
        DOWNSTREAM
    }

    /**
     * Get the service name
     * @return the service name e.g. 'WeatherService'
     */
    String getService();

    /**
     * Get the operation name
     * @return the operation name e.g. 'getWeather'
     */
    String getOperation();

    /**
     * Get the type of this ServiceEvent
     * @return either ACTIVITY or DOWNSTREAM
     */
    Type getType();
}
