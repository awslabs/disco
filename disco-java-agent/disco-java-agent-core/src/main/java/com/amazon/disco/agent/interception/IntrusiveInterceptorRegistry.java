package com.amazon.disco.agent.interception;

import java.util.concurrent.Callable;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;

/**
 * A single IntrusiveInterceptor may be installed to this Registry, for use-cases where program behaviour needs to be
 * altered/mutated by agent applications.
 */
public class IntrusiveInterceptorRegistry {
    private static Logger log = LogManager.getLogger(IntrusiveInterceptorRegistry.class);
    private static final IntrusiveInterceptor defaultInterceptor = new DefaultIntrusiveInterceptor();
    private static IntrusiveInterceptor installed = defaultInterceptor;

    /**
     * Install an IntrusiveInterceptor, to recieve notification from compatible Installables.
     * @param interceptor the interceptor to install
     * @return the previously installed interceptor
     */
    public static IntrusiveInterceptor install(final IntrusiveInterceptor interceptor) {
        IntrusiveInterceptor old = installed;
        installed = interceptor;
        return old;
    }

    /**
     * Uninstall any installed IntrusiveInterceptor, returning the registry to its initial state where
     * no intrusions take place.
     * @return the previously installed interceptor
     */
    public static IntrusiveInterceptor uninstall() {
        return install(defaultInterceptor);
    }

    /**
     * Get the currently installed IntrusiveInterceptor
     * @return the installed interceptor
     */
    public static IntrusiveInterceptor get() {
        return installed;
    }

    /**
     * Helper for MethodDelegations, which are a common interception pattern.
     *
     * Advice should use consider using the OnMethodEnter.skipOn feature when using a Replace interceptor
     * and passing the ContinuationDecision between the MethodEnter and MethodExit Advices, if both exist, using
     * the @Enter annotation which passes the returned value from a MethodEnter into the MethodExit.
     * @param event the event to convey to the decide() method of the installed IntrusiveInterceptor
     * @param call the @SuperCall curried callable of the original code
     * @param <T> the return type of the Callable (if known)
     * @return whatever is returned by the replace() or transform() methods of the IntrusiveInterceptor, or whatever
     *        is returned by the call() method of the supplied Callable if the decide() method produces Continue.
     * @throws Throwable
     */
    public static <T> T intrude(Event event, Callable<T> call) throws Throwable {
        //special case when no user-supplied IntrusiveInterceptor is registered, since there
        //is no point calling the decide() method in this case
        if (installed instanceof DefaultIntrusiveInterceptor) {
            return call.call();
        }

        //else delegate to the installed IntrusiveInterceptor
        IntrusiveInterceptor.ContinuationContext context = installed.decide(event);

        if (context == null || context.getDecision() == null) {
            return call.call();
        }

        switch (context.getDecision()) {
            case CONTINUE:
                return call.call();
            case REPLACE:
                return (T) installed.replace(context);
            case TRANSFORM: {
                T output = null;
                Throwable thrown = null;
                try {
                    output = call.call();
                } catch (Throwable t) {
                    thrown = t;
                }
                return (T) installed.transform(context, output, thrown);
            }
            default:
                log.warn("AlphaOne(Core) unknown response from IntrusiveInterceptor");
                return call.call();
        }

    }

    /**
     * A default always-continues IntrusiveInterceptor. Note that the decide() method of this ought never
     * to be called anyway, but returnes Continue as a safeguard.
     */
    static class DefaultIntrusiveInterceptor implements IntrusiveInterceptor {
        /**
         * {@inheritDoc}
         */
        public ContinuationContext decide(Event event) {
            return ContinuationContext.asContinue(event);
        }
    }
}
