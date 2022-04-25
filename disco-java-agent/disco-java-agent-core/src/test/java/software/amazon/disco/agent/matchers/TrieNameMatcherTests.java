/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.matchers;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Test class for TrieNameMatcher.
 */

public class TrieNameMatcherTests {
    private static final String EMPTY = "";
    private static final int THREAD_COUNT = 10;
    private static final String[] PREFIXES = new String[] {"javax.management.", "com.sun.", "javax.security.", "java.text."
            ,"java.io."};
    private final Map<String, Boolean> MATCH_TEST_CASES = new HashMap<String, Boolean>() {
        {
            put("org.apache.commons", false); // case to check if no prefix in Trie
            put("com.sun.jmx.mbeanserver.GetPropertyAction", true); // case to check if matching with prefixes in Trie
            put("java.io.", true); // case to verify matching exact classes
            put("jdk", false); // case to check when word smaller than prefix
        }};
    private static final String[] matches = new String[] {"com.sun.jmx.mbeanserver.GetPropertyAction", "java.io."};
    private static final String[] notMatches = new String[] {"jdk", "org.apache.commons"};
    private TrieNameMatcher.Trie testTrie;
    private static final String CLASS_TO_INSERT = "sun.rmi.";
    private ElementMatcher matcher;

    @Mock
    private NamedElement namedElement;

    @Before
    public void setup() {
        matcher = new TrieNameMatcher(PREFIXES);
        namedElement = Mockito.mock(NamedElement.class);
        testTrie = new TrieNameMatcher.Trie();
        for (String entry: PREFIXES) {
            testTrie.insert(entry);
        }
    }

    /**
     * Test cases with matches.
     */
    @Test
    public void testMatches() {
        for (String className : matches) {
            Mockito.when(namedElement.getActualName()).thenReturn(className);
            Assert.assertTrue(matcher.matches(namedElement));
        }
    }

    /**
     * Test cases with no matches.
     */
    @Test
    public void testNoMatches() {
        for (String className : notMatches) {
            Mockito.when(namedElement.getActualName()).thenReturn(className);
            Assert.assertFalse(matcher.matches(namedElement));
        }
    }

    /**
     * Test if matches work as expected in case of multithreaded scenario.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testMultiThreading() throws InterruptedException, ExecutionException {
        Set<String> keySet = MATCH_TEST_CASES.keySet();
        List<String> keyList = new ArrayList<>(keySet);
        int size = keyList.size();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            String classKey = keyList.get(new Random().nextInt(size));
            tasks.add(new TrieNameMatcherCallable(classKey, MATCH_TEST_CASES.get(classKey)));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = executorService.invokeAll(tasks);
        for (Future<Boolean> future : futures) {
            Assert.assertTrue(future.get());
        }
        Assert.assertEquals(THREAD_COUNT, futures.size());
    }

    /**
     * Helper class to create Callable.
     */
    class TrieNameMatcherCallable implements Callable<Boolean> {
        String forClassname;
        Boolean expectedResult;
        NamedElement mockNamedElement;

        TrieNameMatcherCallable(String className, Boolean expectedResult) {
            this.forClassname = className;
            this.expectedResult = expectedResult;
            mockNamedElement = Mockito.mock(NamedElement.class);
        }

        @Override
        public Boolean call() {
            Mockito.when(mockNamedElement.getActualName()).thenReturn(forClassname);
            return (expectedResult == matcher.matches(mockNamedElement));
        }
    }

    /**
     *  Below are the tests for {@link TrieNameMatcher.Trie} Trie class.
     */

    /**
     * Test insert function.
     */
    @Test
    public void testInsert() {
        testTrie.insert(CLASS_TO_INSERT);
        Assert.assertTrue(testTrie.exactMatch(CLASS_TO_INSERT));
    }

    /**
     * Test no exception in case of null or empty inserts.
     */
    @Test
    public void testNullOrEmptyInsert() {
        testTrie.insert(null);
        testTrie.insert(EMPTY);
    }

    /**
     * Test no exception in case of null or empty className to be checked in Trie.
     */
    @Test
    public void testIfNullOrEmptyPrefixMatch() {
        Assert.assertFalse(testTrie.prefixMatch(null));
        Assert.assertFalse(testTrie.prefixMatch(EMPTY));
    }

    /**
     * Test if matches function matches for the different input cases present
     * in MATCH_TEST_CASES hashmap with the corresponding values in hashmap.
     */
    @Test
    public void testPrefixMatch() {
        testTrie.insert(CLASS_TO_INSERT);
        testTrie.exactMatch(CLASS_TO_INSERT);
        for(Map.Entry<String, Boolean> classVerify : MATCH_TEST_CASES.entrySet()) {
            Assert.assertEquals(classVerify.getValue(), testTrie.prefixMatch(classVerify.getKey()));
        }
    }

    /**
     * Test if matchInTrie function results correctly in case of exactMatch.
     * True for the words present in PREFIXES and false for "java.util.List" which isn't in the Trie.
     */
    @Test
    public void testExactMatch() {
        for(String className : PREFIXES) {
            Assert.assertTrue(testTrie.exactMatch(className));
        }
        Assert.assertFalse(testTrie.exactMatch("java.util.List"));
    }
}
