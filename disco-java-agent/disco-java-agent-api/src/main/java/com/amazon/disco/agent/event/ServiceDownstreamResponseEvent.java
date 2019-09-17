package com.amazon.disco.agent.event;

/**
 * Concrete ServiceEvent to express when a service downstream call which has completed, i.e. when the service-under-test
 * is making a request of an intercepted subsystem e.g. a remote service or an in-process Cache client.
 */
public class ServiceDownstreamResponseEvent extends AbstractServiceResponseEvent {
    /**
     * Construct a ServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     */
    public ServiceDownstreamResponseEvent(String origin, String service, String operation, ServiceRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.DOWNSTREAM;
    }
}
