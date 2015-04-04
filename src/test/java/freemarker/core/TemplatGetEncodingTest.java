package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.StrongCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

public class TemplatGetEncodingTest {

    @Test
    public void test() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        {
            cfg.setDefaultEncoding("ISO-8859-2");
            StringTemplateLoader tl = new StringTemplateLoader();
            tl.putTemplate("t", "test");
            cfg.setTemplateLoader(tl);
            cfg.setCacheStorage(new StrongCacheStorage());
        }

        {
            Template tDefEnc = cfg.getTemplate("t");
            assertEquals("ISO-8859-2", tDefEnc.getEncoding());
            assertSame(tDefEnc, cfg.getTemplate("t"));

            Template tUTF8 = cfg.getTemplate("t", "UTF-8");
            assertEquals("UTF-8", tUTF8.getEncoding());
            assertSame(tUTF8, cfg.getTemplate("t", "UTF-8"));
            assertNotSame(tDefEnc, tUTF8);
        }

        {
            Template nonStoredT = new Template(null, "test", cfg);
            assertNull(nonStoredT.getEncoding());
        }

        {
            Template nonStoredT = Template.getPlainTextTemplate(null, "<#test>", cfg);
            assertNull(nonStoredT.getEncoding());
        }
    }

}
