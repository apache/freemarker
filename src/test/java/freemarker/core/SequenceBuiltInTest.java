package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.DefaultIterableAdapter;
import freemarker.template.DefaultNonListCollectionAdapter;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;
import freemarker.test.TemplateTest;

public class SequenceBuiltInTest extends TemplateTest {

    @Test
    public void testWithCollection() throws TemplateException, IOException {
        ObjectWrapperWithAPISupport ow = (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper();
        
        TemplateModel xs = DefaultIterableAdapter.adapt(ImmutableSet.of("a", "b"), ow);
        assertThat(xs, not(instanceOf(TemplateCollectionModelEx.class)));
        assertThat(xs, not(instanceOf(TemplateSequenceModel.class)));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            System.out.println(e); //!!T
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");
        
        try {
            assertOutput("${xs?size}", "2");
            fail();
        } catch (TemplateException e) {
            System.out.println(e); //!!T
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence?size}", "2");
    }

    @Test
    public void testWithCollectionEx() throws TemplateException, IOException {
        ObjectWrapperWithAPISupport ow = (ObjectWrapperWithAPISupport) getConfiguration().getObjectWrapper();
        
        TemplateModel xs = DefaultNonListCollectionAdapter.adapt(ImmutableSet.of("a", "b"), ow);
        assertThat(xs, not(instanceOf(TemplateSequenceModel.class)));
        assertThat(xs, instanceOf(TemplateCollectionModelEx.class));
        addToDataModel("xs", xs);

        try {
            assertOutput("${xs[1]}", "b");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?sequence")); // Contains tip to use ?sequence
        }
        assertOutput("${xs?sequence[1]}", "b");

        assertOutput("${xs?size}", "2"); // No need for ?sequence
    }

    @Test
    public void testWithSequence() throws TemplateException, IOException {
        assertOutput("${[11, 12]?sequence[1]}", "12");
        
        
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        // As it returns the sequence as is, it works with an infinite sequence:
        assertOutput("${(11..)?sequence[1]}", "12");
    }

}
