package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

public class TemplateConstructorsTest {

    private static final String READER_CONTENT = "From a reader...";
    private static final String READER_CONTENT_FORCE_UTF8 = "<#ftl encoding='utf-8'>From a reader...";
    
    @Test
    public void test() throws IOException {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        //cfg.setDefaultEncoding("ISO-8859-1");
        
        final String name = "foo/bar.ftl";
        final String sourceName = "foo/bar_de.ftl";
        final String content = "From a String...";
        final String encoding = "UTF-16LE";
        {
            Template t = new Template(name, createReader());
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, createReader(), cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, content, cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(content, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, createReader(), cfg, encoding);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertEquals("UTF-16LE", t.getEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg);
            assertEquals(name, t.getName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg, encoding);
            assertEquals(name, t.getName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertEquals("UTF-16LE", t.getEncoding());
        }
        {
            Template t = Template.getPlainTextTemplate(name, content, cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(content, t.toString());
            assertNull(t.getEncoding());
        }
        {
            try {
                new Template(name, sourceName, createReaderForceUTF8(), cfg, encoding);
                fail();
            } catch (Template.WrongEncodingException e) {
                assertTrue(e.getMessage().contains("utf-8"));
                assertTrue(e.getMessage().contains(encoding));
            }
        }
    }
    
    private final Reader createReader() {
        return new StringReader(READER_CONTENT);
    }

    private final Reader createReaderForceUTF8() {
        return new StringReader(READER_CONTENT_FORCE_UTF8);
    }
    
}
