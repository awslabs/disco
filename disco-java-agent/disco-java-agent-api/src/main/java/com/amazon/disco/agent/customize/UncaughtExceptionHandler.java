package com.amazon.disco.agent.customize;

/**
 * Modeled after Thread#UncaughtExceptionHandler
 * Install an implementation of this via the provided install() method, to receive a notification when
 * any exception is thrown by any method in the AlphaOne Customization APIs. Without such a handler installed,
 * all exceptions - even unchecked ones - will be suppressed
 */
@FunctionalInterface
public interface UncaughtExceptionHandler {
    /**
     * Receive any exception thrown as part of calling an AlphaOne method. In an agent-not-present scenario,
     * calls via the Customization APIs will always be safe
     *
     * If an agent is not present, Exceptions will be dispatched to the UncaughtExceptionHandle if they are produced as
     * a result of invalid arguments/inputs. e.g. when passing a reserved identifier as a key to TransactionContext methods.
     *
     * If an Agent is present, the underlying 'real' method calls may throw exceptions of their own, which will be
     * dispatched by the same mechanism.
     *
     * If client code requires the exception to bubble up into their code, rethrow any RuntimeException with the supplied
     * exception as its inner cause e.g. InvocationTargetException, or any exception which makes sense to the client application.
     *
     * @param call the call which caused the error
     * @param args the arguments passed to the call which produced the error
     * @param t the exception/error thrown during execution of an Agent method call.
     */
    void handleUncaughtException(ReflectiveCall call, Object[] args, Throwable t);

    /**
     * Helper method to install an instance of an UncaughtExceptionHandler.
     * @param handler the handler to install, or null to clear a previously installed handler.
     */
    static void install(UncaughtExceptionHandler handler) {
        ReflectiveCall.installUncaughtExceptionHandler(handler);
    }
}
