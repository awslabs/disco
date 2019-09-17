package com.amazon.disco.agent.event;

/**
 * Specialization of ServiceEvent for service requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs
 */
public interface ServiceRequestEvent extends ServiceEvent {
    /**
     * Get the request object
     * @return the request object
     */
    Object getRequest();
}
