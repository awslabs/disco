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
package software.amazon.disco.agent.web.servlet;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServletNetworkRequestEvent;
import software.amazon.disco.agent.event.HttpServletNetworkResponseEvent;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class is to support Bytebuddy's method delegation for {@link HttpServletServiceInterceptor}
 * When the service() method of HttpServlet or subclass of it is called,
 * the method is intercepted to generate HttpNetworkProtocol(Request/Response)Events.
 */
public class HttpServletServiceMethodDelegation {
    private final static Logger log = LogManager.getLogger(HttpServletServiceInterceptor.class);

    private static final String TX_NAMESPACE = "HTTP_SERVLET_SERVICE";
    private static final String EVENT_ORIGIN = "httpServlet";
    // Common HTTP Header keys
    private static final String DATE_HEADER = "date";
    private static final String HOST_HEADER = "host";
    private static final String ORIGIN_HEADER = "origin";
    private static final String REFERER_HEADER = "referer";
    private static final String USER_AGENT_HEADER = "user-agent";

    /**
     * The HttpServlet#service method is intercepted, and redirected here, where the
     * original request and response objects are sifted through to retrieve useful
     * header information that is stored in the HttpNetworkProtocol(Request/Response)Events
     * and published to the event bus.
     *
     * @param args    the original arguments passed to the invoke call
     * @param invoker the original 'this' of the invoker, in case useful or for debugging
     * @param origin  identifier of the intercepted method, for debugging/logging
     * @param zuper   a callable to call the original method
     * @throws Exception - catch-all for whatever exceptions might be throwable in the original call
     */
    @SuppressWarnings("unused")
    public static void service(@AllArguments Object[] args,
                               @This Object invoker,
                               @Origin String origin,
                               @SuperCall Callable<Object> zuper) throws Throwable {
        HttpServletNetworkRequestEvent requestEvent = null;
        HttpServletNetworkResponseEvent responseEvent = null;
        Throwable throwable = null;
        if (TransactionContext.isWithinCreatedContext() && TransactionContext.getMetadata(TX_NAMESPACE) != null) {
            //since service() calls in subclasses may call their parents, this interceptor can stack up
            //only perform event publication it if we were the first call to take place
            zuper.call();
            return;
        }
        TransactionContext.create();
        TransactionContext.putMetadata(TX_NAMESPACE, true);

        try {
            // To reduce the # of dependencies, we use reflection to obtain the basic methods.
            Object request = args[0];
            HttpServletRequest servletReq = (HttpServletRequest) request;

            // Obtain the metadata information from the host.
            // If they are null, they are't stored, so retrieval would be null as well.
            int srcPort = servletReq.getRemotePort();
            int dstPort = servletReq.getLocalPort();
            String srcIP = servletReq.getRemoteAddr();
            String dstIP = servletReq.getLocalAddr();
            requestEvent = new HttpServletNetworkRequestEvent(EVENT_ORIGIN, srcPort, dstPort, srcIP, dstIP)
                    .withHeaderMap(retrieveHeaderMap(servletReq))
                    .withDate(servletReq.getHeader(DATE_HEADER))
                    .withHost(servletReq.getHeader(HOST_HEADER))
                    .withHTTPOrigin(servletReq.getHeader(ORIGIN_HEADER))
                    .withReferer(servletReq.getHeader(REFERER_HEADER))
                    .withUserAgent(servletReq.getHeader(USER_AGENT_HEADER))
                    .withMethod(servletReq.getMethod())
                    .withRequest(request)
                    .withURL(servletReq.getRequestURL().toString());
            EventBus.publish(requestEvent);
        } catch (Throwable e) {
            log.error("DiSCo(Web) Failed to retrieve request data from servlet service.");
        }

        // call the original, catching anything it throws
        try {
            zuper.call();
        } catch (Throwable t) {
            throwable = t;
        }

        try {
            Object response = args[1];
            HttpServletResponse servletResponse = (HttpServletResponse) response;
            int statusCode = servletResponse.getStatus();
            responseEvent = new HttpServletNetworkResponseEvent(EVENT_ORIGIN, requestEvent)
                    .withHeaderMap(retrieveHeaderMap(servletResponse))
                    .withStatusCode(statusCode)
                    .withResponse(response);
            EventBus.publish(responseEvent);
        } catch (Throwable t) {
            log.error("DiSCo(Web) Failed to retrieve response data from service.");
        }
        //match the create() call with a destroy() in all cases
        TransactionContext.destroy();
        //rethrow anything
        if (throwable != null) {
            throw throwable;
        }
    }

    static Map<String, String> retrieveHeaderMap(HttpServletRequest servletRequest) {
        Map<String, String> ret = new HashMap<>();
        try {
            Enumeration<String> headerNames = servletRequest.getHeaderNames();
            if (headerNames == null) {
                return ret;
            }
            for (String name : Collections.list(headerNames)) {
                ret.put(name, servletRequest.getHeader(name));
            }
        } catch (Throwable t) {
            //do nothing
        }
        return ret;
    }

    static Map<String, String> retrieveHeaderMap(HttpServletResponse servletResponse) {
        Map<String, String> ret = new HashMap<>();
        try {
            Collection<String> headerNames = servletResponse.getHeaderNames();
            if (headerNames == null) {
                return ret;
            }
            for (String name : headerNames) {
                ret.put(name, servletResponse.getHeader(name));
            }
        } catch (Throwable t) {
            //do nothing
        }
        return ret;
    }
}
