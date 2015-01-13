package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

public class TemplateConstructorsTest {

    private static final String READER_CONTENT = "From a reader...";

    @Test
    public void test() throws IOException {
        final String name = "foo/bar.ftl";
        final String sourceName = "foo/bar_de.ftl";
        final String sourceCode = "From a String...";
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
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
            Template t = new Template(name, sourceCode, cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(sourceCode, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, createReader(), cfg, encoding);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertEquals("UTF-16LE", t.getEncoding());
        }
    }
    
    private final Reader createReader() {
        return new StringReader(READER_CONTENT);
    }

}
