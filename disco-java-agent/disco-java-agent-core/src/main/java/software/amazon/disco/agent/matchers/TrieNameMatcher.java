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
import java.util.HashMap;

/**
 * An element matcher to match if given string starts with certain prefixes based on Trie {https://en.wikipedia.org/wiki/Trie}
 *
 * {@link TrieNameMatcher.Trie}.
 */
public class TrieNameMatcher<T extends NamedElement> extends ElementMatcher.Junction.AbstractBase<T> {

    final Trie nameMatcher = new Trie();

    /**
     * Initialize the TrieMatcher by inserting the set of prefixes to be matched against using this matcher.
     * @param prefixes set of prefixes to be inserted
     */
    public TrieNameMatcher(String[] prefixes) {
        for (String item : prefixes) {
            nameMatcher.insert(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(T target) {
        return nameMatcher.prefixMatch(target.getActualName());
    }

    /**
     * Defines the Trie structure where each node of Trie is
     * {@link TrieNameMatcher.Trie.TrieNode}
     *                                      root
     *                                     / \ \
     *                                    c  j  a
     *                                   /  /\ /\
     *                                 o  a d b c
     */
    static class Trie {

        private final TrieNode root;

        /**
         * Defines each element(node) of the Trie in {@link TrieNameMatcher.Trie}
         *                          TrieNode of root looks like - { (c, TrieNode()), (j, TrieNode()), (a, TrieNode()) }
         *                          TrieNode of j looks like - { (a, TrieNode()), (d, TrieNode()) }
         *                          TrieNode of a looks like - {}
         */
        static class TrieNode {
            /**
             * Each node has list of children which denotes the next possible characters from the current node.
             */
            private HashMap<Character, TrieNode> children;

            /**
             * Initialize a node with empty set of children (leaf node).
             */
            TrieNode() {
                children = new HashMap<Character, TrieNode>();
            }

            /**
             * Checks if given character ch is child of the current node.
             *
             * @param ch character to be checked.
             * @return true if find ch in the set of children of the node otherwise false.
             */
            boolean containsKey(char ch) {
                return children.containsKey(ch);
            }

            /**
             * Returns the child node of the node corresponding to the given character.
             *
             * @param ch character for which the child node is needed.
             * @return child node corresponding to the character ch.
             */
            TrieNode get(char ch) {
                return children.get(ch);
            }

            /**
             * Insert a new child node in the set of children for the given character.
             *
             * @param ch   character for which the child node needs to be inserted
             * @param node the node value for the character ch
             */
            void put(char ch, TrieNode node) {
                children.put(ch, node);
            }

            /**
             * Checks if the node is leaf node i.e. have empty set of children.
             *
             * @return true if the node is leaf node otherwise false.
             */
            boolean isLeaf() {
                return children.isEmpty();
            }
        }

        /**
         * Enum to specify type of matches on the Trie.
         */
        enum MatchType {
            PREFIX, // match the target word's prefixes in the Trie
            EXACT // match the target word in Trie
        }

        /**
         * Creates a new Trie.
         */
        Trie() {
            root = new TrieNode();
        }

        /**
         * Insert a given word in the Trie.
         *
         * @param word word to be inserted.
         */
        void insert(String word) {
            if (word == null || word.isEmpty()) return;
            TrieNode node = root;
            char[] chars = word.toCharArray();
            for (char aChar : chars) {
                if (!node.containsKey(aChar)) {
                    node.put(aChar, new TrieNode());
                }
                node = node.get(aChar);
            }
        }

        /**
         * Matches the given word in the Trie as if the word's prefixes matches with any of the prefixes in the Trie.
         *
         * @param word the given word to checked for matching prefix in the Trie.
         * @return boolean true if found the matching prefix in the Trie, otherwise false.
         */
        boolean prefixMatch(String word) {
            return match(word, MatchType.PREFIX);
        }

        /**
         * Matches the given word in the Trie as if the word matches exactly with any of the prefixes present in the Trie.
         *
         * @param word the given word to checked for exact matching in the Trie.
         * @return boolean true if found the exact word in the Trie, otherwise false.
         */
        boolean exactMatch(String word) {
            return match(word, MatchType.EXACT);
        }

        /**
         * Matches the given word in the Trie given the type of match.
         *
         * @param word          the given word to be checked for a match in the Trie.
         * @param matchType     specifies the type of match either PREFIX (to match if the given word's prefixes match in the Trie) or
         *                      EXACT (to match if given word present in trie).
         * @return boolean true if found the matching given the match type in the Trie, otherwise false.
         */
        private boolean match(String word, MatchType matchType) {
            if (word == null || word.isEmpty()) return false;
            TrieNode node = root;
            char[] chars = word.toCharArray();
            for (char aChar : chars) {
                if (matchType.equals(MatchType.PREFIX) && node.isLeaf()) {
                    return true;
                } else if (node.containsKey(aChar)) {
                    node = node.get(aChar);
                } else {
                    return false;
                }
            }
            return node.isLeaf();
        }
    }
}

