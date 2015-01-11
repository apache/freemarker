package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupStrategy;

public class TemplateLookupStrategyTest {

    @Test
    public void testSetSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        assertSame(TemplateLookupStrategy.DEFAULT, cfg.getTemplateLookupStrategy());

        cfg.setSetting(Configuration.TEMPLATE_LOOKUP_STRATEGY_KEY, MyTemplateLookupStrategy.class.getName() + "()");
        assertTrue(cfg.getTemplateLookupStrategy() instanceof MyTemplateLookupStrategy);
        
        cfg.setSetting(Configuration.TEMPLATE_LOOKUP_STRATEGY_KEY, "dEfault");
        assertSame(TemplateLookupStrategy.DEFAULT, cfg.getTemplateLookupStrategy());
    }
    
    @Test
    public void testCustomStrategy() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        MockTemplateLoader tl = new MockTemplateLoader();
        tl.putTemplate("test.ftl", "");
        tl.putTemplate("aa/test.ftl", "");
        cfg.setTemplateLoader(tl);
        
        cfg.setTemplateLookupStrategy(MyTemplateLookupStrategy.INSTANCE);
        
        try {
            cfg.getTemplate("missing.ftl", new Locale("aa", "BB", "CC_DD"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(ImmutableList.of("aa/missing.ftl", "missing.ftl"), tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("aa", "BB", "CC_DD")).getName());
        assertEquals(ImmutableList.of("aa/test.ftl"), tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
    }
    
    @Test
    public void testDefaultStrategy() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        MockTemplateLoader tl = new MockTemplateLoader();
        tl.putTemplate("test.ftl", "");
        tl.putTemplate("test_aa.ftl", "");
        tl.putTemplate("test_aa_BB.ftl", "");
        tl.putTemplate("test_aa_BB_CC.ftl", "");
        tl.putTemplate("test_aa_BB_CC_DD.ftl", "");
        
        cfg.setTemplateLoader(tl);
        
        try {
            cfg.getTemplate("missing.ftl", new Locale("aa", "BB", "CC_DD"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of(
                            "missing_aa_BB_CC_DD.ftl",
                            "missing_aa_BB_CC.ftl",
                            "missing_aa_BB.ftl",
                            "missing_aa.ftl",
                            "missing.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
        
        cfg.setLocale(new Locale("xx"));
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing_xx.ftl", "missing.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
        
        cfg.setLocalizedLookup(false);
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
        cfg.setLocalizedLookup(true);
        
        try {
            cfg.getTemplate("_a_b_.ftl", new Locale("xx", "yy"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("_a_b_.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("_a_b__xx_YY.ftl", "_a_b__xx.ftl", "_a_b_.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("aa", "BB", "CC_DD")).getName());
        assertEquals(ImmutableList.of("test_aa_BB_CC_DD.ftl"), tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("aa", "BB", "CC_XX")).getName());
        assertEquals(ImmutableList.of("test_aa_BB_CC_XX.ftl", "test_aa_BB_CC.ftl"), tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("aa", "BB", "XX_XX")).getName());
        assertEquals(
                ImmutableList.of("test_aa_BB_XX_XX.ftl", "test_aa_BB_XX.ftl", "test_aa_BB.ftl"),
                tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("aa", "XX", "XX_XX")).getName());
        assertEquals(
                ImmutableList.of("test_aa_XX_XX_XX.ftl", "test_aa_XX_XX.ftl", "test_aa_XX.ftl", "test_aa.ftl"),
                tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("xx", "XX", "XX_XX")).getName());
        assertEquals(
                ImmutableList.of(
                        "test_xx_XX_XX_XX.ftl", "test_xx_XX_XX.ftl", "test_xx_XX.ftl", "test_xx.ftl", "test.ftl"),
                tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
        
        assertEquals("test.ftl", cfg.getTemplate("test.ftl", new Locale("xx", "BB", "CC_DD")).getName());
        assertEquals(
                ImmutableList.of(
                    "test_xx_BB_CC_DD.ftl", "test_xx_BB_CC.ftl", "test_xx_BB.ftl", "test_xx.ftl", "test.ftl"),
                tl.templatesTried);
        tl.templatesTried.clear();
        cfg.clearTemplateCache();
    }
    
    private static class MockTemplateLoader extends StringTemplateLoader {
        
        private final List<String> templatesTried = new ArrayList<String>();
        
        @Override
        public Object findTemplateSource(String name) {
            templatesTried.add(name);
            return super.findTemplateSource(name);
        }

        public List<String> getTemplatesTried() {
            return templatesTried;
        }
        
    }
    
    public static class MyTemplateLookupStrategy implements TemplateLookupStrategy {
        
        public static final MyTemplateLookupStrategy INSTANCE = new MyTemplateLookupStrategy();
        
        private MyTemplateLookupStrategy() { }

        public Object findTemplateSource(TemplateLookupContext ctx) throws IOException {
            String lang = ctx.getTemplateLocale().getLanguage().toLowerCase();
            Object ts = ctx.findTemplateSourceWithAcquisitionStrategy(lang + "/" + ctx.getTemplateName());
            if (ts != null) {
                return ts;
            }
            
            return ctx.findTemplateSourceWithAcquisitionStrategy(ctx.getTemplateName());
        }
        
    }

}
