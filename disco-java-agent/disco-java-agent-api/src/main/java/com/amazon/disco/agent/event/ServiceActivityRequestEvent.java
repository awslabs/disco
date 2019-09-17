package com.amazon.disco.agent.event;

/**
 * Concrete ServiceEvent to express a service activity request, i.e. when the service-under-test is just about to execute a transaction
 */
public class ServiceActivityRequestEvent extends AbstractServiceRequestEvent {
    /**
     * Construct a ServiceActivityRequestEvent
     * @param origin the origin of the activity e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public ServiceActivityRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.ACTIVITY;
    }
}
