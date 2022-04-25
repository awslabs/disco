/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.sql;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

/**
 * A class for intercepting methods on the JDBC Connection class.
 */
public class ConnectionInterceptor implements Installable {
    // Must be public for use in Advice
    public static final Logger log = LogManager.getLogger(ConnectionInterceptor.class);
    public static final String SQL_PREPARE_ORIGIN = "SqlPrepare";

    /**
     * This method is inlined before any calls to prepareStatement or prepareCall, and its
     * purpose is to capture the query string provided as the first argument.
     *
     * @param queryString The (potentially parameterized) query string provided by user
     * @param origin Identifier of the intercepted method, for debugging/logging
     * @param conn Connection object being used to prepare the statement/call
     * @return The generated disco {@link ServiceDownstreamRequestEvent}, with fields filled out
     *         on a best-effort basis, including the Connection as the request object.
     */
    @Advice.OnMethodEnter
    public static ServiceRequestEvent enter(@Advice.Argument(value = 0) final String queryString,
                                            @Advice.Origin final String origin,
                                            @Advice.This final Connection conn) {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Sql) interception of " + origin);
        }

        String db = null;

        try {
            db = conn.getCatalog();
        } catch (Exception e) {
            log.warn("Disco(Sql) failed to retrieve Database name for SQL Downstream Service event", e);
        }

        ServiceRequestEvent requestEvent = new ServiceDownstreamRequestEvent(SQL_PREPARE_ORIGIN, db, queryString)
                .withRequest(conn);
        EventBus.publish(requestEvent);
        return requestEvent;
    }

    /**
     * This method is inlined with a preparation method at the moment it would return or throw a {@link Throwable}.
     * It extracts the created statement and throwable if any and publishes a {@link ServiceDownstreamResponseEvent}.
     *
     * @param requestEvent the disco event generated when this prepare request was made
     * @param response the {@code PreparedStatement} returned by the {@code Connection}
     * @param thrown the Throwable thrown by the connection, or null if preparation was successful.
     *               Typically a {@code SQLException}.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter final ServiceRequestEvent requestEvent,
                            @Advice.Return final PreparedStatement response,
                            @Advice.Thrown final Throwable thrown) {

        ServiceResponseEvent responseEvent = new ServiceDownstreamResponseEvent(
                SQL_PREPARE_ORIGIN,
                requestEvent.getService(),
                requestEvent.getOperation(),
                requestEvent)
                .withResponse(response)
                .withThrown(thrown);

        EventBus.publish(responseEvent);
    }

    /**
     * Installs part of the Disco SQL interception library into a Java program. Intended to be invoked
     * during an agent's premain.
     *
     * @param agentBuilder - an AgentBuilder to append instructions to
     * @return - the {@code AgentBuilder} object for chaining
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .include(this.getClass().getClassLoader())
                        .advice(buildMethodMatcher(), ConnectionInterceptor.class.getName()));
    }

    /**
     * Builds an element matcher that will match any implementation of the JDBC Connection interface
     *
     * @return - An ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    static ElementMatcher<? super TypeDescription> buildClassMatcher() {
        return hasSuperType(named("java.sql.Connection"))
                .and(not(isInterface()));
    }

    /**
     * Builds an ElementMatcher for the either the prepareStatement or prepareCall methods. Both send a
     * pre-defined SQL query to the server to optimize repeated executions. The first argument of these
     * methods is the query we'd like to capture.
     *
     * @return - An ElementMatcher that can match one of the methods to send a SQL query
     */
    static ElementMatcher<? super MethodDescription> buildMethodMatcher() {
        return nameStartsWith("prepare")
                .and(takesArgument(0, String.class))
                .and(returns(hasSuperType(named("java.sql.PreparedStatement"))));
    }
}
