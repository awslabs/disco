package com.amazon.disco.agent.event;


/**
 * Base class for service events both Activities (my service is handling a request) and Downstream (I am calling another Service)
 * A 'Service' in the downstream sense may not necessarily be a remote endpoint in the sense of a Coral service etc. It
 * could be an in-process client for e.g. a Cache, or Weblabs. It means a service in the sense of anything with an API,
 * whether that is remote or local.
 */
public abstract class AbstractServiceEvent extends AbstractEvent implements ServiceEvent {

    /**
     * The data which ServiceEvents may contain
     */
    enum DataKey {
        /**
         * The service name, e.g. 'WeatherService'
         */
        SERVICE,

        /**
         * The operation name e.g. 'getWeather'
         */
        OPERATION
    }

    /**
     * Constructor for a ServiceEvent
     * @param origin the origin of this event e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. WeatherService
     * @param operation the operation name e.g getWeather
     */
    public AbstractServiceEvent(String origin, String service, String operation) {
        super(origin);
        withData(DataKey.SERVICE.name(), service);
        withData(DataKey.OPERATION.name(), operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractServiceEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getService() {
        return String.class.cast(getData(DataKey.SERVICE.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperation() {
        return String.class.cast(getData(DataKey.OPERATION.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Type getType();
}
