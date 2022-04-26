/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * A composable interface that Disco Events can implement if consumers of those events should be able to manipulate
 * the headers of the request associated with the event.
 */
public interface HeaderReplaceable {

    /**
     * Creates the provided header if it does not exist yet, or replaces the header if it does already exist.
     * This should be overridden in a concrete Event class if the functionality is available.
     *
     * @param key - key of the header to create or replace
     * @param value - value of the header
     * @return true if the header is successfully replaced
     */
    boolean replaceHeader(String key, String value);
}
