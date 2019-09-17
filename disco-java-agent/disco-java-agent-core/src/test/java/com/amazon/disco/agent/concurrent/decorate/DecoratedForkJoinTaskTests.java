package com.amazon.disco.agent.concurrent.decorate;

import org.junit.Test;

import java.util.concurrent.ForkJoinTask;

/**
 * All tests can do is throw reflection errors, since unit-tests are agent-not-present scenario
 */
public class DecoratedForkJoinTaskTests {

    @Test(expected = NoSuchFieldException.class)
    public void testCreateThrows() throws Exception {
        ForkJoinTask fjt = new TestForkJoinTask();
        DecoratedForkJoinTask.create(fjt);

    }

    @Test(expected = NoSuchFieldException.class)
    public void testGetThrows() throws Exception {
        ForkJoinTask fjt = new TestForkJoinTask();
        DecoratedForkJoinTask.get(fjt);

    }

    @Test(expected = NoSuchFieldException.class)
    public void testLookupThrows() throws Exception {
        DecoratedForkJoinTask.lookup();
    }

    static class TestForkJoinTask extends ForkJoinTask {
        @Override
        public Object getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Object value) {
        }

        @Override
        protected boolean exec() {
            return false;
        }
    }
}
