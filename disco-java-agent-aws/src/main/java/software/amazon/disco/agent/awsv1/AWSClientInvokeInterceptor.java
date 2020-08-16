package software.amazon.disco.agent.awsv1;

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
     * It reflectively acquires the service and operation names for the request, taking care to swallow and log
     * any reflective exceptions. It uses the collected data to create a {@link ServiceDownstreamRequestEvent}
     * and publish it to the Disco event bus.
     *
     * @param request - The method signature of the intercepted call is:
     *                  Response (DefaultRequest, HttpResponseHandler, ExecutionContext
     *                  The first argument is the request object which is the argument we need.
     * @param origin - an identifier of the intercepted Method, for logging/debugging
     * @return - The Disco event created
     */
    @Advice.OnMethodEnter
    public static ServiceDownstreamRequestEvent enter(@Advice.Argument(0) final Object request,
                                                      @Advice.Origin final String origin)
    {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(AWSv1) method interception of " + origin);
        }

        String service = null;
        String operation = null;

        //Retrieve name of AWS service we are calling
        try {
            service = (String) request.getClass().getMethod("getServiceName").invoke(request);
        } catch (Exception e) {
            log.warn("Disco(AWSv1) failed to retrieve service name from AWS client request", e);
        }


        //Original request contains both the operation name and the request object
        try {
            Object originalRequest = request.getClass().getMethod("getOriginalRequest").invoke(request);
            operation = originalRequest.getClass().getSimpleName().replace("Request", "");
        } catch (Exception e) {
            log.warn("Disco(AWSv1) failed to retrieve operation name from AWS client request", e);
        }

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
    @Advice.OnMethodExit(onThrowable = Throwable.class)
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
                    .advice(buildMethodMatcher(), AWSClientInvokeInterceptor.class.getName()));
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