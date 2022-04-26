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

package software.amazon.disco.agent.awsv1;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.event.AwsV1ServiceDownstreamRequestEventImpl;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * When making a downstream AWS call, the doInvoke method is intercepted.
 */
public class AWSClientInvokeInterceptor implements Installable {
    /**
     * Disco logger. Must be public due to use in Advice methods
     */
    public static final Logger log = LogManager.getLogger(AWSClientInvokeInterceptor.class);

    /**
     * The Disco Origin for AWS SDK V1 events. Public because it's referenced in the Advice methods
     */
    public static final String AWS_V1_ORIGIN = "AWSv1";

    /**
     * This method is inlined by ByteBuddy before the doInvoke() method inside each of the AWS clients.
     * It  acquires the service and operation names for the request, and uses the collected data to create a {@link ServiceDownstreamRequestEvent}
     * and publish it to the Disco event bus.
     *
     * @param request - The method signature of the intercepted call is:
     *                  Response (Request, HttpResponseHandler, ExecutionContext
     *                  The first argument is the request object which is the argument we need.
     * @param origin - an identifier of the intercepted Method, for logging/debugging
     * @return - The Disco event created
     */
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ServiceDownstreamRequestEvent enter(@Advice.Argument(0) final Request<?> request, @Advice.Origin final String origin) {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(AWSv1) method interception of " + origin);
        }

        String service = request.getServiceName();
        AmazonWebServiceRequest originalRequest = request.getOriginalRequest();
        String operation = originalRequest.getClass().getSimpleName().replace("Request", "");

        ServiceDownstreamRequestEvent requestEvent = new AwsV1ServiceDownstreamRequestEventImpl(AWS_V1_ORIGIN, service, operation);
        requestEvent.withRequest(request);
        EventBus.publish(requestEvent);

        return requestEvent;
    }

    /**
     * This method is inlined by ByteBuddy at the end of the doInvoke() method of all AWS clients.
     * It constructs a {@link ServiceDownstreamResponseEvent} with the response of doInvoke(), metadata from the request
     * event, and any throwable from the request and publishes it to the Disco Event Bus.
     *
     * @param requestEvent - The event produced by the {@link #enter} method
     * @param response - The response returned from the AWS Client's request
     * @param thrown - The throwable thrown by the request, if any
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(@Advice.Enter final ServiceDownstreamRequestEvent requestEvent,
                            @Advice.Return final Object response,
                            @Advice.Thrown final Throwable thrown)
    {

        ServiceResponseEvent responseEvent = new ServiceDownstreamResponseEvent(
                AWS_V1_ORIGIN,
                requestEvent.getService(),
                requestEvent.getOperation(),
                requestEvent)
                .withResponse(response)
                .withThrown(thrown);

        EventBus.publish(responseEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform(new AgentBuilder.Transformer.ForAdvice()
                    .include(this.getClass().getClassLoader()) //ensure that bytebuddy can see this classloader, not just the destination class' classloader, when resolving types
                    .advice(buildMethodMatcher(), this.getClass().getName()));
    }

    /**
     * Builds a class matcher to discover all implemented AWS clients.
     * @return an ElementMatcher suitable for passing to the type() method of a AgentBuilder
     */
    ElementMatcher<? super TypeDescription> buildClassMatcher() {
        return hasSuperType(named("com.amazonaws.AmazonWebServiceClient"))
                .and(not(isInterface()))
                .and(not(isAbstract()));
    }

    /**
     * Builds a method matcher to match against all doInvoke methods in AWS clients
     * @return an ElementMatcher suitable for passing to builder.method()
     */
    ElementMatcher<? super MethodDescription> buildMethodMatcher() {
        return named("doInvoke")
                .and(not(isAbstract()));
    }
}