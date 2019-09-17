package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.interception.Installable;
import com.amazon.disco.agent.interception.Package;

import java.util.Arrays;
import java.util.Collection;

/**
 * Definition of the Concurrency Support package, containing all Installables required.
 */
public class ConcurrencySupport implements Package {
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Installable> get() {
        return Arrays.asList(
                new ExecutorInterceptor(),
                new ForkJoinPoolInterceptor(),
                new ForkJoinTaskInterceptor(),
                new ThreadInterceptor(),
                new ThreadSubclassInterceptor()
        );
    }
}
