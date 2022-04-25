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

package software.amazon.disco.agent.coroutines;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatcherUtil {
    /**
     * Helper function to test the method matcher against an input class
     *
     * @param methodName name of method.
     * @param classToIntercept class we are verifying contains the method.
     * @param methodMatcher to match against.
     */
    public static void methodMatches(String methodName,
                                     Class classToIntercept,
                                     ElementMatcher.Junction<? super MethodDescription> methodMatcher)  {
        List<Method> methodList = Arrays.stream(classToIntercept.getDeclaredMethods())
            .filter(r -> r.getName().equals(methodName))
            .collect(Collectors.toList());

        assertEquals(1, methodList.size());
        assertTrue(methodMatcher.matches(new MethodDescription.ForLoadedMethod(methodList.get(0))));
    }

    /**
     * Helper function to test the class matcher matching

     * @param classToIntercept Class type we are validating.
     * @param classMatcher to match against.
     */
    public static void classMatches(Class classToIntercept, ElementMatcher<TypeDescription> classMatcher) {
        assertTrue(classMatcher.matches(new TypeDescription.ForLoadedType(classToIntercept)));
    }
}
