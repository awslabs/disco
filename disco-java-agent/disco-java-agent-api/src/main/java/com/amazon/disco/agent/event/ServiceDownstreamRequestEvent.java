package com.amazon.disco.agent.event;

/**
 * Concrete ServiceEvent to express a service downstream call which is about to occur, i.e. when the service-under-test
 * is making a request of an intercepted subsystem e.g. a remote service or an in-process Cache client.
 */
public class ServiceDownstreamRequestEvent extends AbstractServiceRequestEvent {
    /**
     * Construct a ServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public ServiceDownstreamRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.DOWNSTREAM;
    }
}

