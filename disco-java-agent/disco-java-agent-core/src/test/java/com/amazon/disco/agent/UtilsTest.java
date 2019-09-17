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
                "alphaone");
    }

    @Before
    public void beforeTest() {
        doReturn(fakeEnv).when(utils).env();
    }

    @Test
    public void testApollo() throws Exception {
        // Let's check if there is no env system temp is used
        File tmp = utils.getTempFolder();
        assertEquals(new File(System.getProperty("java.io.tmpdir"), "alphaone").getAbsolutePath(), tmp.getAbsolutePath());
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