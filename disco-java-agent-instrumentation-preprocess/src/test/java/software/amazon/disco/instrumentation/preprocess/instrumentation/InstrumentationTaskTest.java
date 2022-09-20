/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;
import software.amazon.disco.instrumentation.preprocess.export.JarExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.CacheStrategy;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class InstrumentationTaskTest {
    InstrumentationTask task;
    Map<String, InstrumentationArtifact> artifactMap;
    PreprocessConfig config;
    File source;
    SourceInfo sourceInfo;
    CacheStrategy strategy;

    @Mock
    JarExportStrategy exportStrategy;

    @Mock
    JarLoader jarLoader;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        Map<String, byte[]> byteArrayMap = new HashMap<>();
        byteArrayMap.put("ClassA", "ClassA".getBytes());
        byteArrayMap.put("ClassB", "ClassB".getBytes());

        source = tempFolder.newFile("someJar");
        sourceInfo = new SourceInfo(source, exportStrategy, byteArrayMap, null);

        strategy = Mockito.mock(CacheStrategy.class);
        config = PreprocessConfig.builder().cacheStrategy(strategy).build();

        task = Mockito.spy(new InstrumentationTask(jarLoader, source.toPath(), config, "lib"));
        artifactMap = new HashMap<>();
        artifactMap.put("ClassA", new InstrumentationArtifact(byteArrayMap.get("ClassA")));
        artifactMap.put("ClassB", new InstrumentationArtifact(byteArrayMap.get("ClassB")));

        Mockito.doReturn(sourceInfo).when(jarLoader).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));
        Mockito.doReturn(artifactMap).when(task).getInstrumentationArtifacts();
    }

    @After
    public void after() {
        TransformerExtractor.getTransformers().clear();
    }

    @Test
    public void testApplyInstrumentationWorksAndReturnsCompletedStatus() throws IllegalClassFormatException, PreprocessCacheException {
        configureTransformerExtractor(null);

        InstrumentationOutcome outcome = task.applyInstrumentation();

        Mockito.verify(task).applyInstrumentationOnClass("ClassA", artifactMap.get("ClassA").getClassBytes());
        Mockito.verify(task).applyInstrumentationOnClass("ClassB", artifactMap.get("ClassB").getClassBytes());
        Mockito.verify(exportStrategy).export(sourceInfo, artifactMap, config, "lib");
        Mockito.verify(task).clearInstrumentationArtifacts();

        assertNotNull(outcome);
        assertEquals(InstrumentationOutcome.Status.COMPLETED, outcome.getStatus());
        assertEquals(source.getAbsolutePath(), outcome.getSourcePath());
        Mockito.verify(strategy).cacheSource(Paths.get(source.getAbsolutePath()));
    }

    @Test
    public void testApplyInstrumentationWorksAndReturnsNoOPStatus_WhenNoClassInstrumented() throws IllegalClassFormatException, PreprocessCacheException {
        Mockito.doReturn(sourceInfo).when(jarLoader).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));

        // returns an empty map indicating no classes were instrumented
        Mockito.doReturn(Collections.emptyMap()).when(task).getInstrumentationArtifacts();

        configureTransformerExtractor(null);

        InstrumentationOutcome outcome = task.applyInstrumentation();

        assertNotNull(outcome);
        assertEquals(InstrumentationOutcome.Status.NO_OP, outcome.getStatus());
        assertEquals(source.getAbsolutePath(), outcome.getSourcePath());
        Mockito.verify(strategy).cacheSource(Paths.get(source.getAbsolutePath()));
    }

    @Test
    public void testApplyInstrumentationWorksAndReturnsNoOPStatus_WhenNoClassExtractedFromJar() throws IllegalClassFormatException, PreprocessCacheException {
        Mockito.doReturn(new SourceInfo(source, null, Collections.emptyMap())).when(jarLoader).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));
        Mockito.doReturn(Collections.emptyMap()).when(task).getInstrumentationArtifacts();
        configureTransformerExtractor(null);

        InstrumentationOutcome outcome = task.applyInstrumentation();

        Mockito.verify(task, Mockito.never()).applyInstrumentationOnClass(Mockito.anyString(), Mockito.any(byte[].class));
        assertNotNull(outcome);
        assertEquals(InstrumentationOutcome.Status.NO_OP, outcome.getStatus());
        assertEquals(source.getAbsolutePath(), outcome.getSourcePath());

        Mockito.verify(strategy).cacheSource(Paths.get(source.getAbsolutePath()));
    }

    @Test
    public void testApplyInstrumentationWorksAndReturnsWarningOccurredStatus() throws IllegalClassFormatException, PreprocessCacheException {
        // one of the configured transformers will throw this exception wrapped in a 'InstrumentationException' when processing 'ClassB'.
        configureTransformerExtractor(new IllegalStateException());

        InstrumentationOutcome outcome = task.applyInstrumentation();

        Mockito.verify(task).applyInstrumentationOnClass("ClassA", artifactMap.get("ClassA").getClassBytes());
        Mockito.verify(task).applyInstrumentationOnClass("ClassB", artifactMap.get("ClassB").getClassBytes());
        Mockito.verify(exportStrategy).export(sourceInfo, artifactMap, config, "lib");
        Mockito.verify(task).clearInstrumentationArtifacts();

        assertNotNull(outcome);
        assertEquals(InstrumentationOutcome.Status.WARNING_OCCURRED, outcome.getStatus());
        assertEquals(source.getAbsolutePath(), outcome.getSourcePath());

        Mockito.verify(strategy, Mockito.never()).cacheSource(Mockito.any());
    }

    @Test(expected = PreprocessCacheException.class)
    public void testApplyInstrumentationFails_whenCachingFails() throws IllegalClassFormatException, PreprocessCacheException {
        Mockito.doReturn(new SourceInfo(source, null, Collections.emptyMap())).when(jarLoader).load(Mockito.any(Path.class), Mockito.any(PreprocessConfig.class));
        Mockito.doReturn(Collections.emptyMap()).when(task).getInstrumentationArtifacts();
        configureTransformerExtractor(null);

        Mockito.doThrow(new PreprocessCacheException("ops")).when(strategy).cacheSource(Mockito.any());
        task.applyInstrumentation();
    }

    @Test(expected = InstrumentationException.class)
    public void testApplyInstrumentationFailsAndPropagatesUnCaughtException() throws IllegalClassFormatException, PreprocessCacheException {
        // This uncaught exception will be thrown by one of the configured transformers
        configureTransformerExtractor(new RuntimeException());

        task.applyInstrumentation();

        Mockito.verify(task).applyInstrumentationOnClass("ClassA", artifactMap.get("ClassA").getClassBytes());
        Mockito.verify(task).applyInstrumentationOnClass("ClassB", artifactMap.get("ClassB").getClassBytes());

        Mockito.verify(strategy, Mockito.never()).cacheSource(Mockito.any());
    }

    @Test
    public void testApplyInstrumentationOnClassWorks() throws IllegalClassFormatException {
        configureTransformerExtractor(null);

        task.applyInstrumentationOnClass("ClassA", "ClassA".getBytes());

        verifyTransformerInteractions(false);
    }

    @Test
    public void testApplyInstrumentationOnClassFailsAndReThrowsExceptionIfFailOnClassNotFoundIsTrue() throws IllegalClassFormatException {
        PreprocessConfig config = PreprocessConfig.builder().failOnUnresolvableDependency(true).build();
        task = Mockito.spy(new InstrumentationTask(jarLoader, source.toPath(), config, "lib"));
        configureTransformerExtractor(new IllegalStateException());

        try {
            task.applyInstrumentationOnClass("ClassA", "ClassA".getBytes());
            Assert.fail();
        } catch (Throwable e) {
            if (!(e instanceof InstrumentationException)) {
                throw e;
            }
        }

        verifyTransformerInteractions(true);
    }

    @Test
    public void testApplyInstrumentationOnClassWorksAndNotReThrowsExceptionIfFailOnClassNotFoundIsFalse() throws IllegalClassFormatException {
        configureTransformerExtractor(new IllegalStateException());

        task.applyInstrumentationOnClass("ClassA", "ClassA".getBytes());

        verifyTransformerInteractions(true);
    }

    private void verifyTransformerInteractions(boolean exceptionThrown) throws IllegalClassFormatException {
        // first transformer
        Mockito.verify(TransformerExtractor.getTransformers().get(0))
            .transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassA"), Mockito.eq(null), Mockito.eq(null), Mockito.eq("ClassA".getBytes()));

        VerificationMode mode;
        if (exceptionThrown) {
            // the first transformer will attempt to instrument 'ClassA', fails and the second transformer will not attempt to instrument the
            // since the same class since 'IllegalStateException' exception will be thrown again.

            // this applies for all exceptions thrown while attempting to instrument a given class.
            mode = Mockito.never();
        } else {
            mode = Mockito.times(1);
        }
        Mockito.verify(TransformerExtractor.getTransformers().get(1), mode)
            .transform(Mockito.any(ClassLoader.class), Mockito.eq("ClassA"), Mockito.eq(null), Mockito.eq(null), Mockito.eq("ClassA".getBytes()));
    }

    private TransformerExtractor configureTransformerExtractor(Exception innerException) throws IllegalClassFormatException {
        TransformerExtractor transformerExtractor = new TransformerExtractor(Mockito.mock(Instrumentation.class));
        ClassFileTransformer transformer_1 = Mockito.mock(ClassFileTransformer.class);
        ClassFileTransformer transformer_2 = Mockito.mock(ClassFileTransformer.class);
        transformerExtractor.addTransformer(transformer_1);
        transformerExtractor.addTransformer(transformer_2);

        // configure the first transformer to throw the provided exception to be wrapped inside an 'InstrumentationException' when
        // instrumenting 'ClassA'
        if (innerException != null) {
            Mockito.doThrow(new InstrumentationException("message", innerException)).when(transformer_1).transform(
                Mockito.any(ClassLoader.class),
                Mockito.eq("ClassA"),
                Mockito.eq(null),
                Mockito.eq(null),
                Mockito.eq("ClassA".getBytes())
            );
        }

        return transformerExtractor;
    }
}
