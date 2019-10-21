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

package software.amazon.disco.agent.event;


import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * DiSCo provides an event bus for various events such as Activities being called, threads being entered, and downstream
 * services being called.
 */
public class EventBus {
    private static Logger log = LogManager.getLogger(EventBus.class);
    static Map<Integer, Set<Listener>> listeners;

    /**
     * static initializer, returning the EventBus listeners to their initial state
     */
    static {
        init();
    }

    /**
     * Initialize the EventBus listeners to a default state
     */
    static private void init() {
        //sorted by priority
        listeners = new TreeMap<>();
    }

    /**
     * Publish an event which will be broadcast to all listeners
     * @param e the event to publish
     */
    static public void publish(Event e)  {
        if (e == null) {
            return;
        }

        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Core) publishing event " + e + " from origin " + e.getOrigin());
        }

        for (Set<Listener> listenerSet : listeners.values()) {
            for (Listener l : listenerSet) {
                l.listen(e);
            }
        }
    }

    /**
     * Add a listener to the EventBus
     * @param l the listener to add
     */
    static public void addListener(Listener l) {
        if (l == null) {
            return;
        }

        //todo have private 'system' level priorities which only internal bus listeners can use
        if (!listeners.containsKey(l.getPriority())) {
            listeners.put(l.getPriority(), new HashSet<>());
        }
        listeners.get(l.getPriority()).add(l);
    }

    /**
     * Remove a listener from the EventBus
     * @param l the listener to remove. It is safe to remove a listener not currently added.
     */
    static public void removeListener(Listener l) {
        if (l == null) {
            return;
        }

        if (listeners.containsKey(l.getPriority())) {
            listeners.get(l.getPriority()).remove(l);
        }
    }

    /**
     * Remove all listeners from the EventBus, returning it to its initial state
     */
    static public void removeAllListeners() {
        init();
    }

    /**
     * Test if a given listener is currently registered with the EventBus
     * @param listener the listener to test for
     * @return true if the listener is presently registered to receive events
     */
    static public boolean isListenerPresent(Listener listener) {
        Collection<Listener> listenersByPriority = listeners.get(listener.getPriority());
        if (listenersByPriority != null ){
            if (listenersByPriority.contains(listener)) {
                return true;
            }
        }

        return false;
    }
}
