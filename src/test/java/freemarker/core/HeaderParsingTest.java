package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class HeaderParsingTest extends TemplateOutputTest {

    private final Configuration cfgStripWS = new Configuration(new Version(2, 3, 21));
    private final Configuration cfgNoStripWS = new Configuration(new Version(2, 3, 21));
    {
        cfgNoStripWS.setWhitespaceStripping(false);
    }
    
    @Test
    public void test() throws IOException, TemplateException {
        assertOutput("<#ftl>text", "text", "text");
        assertOutput(" <#ftl> text", " text", " text");
        assertOutput("\n<#ftl>\ntext", "text", "text");
        assertOutput("\n \n\n<#ftl> \ntext", "text", "text");
        assertOutput("\n \n\n<#ftl>\n\ntext", "\ntext", "\ntext");
    }
    
    private void assertOutput(final String ftl, String expectedOutStripped, String expectedOutNonStripped)
            throws IOException, TemplateException {
        for (int i = 0; i < 4; i++) {
            String ftlPermutation = ftl;
            if ((i & 1) == 1) {
                ftlPermutation = ftlPermutation.replace("<#ftl>", "<#ftl encoding='utf-8'>");
            }
            if ((i & 2) == 2) {
                ftlPermutation = ftlPermutation.replace('<', '[').replace('>', ']');
            }
            
            assertOutput(ftlPermutation, expectedOutStripped, cfgStripWS);
            assertOutput(ftlPermutation, expectedOutNonStripped, cfgNoStripWS);
        }
    }
    
}
