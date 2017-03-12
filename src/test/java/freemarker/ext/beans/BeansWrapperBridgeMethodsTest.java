package freemarker.ext.beans;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class BeansWrapperBridgeMethodsTest {
    
    @Test
    public void testWithoutDefaultMethod() throws TemplateModelException {
        test(BridgeMethodsBean.class);
    }

    @Test
    public void testWithDefaultMethod() throws TemplateModelException {
        test(BridgeMethodsWithDefaultMethodBean.class);
    }

    @Test
    public void testWithDefaultMethod2() throws TemplateModelException {
        test(BridgeMethodsWithDefaultMethodBean2.class);
    }

    private void test(Class<?> pClass) throws TemplateModelException {
        BeansWrapper ow = new BeansWrapperBuilder(Configuration.VERSION_2_3_26).build();
        TemplateHashModel wrapped;
        try {
            wrapped = (TemplateHashModel) ow.wrap(pClass.newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        TemplateMethodModelEx m1 = (TemplateMethodModelEx) wrapped.get("m1");
        assertEquals(BridgeMethodsBean.M1_RETURN_VALUE, "" + m1.exec(Collections.emptyList()));
        
        TemplateMethodModelEx m2 = (TemplateMethodModelEx) wrapped.get("m2");
        assertNull(m2.exec(Collections.emptyList()));
    }
    
}
