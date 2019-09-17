package com.amazon.disco.agent.event;

/**
 * Base class for service requests, whether from an activity or a downstream
 */
abstract public class AbstractServiceRequestEvent extends AbstractServiceEvent implements ServiceRequestEvent {
    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The complete request object to the service
         */
        REQUEST
    }

    /**
     * Create a new AbstractServiceRequestEvent
     * @param origin the origin of the event
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public AbstractServiceRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractServiceRequestEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * Add a request object to this event
     * @param request the request object
     * @return the 'this' for method chaining
     */
    public AbstractServiceRequestEvent withRequest(Object request) {
        withData(DataKey.REQUEST.name(), request);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getRequest() {
        return getData(DataKey.REQUEST.name());
    }
}
