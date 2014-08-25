package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;

public class TemplateLanguageVersionTest {

    @Test
    public void testDefaultVersion() throws IOException {
        testDefaultWithVersion(_TemplateAPI.VERSION_2_3_0, _TemplateAPI.VERSION_2_3_0);
        testDefaultWithVersion(new Version(2, 3, 18), _TemplateAPI.VERSION_2_3_0);
        testDefaultWithVersion(_TemplateAPI.VERSION_2_3_19, _TemplateAPI.VERSION_2_3_19);
        testDefaultWithVersion(_TemplateAPI.VERSION_2_3_20, _TemplateAPI.VERSION_2_3_20);
        testDefaultWithVersion(_TemplateAPI.VERSION_2_3_21, _TemplateAPI.VERSION_2_3_21);
        try {
            testDefaultWithVersion(new Version(2, 3, 22), _TemplateAPI.VERSION_2_3_21);
            fail("Maybe you need to update this test for the new FreeMarker version");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("version"));
        }
    }

    private void testDefaultWithVersion(Version cfgV, Version ftlV) throws IOException {
        Configuration cfg = createConfiguration(cfgV);
        Template t = new Template("adhoc", "foo", cfg);
        assertEquals(ftlV, t.getTemplateLanguageVersion());
        t = cfg.getTemplate("test.ftl");
        assertEquals(ftlV, t.getTemplateLanguageVersion());
    }
    
    private Configuration createConfiguration(Version version) {
        Configuration cfg = new Configuration(version);
        StringTemplateLoader stl = new StringTemplateLoader();
        stl.putTemplate("test.ftl", "foo");
        cfg.setTemplateLoader(stl);
        return cfg;
    }
    
}
