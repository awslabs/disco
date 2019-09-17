package com.amazon.disco.agent.event;

/**
 * Specialization of ServiceEvent for service requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs
 */
public interface ServiceResponseEvent extends ServiceEvent {
    /**
     * Get the associated request event
     * @return the request object
     */
    ServiceRequestEvent getRequest();

    /**
     * Get the response object
     * @return the response object
     */
    Object getResponse();

    /**
     * Get the thrown exception
     * @return the thrown exception
     */
    Throwable getThrown();
}
