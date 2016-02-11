package freemarker.cache;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class MultiTemplateLoaderTest {

    @Test
    public void testBasics() throws IOException {
        StringTemplateLoader stl1 = new StringTemplateLoader();
        stl1.putTemplate("1.ftl", "1");
        stl1.putTemplate("both.ftl", "both 1");

        StringTemplateLoader stl2 = new StringTemplateLoader();
        stl2.putTemplate("2.ftl", "2");
        stl2.putTemplate("both.ftl", "both 2");
        
        MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] { stl1, stl2 });
        assertEquals("1", getTemplate(mtl, "1.ftl"));
        assertEquals("2", getTemplate(mtl, "2.ftl"));
        assertEquals("both 1", getTemplate(mtl, "both.ftl"));
        assertNull(getTemplate(mtl, "neither.ftl"));
    }

    @Test
    public void testSticky() throws IOException {
        testStickiness(true);
    }

    @Test
    public void testNonSticky() throws IOException {
        testStickiness(false);
    }
    
    private void testStickiness(boolean sticky) throws IOException {
        StringTemplateLoader stl1 = new StringTemplateLoader();
        stl1.putTemplate("both.ftl", "both 1");
        
        StringTemplateLoader stl2 = new StringTemplateLoader();
        stl2.putTemplate("both.ftl", "both 2");

        MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[] { stl1, stl2 });
        mtl.setSticky(sticky);
        
        assertEquals("both 1", getTemplate(mtl, "both.ftl"));
        assertTrue(stl1.removeTemplate("both.ftl"));
        assertEquals("both 2", getTemplate(mtl, "both.ftl"));
        stl1.putTemplate("both.ftl", "both 1");
        assertEquals(sticky ? "both 2" : "both 1", getTemplate(mtl, "both.ftl"));
        assertTrue(stl2.removeTemplate("both.ftl"));
        assertEquals("both 1", getTemplate(mtl, "both.ftl"));
    }
    
    private String getTemplate(TemplateLoader tl, String name) throws IOException {
        Object tSrc = tl.findTemplateSource(name);
        if (tSrc == null) {
            return null;
        }
        
        return IOUtils.toString(tl.getReader(tSrc, "UTF-8"));
    }
    
}
