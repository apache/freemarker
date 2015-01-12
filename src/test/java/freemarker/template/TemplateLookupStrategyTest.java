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
import freemarker.cache.TemplateLookupResult;
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
        
        {
            final Template t = cfg.getTemplate("test.ftl", new Locale("aa", "BB", "CC_DD"));
            assertEquals("test.ftl", t.getName());
            assertEquals("aa/test.ftl", t.getSourceName());
            assertEquals(ImmutableList.of("aa/test.ftl"), tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
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

        for (String templateName : new String[] { "test.ftl", "./test.ftl", "/test.ftl", "x/foo/../../test.ftl" }) {
            {
                final Template t = cfg.getTemplate("test.ftl", new Locale("aa", "BB", "CC_DD"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test_aa_BB_CC_DD.ftl", t.getSourceName());
                assertEquals(ImmutableList.of("test_aa_BB_CC_DD.ftl"), tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
            
            {
                final Template t = cfg.getTemplate(templateName, new Locale("aa", "BB", "CC_XX"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test_aa_BB_CC.ftl", t.getSourceName());
                assertEquals(ImmutableList.of("test_aa_BB_CC_XX.ftl", "test_aa_BB_CC.ftl"), tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
            
            {
                final Template t = cfg.getTemplate(templateName, new Locale("aa", "BB", "XX_XX"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test_aa_BB.ftl", t.getSourceName());
                assertEquals(
                        ImmutableList.of("test_aa_BB_XX_XX.ftl", "test_aa_BB_XX.ftl", "test_aa_BB.ftl"),
                        tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
    
            {
                final Template t = cfg.getTemplate(templateName, new Locale("aa", "XX", "XX_XX"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test_aa.ftl", t.getSourceName());
                assertEquals(
                        ImmutableList.of("test_aa_XX_XX_XX.ftl", "test_aa_XX_XX.ftl", "test_aa_XX.ftl", "test_aa.ftl"),
                        tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
            
            {
                final Template t = cfg.getTemplate(templateName, new Locale("xx", "XX", "XX_XX"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test.ftl", t.getSourceName());
                assertEquals(
                        ImmutableList.of(
                                "test_xx_XX_XX_XX.ftl", "test_xx_XX_XX.ftl", "test_xx_XX.ftl", "test_xx.ftl", "test.ftl"),
                        tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
            
            {
                final Template t = cfg.getTemplate(templateName, new Locale("xx", "BB", "CC_DD"));
                assertEquals("test.ftl", t.getName());
                assertEquals("test.ftl", t.getSourceName());
                assertEquals(
                        ImmutableList.of(
                            "test_xx_BB_CC_DD.ftl", "test_xx_BB_CC.ftl", "test_xx_BB.ftl", "test_xx.ftl", "test.ftl"),
                        tl.templatesTried);
                tl.templatesTried.clear();
                cfg.clearTemplateCache();
            }
        }
    }
    
    @Test
    public void testAcquisition() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        MockTemplateLoader tl = new MockTemplateLoader();
        tl.putTemplate("t.ftl", "");
        tl.putTemplate("sub/i.ftl", "");
        tl.putTemplate("x/sub/i.ftl", "");
        cfg.setTemplateLoader(tl);

        {
            final Template t = cfg.getTemplate("/./moo/../x/y/*/sub/i.ftl", new Locale("xx"));
            assertEquals("x/y/*/sub/i.ftl", t.getName());
            assertEquals("x/sub/i.ftl", t.getSourceName());
            assertEquals(
                    ImmutableList.of(
                        "x/y/sub/i_xx.ftl", "x/sub/i_xx.ftl", "sub/i_xx.ftl",
                        "x/y/sub/i.ftl", "x/sub/i.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }

        {
            final Template t = cfg.getTemplate("a/b/*/./sub/i.ftl", new Locale("xx"));
            assertEquals("a/b/*/sub/i.ftl", t.getName());
            assertEquals("sub/i.ftl", t.getSourceName());
            assertEquals(
                    ImmutableList.of(
                        "a/b/sub/i_xx.ftl", "a/sub/i_xx.ftl", "sub/i_xx.ftl",
                        "a/b/sub/i.ftl", "a/sub/i.ftl", "sub/i.ftl"),
                    tl.templatesTried);
            tl.templatesTried.clear();
            cfg.clearTemplateCache();
        }
    }
    
    private static class MockTemplateLoader extends StringTemplateLoader {
        
        private final List<String> templatesTried = new ArrayList<String>();
        
        @Override
        public Object findTemplateSource(String name) {
            templatesTried.add(name);
            return super.findTemplateSource(name);
        }
        
    }
    
    public static class MyTemplateLookupStrategy implements TemplateLookupStrategy {
        
        public static final MyTemplateLookupStrategy INSTANCE = new MyTemplateLookupStrategy();
        
        private MyTemplateLookupStrategy() { }

        public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
            String lang = ctx.getTemplateLocale().getLanguage().toLowerCase();
            TemplateLookupResult lookupResult = ctx.lookupWithAcquisitionStrategy(lang + "/" + ctx.getTemplateName());
            if (lookupResult.isPositive()) {
                return lookupResult;
            }
            
            return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
        }
        
    }

}
