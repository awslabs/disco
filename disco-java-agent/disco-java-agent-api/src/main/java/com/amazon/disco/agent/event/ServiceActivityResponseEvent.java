package com.amazon.disco.agent.event;

/**
 * Concrete ServiceEvent to express a service activity response, i.e. when the service-under-test has completed processing a transaction
 */
public class ServiceActivityResponseEvent extends AbstractServiceResponseEvent {
    /**
     * Construct a ServiceActivityResponseEvent
     * @param origin the origin of the activity e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     */
    public ServiceActivityResponseEvent(String origin, String service, String operation, ServiceRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.ACTIVITY;
    }
}
