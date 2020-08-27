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

package software.amazon.disco.agent.web.servlet;

import software.amazon.disco.agent.web.HeaderAccessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface HttpServletResponseAccessor extends HeaderAccessor {
    /**
     * Get a collection of all header names from the servlet response
     * @return collection of header names
     */
    Collection<String> getHeaderNames();

    /**
     * Get the value of the named header
     * @param name the name of the header
     * @return the value of the named header
     */
    String getHeader(String name);

    /**
     * get the status code from the response
     * @return the status code
     */
    int getStatus();

    /**
     * {@inheritDoc}
     */
    @Override
    default Map<String, String> retrieveHeaderMap() {
        Map<String, String> ret = new HashMap<>();
        try {
            Collection<String> headerNames = getHeaderNames();
            if (headerNames == null) {
                return ret;
            }

            for (String name : headerNames) {
                try {
                    ret.put(name, getHeader(name));
                } catch (Throwable t) {
                    //do nothing
                }
            }
        } catch (Throwable t) {
            //do nothing
        }

        return ret;
    }
}
