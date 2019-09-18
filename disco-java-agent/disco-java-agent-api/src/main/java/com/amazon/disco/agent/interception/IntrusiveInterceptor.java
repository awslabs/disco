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

package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.event.Event;


/**
 *  An interceptor that is intrusive to the code being intercepted. Participating Installables can delegate
 *  to any installed IntrusiveInterceptor, in the InstrusiveInterceptorRegistry, in order for Agents to perform intrusive
 *  or mutative actions on the running code.
 */
public interface IntrusiveInterceptor  {

    /**
     * A POJO context object to be passed from the decide() method to other methods, to communicate between them.
     */
    final class ContinuationContext {
        final private Decision decision;
        final private Event event;
        final private Object userData;

        /**
         * Create a new ContinuationContext containing the specified Decision, with surround event and userData metadata.
         * @param decision the Decision
         * @param event the Event
         * @param userData any arbitrary user data, or null.
         */
        ContinuationContext(Decision decision, Event event, Object userData) {
            this.decision = decision;
            this.event = event;
            this.userData = userData;
        }

        /**
         * Factory method to create a new ContinuationContext which is a Continue decision
         * @param event any Event to attach to this context
         * @param userData any userData to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asContinue(Event event, Object userData) {
            return new ContinuationContext(Decision.CONTINUE, event, userData);
        }

        /**
         * Factory method to create a new ContinuationContext which is a Continue decision
         * @param event any Event to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asContinue(Event event) {
            return asContinue(event, null);
        }

        /**
         * Factory method to create a new ContinuationContext which is a Replace decision
         * @param event any Event to attach to this context
         * @param userData any userData to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asReplace(Event event, Object userData) {
            return new ContinuationContext(Decision.REPLACE, event, userData);
        }

        /**
         /**
         * Factory method to create a new ContinuationContext which is a Replace decision
         * @param event any Event to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asReplace(Event event) {
            return asReplace(event, null);
        }

        /**
         * Factory method to create a new ContinuationContext which is a Transform decision
         * @param event any Event to attach to this context
         * @param userData any userData to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asTransform(Event event, Object userData) {
            return new ContinuationContext(Decision.TRANSFORM, event, userData);
        }

        /**
         * Factory method to create a new ContinuationContext which is a Replace decision
         * @param event any Event to attach to this context
         * @return a newly constructed ContinuationContext as requested
         */
        static public ContinuationContext asTransform(Event event) {
            return asTransform(event, null);
        }

        /**
         * Getter for the contained Decision.
         * @return the decision, one of Continue;Replace;Transform
         */
        public Decision getDecision() {
            return decision;
        }

        /**
         * Get the contained event
         * @return the event
         */
        public Event getEvent() {
            return event;
        }

        /**
         * Return any stored user-specified contextual data.
         * @return the stored userData, or null if absent
         */
        public Object getUserData() {
            return userData;
        }
    }

    /**
     * All IntrusiveInterceptors must implement the decide() method, where the incoming Event may be inspected to decide whether
     * code should Continue, or whether mutative behaviour should occur. Note that the interception producing the call to decide()
     * my not necessarily have published the Event to the bus, nor is it obligated to do so afterward. In any case,
     * IntrusiveInterceptors should not publish the given event to the EventBus, unless certain that they control all possible
     * Listeners, and that those Listeners can handle the publication.
     *
     * @param event an informative event about the surrounding context of the interception e.g. a ServiceDownstreamRequestEvent
     * @return a Continue, Replace or Transform decision
     */
    ContinuationContext decide(Event event);

    /**
     * If the decide() method produced a Replace decision, this method should be called next by the intrusive interception.
     * The real piece of code intercepted will not have been called (nor ever will be). If the caller expects a returned value,
     * it will be of type T, and should be returned/synthesized by this method.
     *
     * @param context the decision and metadata previously returned by the decide() method of this IntrusiveInterceptor.
     * @return if the calling code expects a return value, which is dependent on the interception taking place, this method should
     *       return something compatible.
     * @throws Throwable any kind of Throwable could conceivably be thrown
     */
    default Object replace(ContinuationContext context) throws Throwable {
        throw new IllegalStateException("DiSCo(Interfaces) if your logic allows replace() to be called, you must implement it with a body which makes sense");
    }

    /**
     * If the decide() method produced a Transform decision, this method should be called next by the intrusive interception.
     * The real intercepted code has already been called by the intrusive interception, and if it returned a value it will be given
     * as an argument, likewise any exception it threw instead.
     *
     * @param context the decision and metadata previously returned by the decide() method of this IntrusiveInterceptor.
     * @param realOutput the output returned by the intercepted code, if any
     * @param thrown any exception thrown by the intercepted code, or null if none was thrown
     * @return if the calling code expects a return value, which is dependent on the interception taking place, this method should
     * return something compatible. It is a sensible default behavior to return realOutput if no other action needs to be taken (or
     * to rethrow thrown if not null). If synthesizing a new output, it ought to be convertible to whatever type realOutput is.
     *
     * @throws Throwable any kind of Throwable could conceivably be thrown
     */
    default Object transform(ContinuationContext context, Object realOutput, Throwable thrown) throws Throwable {
        if (thrown != null) throw thrown;
        return realOutput;
    }

    /**
     *  A container for all possible Intrusive interceptor Decisions
     */
    enum Decision {

        /**
         * Indicates that no mutative action should be taken, and that the intercepted
         * code should continue normally.
         */
        CONTINUE,

        /**
         * Indicates that the entirety of the intercepted code will not be executed, and instead
         * can be replaced with whatever code is implemented in the replace() method.
         * <p>
         * For Installables which use Advice rather than MethodDelegation this value needs to be converted into an object
         * instance and passed to the onSkip value within the Advice.OnMethodEnter annotation, to denote that the real
         * method should not be called. The method annotated with @Advice.OnMethodExit can then return a new or synthesized
         * return value from the intercepted method, by writing to its argument using a writable @Advice.Return
         * annotation e.g.
         * <p>
         * \@Advice.Return(readOnly = false, typing = DYNAMIC) Object returned
         */
        REPLACE,

        /**
         * Indicates that the original intercepted code will be executed, but before the result
         * is returned to the caller, or exception thrown, the transform() method can mutate the return value or thrown exception,
         * or perform logic based on the response or upon the exception. If the program's behavior will not be different *to the calling code*
         * agent authors should consider using the EventBus Listener pattern instead, to merely inspect the program behaviour.
         */
        TRANSFORM
    }
}
