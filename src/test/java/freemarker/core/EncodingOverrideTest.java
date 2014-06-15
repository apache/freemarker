package freemarker.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class EncodingOverrideTest {

    @Test
    public void testExactMarchingCharset() throws Exception {
        Template t = createConfig("UTF-8").getTemplate("encodingOverride-UTF-8.ftl");
        assertEquals("UTF-8", t.getEncoding());
        checkTempateOutput(t);
    }

    @Test
    public void testCaseDiffCharset() throws Exception {
        Template t = createConfig("utf-8").getTemplate("encodingOverride-UTF-8.ftl");
        assertEquals("utf-8", t.getEncoding());
        checkTempateOutput(t);
    }

    @Test
    public void testReallyDiffCharset() throws Exception {
        Template t = createConfig("utf-8").getTemplate("encodingOverride-ISO-8859-1.ftl");
        assertEquals("ISO-8859-1", t.getEncoding());
        checkTempateOutput(t);
    }

    private void checkTempateOutput(Template t) throws TemplateException, IOException {
        StringWriter out = new StringWriter(); 
        t.process(Collections.emptyMap(), out);
        assertEquals("Béka", out.toString());
    }
    
    private Configuration createConfig(String charset) {
       Configuration cfg = new Configuration(new Version(2, 3, 21));
       cfg.setClassForTemplateLoading(EncodingOverrideTest.class, "");
       cfg.setDefaultEncoding(charset);
       return cfg;
    }

}
