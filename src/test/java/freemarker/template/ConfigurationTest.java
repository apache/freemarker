package freemarker.template;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.utility.NullWriter;

public class ConfigurationTest extends TestCase{

    public ConfigurationTest(String name) {
        super(name);
    }
    
    public void testIncompatibleImprovementsChangesDefaults() {
        Version newVersion = new Version(2, 3, 21);
        Version oldVersion = new Version(2, 3, 20);
        
        Configuration cfg = new Configuration();
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        assertEquals(cfg.getIncompatibleImprovements(), new Version(2, 3, 0));
        
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setObjectWrapper(new SimpleObjectWrapper());
        cfg.setIncompatibleImprovements(oldVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertUsesLegacyTemplateLoader(cfg);
        
        cfg.setTemplateLoader(new StringTemplateLoader());
        cfg.setIncompatibleImprovements(newVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());

        cfg.setObjectWrapper(ObjectWrapper.DEFAULT_WRAPPER);
        cfg.setIncompatibleImprovements(newVersion);
        assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());
        
        // ---
        
        cfg = new Configuration(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
    }
    
    public void testTemplateLoadingErrors() throws Exception {
        Configuration cfg = new Configuration();
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().contains("wasn't set") && e.getMessage().contains("default"));
        }
        
        cfg = new Configuration(new Version(2, 3, 21));
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().contains("wasn't set") && !e.getMessage().contains("default"));
        }
        
        cfg.setClassForTemplateLoading(this.getClass(), "nosuchpackage");
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (IOException e) {
            assertTrue(!e.getMessage().contains("wasn't set"));
        }
    }
    
    private void assertUsesLegacyObjectWrapper(Configuration cfg) {
        assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
    }

    private void assertUsesNewObjectWrapper(Configuration cfg) {
        assertEquals(
                new Version(2, 3, 21),
                ((DefaultObjectWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
    }
    
    private void assertUsesNewTemplateLoader(Configuration cfg) {
        assertNull(cfg.getTemplateLoader());
    }
    
    private void assertUsesLegacyTemplateLoader(Configuration cfg) {
        assertTrue(cfg.getTemplateLoader() instanceof FileTemplateLoader);
    }
    
    public void testVersion() {
        Version v = Configuration.getVersion();
        assertTrue(v.intValue() > 2003020);
        assertNotNull(v.getExtraInfo());
        assertSame(v.toString(), Configuration.getVersionNumber());
        
        try {
            new Configuration(new Version(999, 1, 2));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("upgrade"));
        }
        
        try {
            new Configuration(new Version(2, 2, 2));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("2.3.0"));
        }
    }
    
    public void testShowErrorTips() throws Exception {
        Configuration cfg = new Configuration();
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertTrue(e.getMessage().contains("Tip:"));
        }
        
        cfg.setShowErrorTips(false);
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertFalse(e.getMessage().contains("Tip:"));
        }
    }
    
}
