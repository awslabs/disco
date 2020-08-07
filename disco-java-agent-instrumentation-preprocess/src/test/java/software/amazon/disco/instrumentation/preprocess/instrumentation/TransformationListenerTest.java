package software.amazon.disco.instrumentation.preprocess.instrumentation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;

@RunWith(MockitoJUnitRunner.class)
public class TransformationListenerTest {
    static DynamicType mockDynamicTypeWithAuxiliary;
    static TransformationListener mockListener;
    static String uid = "112233";

    @Mock
    TypeDescription mockTypeDescription;

    @Before
    public void before() {
        mockListener = Mockito.spy(new TransformationListener(uid));
        mockDynamicTypeWithAuxiliary = MockEntities.makeMockDynamicTypeWithAuxiliaryClasses();

        Mockito.when(mockTypeDescription.getInternalName()).thenReturn(MockEntities.makeClassPaths().get(0));
    }

    @After
    public void after() {
        TransformationListener.getInstrumentedTypes().clear();
    }

    @Test
    public void testOnTransformationWorksAndInvokesCollectDataFromEvent() {
        Mockito.doCallRealMethod().when(mockListener).onTransformation(mockTypeDescription, null, null, false, mockDynamicTypeWithAuxiliary);

        mockListener.onTransformation(mockTypeDescription, null, null, false, mockDynamicTypeWithAuxiliary);

        Mockito.verify(mockListener).collectDataFromEvent(mockTypeDescription, mockDynamicTypeWithAuxiliary);
    }

    @Test
    public void testCollectDataFromEventWorksAndPopulatesMapWithNewEntries() {
        TransformationListener mockListener = Mockito.spy(new TransformationListener(uid));
        Mockito.doCallRealMethod().when(mockListener).collectDataFromEvent(mockTypeDescription, mockDynamicTypeWithAuxiliary);

        mockListener.collectDataFromEvent(mockTypeDescription, mockDynamicTypeWithAuxiliary);

        Assert.assertTrue(TransformationListener.getInstrumentedTypes().size() == 3);
    }

    @Test
    public void testCollectDataFromEventWorksAndUpdatesExistingEntriesFromMap() {
        DynamicType updatedType = MockEntities.makeMockDynamicType();

        Mockito.doCallRealMethod().when(mockListener).collectDataFromEvent(mockTypeDescription, mockDynamicTypeWithAuxiliary);
        Mockito.doCallRealMethod().when(mockListener).collectDataFromEvent(mockTypeDescription, updatedType);

        mockListener.collectDataFromEvent(mockTypeDescription, mockDynamicTypeWithAuxiliary);
        mockListener.collectDataFromEvent(mockTypeDescription, updatedType);

        Assert.assertTrue(TransformationListener.getInstrumentedTypes().size() == 3);
    }
}
