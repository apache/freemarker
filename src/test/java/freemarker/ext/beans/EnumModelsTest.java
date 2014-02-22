package freemarker.ext.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;

@RunWith(JUnit4.class)
public class EnumModelsTest {
    
    @Test
    public void modelCaching() throws Exception {
        BeansWrapper bw = new BeansWrapper(new Version(2, 3, 21));
        TemplateHashModel enums = bw.getEnumModels();
        TemplateHashModel e = (TemplateHashModel) enums.get(E.class.getName());
        assertNotNull(e);
        assertNotNull(e.get("A"));
        assertNotNull(e.get("B"));
        assertNull(e.get("C"));

        try {
            enums.get("no.such.ClassExists");
            fail();
        } catch (TemplateModelException ex) {
            assertTrue(ex.getCause() instanceof ClassNotFoundException);
        }
        
        TemplateModel a = e.get("A");
        assertTrue(a instanceof TemplateScalarModel);
        assertTrue(a instanceof TemplateHashModel);
        assertEquals(((TemplateScalarModel) a).getAsString(), "ts:A");
        TemplateMethodModelEx nameMethod = (TemplateMethodModelEx) ((TemplateHashModel) a).get("name");
        assertEquals(((TemplateScalarModel) nameMethod.exec(new ArrayList())).getAsString(), "A");
        
        assertSame(e, enums.get(E.class.getName()));
        
        bw.clearClassIntrospecitonCache();
        TemplateHashModel eAfterClean = (TemplateHashModel) enums.get(E.class.getName());
        assertNotSame(e, eAfterClean);
        assertSame(eAfterClean, enums.get(E.class.getName()));
        assertNotNull(eAfterClean.get("A"));
        assertNotNull(eAfterClean.get("B"));
        assertNull(eAfterClean.get("C"));
    }
    
    public static enum E {
        A, B;

        @Override
        public String toString() {
            return "ts:" + super.toString();
        }
        
    }

}
