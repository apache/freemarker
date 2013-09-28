package freemarker.ext.beans;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModel;

@RunWith(JUnit4.class)
public class BeansWrapperMiscTest {

    @Test
    public void booleans() throws Exception {
        final BeansWrapper bw = new BeansWrapper();

        assertTrue(((TemplateBooleanModel) bw.wrap(Boolean.TRUE)).getAsBoolean());
        assertFalse(((TemplateBooleanModel) bw.wrap(Boolean.FALSE)).getAsBoolean());
        
        TemplateHashModel tm = (TemplateHashModel) bw.wrap(Boolean.TRUE);
        assertNotNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        assertNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
        assertNull(tm.get("hashCode"));
        assertNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        assertNotNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        
        assertSame(tm, bw.wrap(Boolean.TRUE));
    }
    
}
