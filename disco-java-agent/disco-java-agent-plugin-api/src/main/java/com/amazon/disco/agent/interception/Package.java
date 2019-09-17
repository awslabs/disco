package com.amazon.disco.agent.interception;

import java.util.Collection;

/**
 * Abstraction around a collection of Installables, so that products can install a whole package, without
 * needing to know the types of each Installable, which is effectively an implementation detail.
 */
public interface Package {
    /**
     * Return a list/set of this package's Installables
     * @return a list/set of this package's Installables
     */
    Collection<Installable> get();
}
