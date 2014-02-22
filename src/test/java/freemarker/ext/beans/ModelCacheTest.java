package freemarker.ext.beans;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.TemplateModel;
import freemarker.template.Version;

@RunWith(JUnit4.class)
public class ModelCacheTest {
    
    @Test
    public void modelCacheOff() throws Exception {
        BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 21));
        assertFalse(bw.getUseCache());  // default is off
        
        String s = "foo";
        assertNotSame(bw.wrap(s), bw.wrap(s));
        
        C c = new C();
        assertNotSame(bw.wrap(c), bw.wrap(c));
    }
    
    @Test
    public void modelCacheOn() throws Exception {
        BeansWrapper bw = new BeansWrapper(new Version(2, 3, 21));
        bw.setUseCache(true);
        assertTrue(bw.getUseCache());
        
        String s = "foo";
        assertSame(bw.wrap(s), bw.wrap(s));
        
        C c = new C();
        TemplateModel wrappedC = bw.wrap(c);
        assertSame(wrappedC, bw.wrap(c));
        
        bw.clearClassIntrospecitonCache();
        assertNotSame(wrappedC, bw.wrap(c));
        assertSame(bw.wrap(c), bw.wrap(c));
    }

    static public class C { }
    
}
