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

package com.amazon.disco.agent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Spy
    private Utils utils;

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    private Map<String, String> fakeEnv = new HashMap<String, String>();

    private File buildExpectedFolder(File parent) {
        return new File(new File(new File(new File(parent,
                "var"),
                "tmp"),
                "user"),
                "com.amazon.disco.agent");
    }

    @Before
    public void beforeTest() {
        doReturn(fakeEnv).when(utils).env();
    }

    @Test
    public void testApollo() throws Exception {
        // Let's check if there is no env system temp is used
        File tmp = utils.getTempFolder();
        assertEquals(new File(System.getProperty("java.io.tmpdir"), "com.amazon.disco.agent").getAbsolutePath(), tmp.getAbsolutePath());
    }

    @Test
    public void testEnvRoot() throws Exception {
        File fakeRoot = folder.newFolder("envroot");
        fakeEnv.put("ENVROOT", fakeRoot.getAbsolutePath());
        assertEquals(buildExpectedFolder(fakeRoot).getAbsolutePath(), utils.getTempFolder().getAbsolutePath());
    }

    @Test
    public void testApolloActualEnvRoot() throws Exception {
        File fakeRoot = folder.newFolder("act1");
        fakeEnv.put("APOLLO_ACTUAL_ENVIRONMENT_ROOT", fakeRoot.getAbsolutePath());
        assertEquals(buildExpectedFolder(fakeRoot).getAbsolutePath(), utils.getTempFolder().getAbsolutePath());
    }

    @Test
    public void testApolloCanonicalEnvRoot() throws Exception {
        File fakeRoot = folder.newFolder("cann");
        fakeEnv.put("APOLLO_CANONICAL_ENVIRONMENT_ROOT", fakeRoot.getAbsolutePath());
        assertEquals(buildExpectedFolder(fakeRoot).getAbsolutePath(), utils.getTempFolder().getAbsolutePath());
    }
}