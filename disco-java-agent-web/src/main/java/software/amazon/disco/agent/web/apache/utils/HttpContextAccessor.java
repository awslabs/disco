/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.web.apache.utils;

import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.web.MethodHandleWrapper;

/**
 * Concrete accessor for the methods reflectively accessed within HttpContext.
 */
public class HttpContextAccessor {
    private static final String HTTP_CONTEXT_CLASS_NAME = "org.apache.http.protocol.HttpContext";
    private static final String HTTP_RESPONSE_CLASS_NAME = "org.apache.http.HttpResponse";
    static final String DISCO_REQUEST_EVENT_ATTR_NAME = "disco.request_event";

    private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    //these methods only use simple types, so can be initialized inline without any exception handling
    private static MethodHandleWrapper setAttribute = new MethodHandleWrapper(HTTP_CONTEXT_CLASS_NAME, classLoader, "setAttribute", void.class, String.class, Object.class);
    private static MethodHandleWrapper getAttribute = new MethodHandleWrapper(HTTP_CONTEXT_CLASS_NAME, classLoader, "getAttribute", Object.class, String.class);
    private static MethodHandleWrapper removeAttribute = new MethodHandleWrapper(HTTP_CONTEXT_CLASS_NAME, classLoader, "removeAttribute", Object.class, String.class);

    private final Object contextObject;

    /**
     * Construct a new HttpContextAccessor with a concrete HttpContext object.
     *
     * @param context The args of HttpClient.execute, in which contains a concrete HttpContext object to inspect
     */
    public HttpContextAccessor(final Object context) {
        this.contextObject = context;
    }

    /**
     * Adds a attribute to this message.
     *
     * @param id The id of the attribute
     * @param value The value of the attribute
     */
    public void setAttribute(final String id, final Object value) {
        setAttribute.invoke(contextObject, id, value);
    }

    /**
     * Gets attribute with a certain id from this context.
     *
     * @param id The id of the attributes
     * @return The value of the attribute
     */
    public Object getAttribute(final String id) {
        return getAttribute.invoke(contextObject, id);
    }

    /**
     * Removes attribute with a certain id from this context.
     *
     * @param id The id of the attributes to remove
     * @return The value of the attribute to remove
     */
    public Object removeAttribute(final String id) {
        return removeAttribute.invoke(contextObject, id);
    }

    /**
     * Helper method to put {@link HttpServiceDownstreamRequestEvent} into this context.
     *
     * This aims to map the {@link HttpServiceDownstreamRequestEvent}
     * to a {@link software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent} upon response received.
     *
     * @param requestEvent The corresponding {@link HttpServiceDownstreamRequestEvent} to the request
     */
    public void setRequestEvent(final HttpServiceDownstreamRequestEvent requestEvent) {
        setAttribute(DISCO_REQUEST_EVENT_ATTR_NAME, requestEvent);
    }

    /**
     * Helper method to remove {@link HttpServiceDownstreamRequestEvent} from this context.
     *
     * This aims to map the {@link HttpServiceDownstreamRequestEvent}
     * to a {@link software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent} upon response received.
     *
     * @return The corresponding {@link HttpServiceDownstreamRequestEvent} to the request
     */
    public HttpServiceDownstreamRequestEvent removeRequestEvent() {
        return (HttpServiceDownstreamRequestEvent) removeAttribute(DISCO_REQUEST_EVENT_ATTR_NAME);
    }

    /**
     * Helper method to get HttpResponse from this context and make it accessible via {@link HttpResponseAccessor}.
     *
     * @return The {@link HttpResponseAccessor} to manipulate the HttpResponse object
     */
    public HttpResponseAccessor getResponseAccessor() {
        final Object responseObject = getAttribute("http.response");
        try {
            Class<?> httpResponseClass = Class.forName(HTTP_RESPONSE_CLASS_NAME, true, ClassLoader.getSystemClassLoader());
            if (httpResponseClass != null && httpResponseClass.isInstance(responseObject)) {
                return new HttpResponseAccessor(responseObject);
            }
        } catch (ClassNotFoundException e) {
            // do nothing
        }
        return null;
    }
}
