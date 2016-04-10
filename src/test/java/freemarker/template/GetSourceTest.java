package freemarker.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GetSourceTest {

    
    @Test
    public void testGetSource() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        
        {
            // Note: Default tab size is 8.
            Template t = new Template(null, "a\n\tb\nc", cfg);
            // A historical quirk we keep for B.C.: it repaces tabs with spaces.
            assertEquals("a\n        b\nc", t.getSource(1, 1, 1, 3));
        }
        
        {
            cfg.setTabSize(4);
            Template t = new Template(null, "a\n\tb\nc", cfg);
            assertEquals("a\n    b\nc", t.getSource(1, 1, 1, 3));
        }
        
        {
            cfg.setTabSize(1);
            Template t = new Template(null, "a\n\tb\nc", cfg);
            // If tab size is 1, it behaves as it always should have: it keeps the tab.
            assertEquals("a\n\tb\nc", t.getSource(1, 1, 1, 3));
        }
    }
    
}
