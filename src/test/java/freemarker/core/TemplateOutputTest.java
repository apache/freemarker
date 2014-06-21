package freemarker.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Ignore;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Ignore
public abstract class TemplateOutputTest {

    protected void assertOutput(String ftl, String expectedOut, Configuration cfg) throws IOException, TemplateException {
        Template t = new Template(null, ftl, cfg);
        StringWriter out = new StringWriter();
        t.process(Collections.emptyMap(), out);
        assertEquals(expectedOut, out.toString());
    }
    
}
