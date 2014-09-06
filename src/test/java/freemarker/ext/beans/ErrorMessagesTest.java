package freemarker.ext.beans;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class ErrorMessagesTest {

    @Test
    public void getterMessage() throws TemplateModelException {
        BeansWrapper bw = new BeansWrapperBuilder(Configuration.VERSION_2_3_0).getResult();
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new TestBean());
        try {
            thm.get("foo");
        } catch (TemplateModelException e) {
            e.printStackTrace();
            final String msg = e.getMessage();
            assertTrue(msg.contains("\"foo\""));
            assertTrue(msg.contains("existing sub-variable"));
        }
        assertNull(thm.get("bar"));
    }
    
    public class TestBean {
        
        public String getFoo() {
            throw new RuntimeException("Dummy");
        }
        
    }
    
}
