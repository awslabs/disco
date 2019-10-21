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

package com.amazon.disco.agent.concurrent;

import java.util.HashSet;
/**
 * Encapsulates metadata values and tags so that they can be stored in a single
 * thread local map in TransactionContext.
 *
 * Tags can be used to group metadata tags for specific features or
 * functionality.  The initial use case for tags is to specify which metadata
 * should be propagated with downstream calls.
 *
 */
public class MetadataItem {
    private Object value;
    private HashSet<String> tags = new java.util.HashSet<>();


    /**
     * Construct a new MetadataItem.
     *
     * @param value The value for this MetadataItem.
     */
    public MetadataItem(Object value) {
        this.value = value;

    }

    /**
     * Sets or updates the value for this MetadataItem.
     *
     * @param value The new or updated value for this MetadataItem.
     */
    public void set(Object value) {
        this.value = value;
    }

    /**
     * Gets the value for this MetadataItem
     *
     * @return The value of this MetadataItem.
     */
    public Object get() {
        return value;
    }

    /**
     * Specify a tag for this MetadataItem.  Duplicate tags will not create.
     * additional tag entries.
     *
     * @param tag The tag to be attributed to this MetadataItem.
     */
    public void setTag(String tag) {
        tags.add(tag);
    }

    /**
     * Get the set of tags for this MetadataItem.
     *
     * @return the unique set of tags for this MetadataItem.
     */
    public java.util.Set<String> getTags() {
        return tags;
    }

    /**
     * Check if a specific tag exists for this MetadataItem
     * @param tag a tag to inspect on this metadata
     * @return True if the tag exists.
     */
    public Boolean hasTag(String tag) {
        if (tags.contains(tag)) {
            return true;
        }
        return false;
    }


    /**
     * Clear a specific tag from this MetadataItem
     *
     * @param tag The tag to be cleared.
     */
    public void clearTag(String tag) {
        tags.remove(tag);
    }

}
