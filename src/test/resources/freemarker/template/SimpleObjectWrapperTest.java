package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.junit.Test;

import freemarker.template.DefaultObjectWrapperTest.TestBean;

public class SimpleObjectWrapperTest {
    
    @Test
    public void testBasics() throws TemplateModelException {
        {
            SimpleObjectWrapper ow = new SimpleObjectWrapper(Configuration.VERSION_2_3_22);
            testCustomizationCommonPart(ow);
            assertTrue(ow.wrap(Collections.emptyMap()) instanceof SimpleMapAdapter);
            assertTrue(ow.wrap(Collections.emptyList()) instanceof SimpleListAdapter);
            assertTrue(ow.wrap(new boolean[] { }) instanceof SimpleArrayAdapter);
            assertTrue(ow.wrap(new HashSet()) instanceof SimpleSequence);  // at least until IcI 2.4
        }
        
        {
            SimpleObjectWrapper ow = new SimpleObjectWrapper(Configuration.VERSION_2_3_21);
            testCustomizationCommonPart(ow);
            assertTrue(ow.wrap(Collections.emptyMap()) instanceof SimpleHash);
            assertTrue(ow.wrap(Collections.emptyList()) instanceof SimpleSequence);
            assertTrue(ow.wrap(new boolean[] { }) instanceof SimpleSequence);
            assertTrue(ow.wrap(new HashSet()) instanceof SimpleSequence);
        }
    }

    @SuppressWarnings("boxing")
    private void testCustomizationCommonPart(SimpleObjectWrapper ow) throws TemplateModelException {
        assertFalse(ow.isWriteProtected());
        
        assertTrue(ow.wrap("x") instanceof SimpleScalar);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        try {
            ow.wrap(new TestBean());
            fail();
        } catch (TemplateModelException e) {
            assertTrue(e.getMessage().contains("type"));
        }
    }
    
}
