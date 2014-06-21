package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class WhitespaceStrippingTest extends TemplateOutputTest {

    private final Configuration cfgStripWS = new Configuration(new Version(2, 3, 21));
    private final Configuration cfgNoStripWS = new Configuration(new Version(2, 3, 21));
    {
        cfgNoStripWS.setWhitespaceStripping(false);
    }

    @Test
    public void testBasics() throws Exception {
        assertOutput("<#assign x = 1>\n<#assign y = 2>\n${x}\n${y}", "1\n2", "\n\n1\n2");
        assertOutput(" <#assign x = 1> \n <#assign y = 2> \n${x}\n${y}", "1\n2", "  \n  \n1\n2");
    }

    @Test
    public void testFTLHeader() throws Exception {
        assertOutput("<#ftl>x", "x", "x");
        assertOutput("  <#ftl>  x", "  x", "  x");
        assertOutput("\n<#ftl>\nx", "x", "x");
        assertOutput("\n<#ftl>\t \nx", "x", "x");
        assertOutput("  \n \n  <#ftl> \n \n  x", " \n  x", " \n  x");
    }

    @Test
    public void testComment() throws Exception {
        assertOutput(" a <#-- --> b ", " a  b ", " a  b ");
        assertOutput(" a \n<#-- -->\n b ", " a \n b ", " a \n\n b ");
        // These are wrong, but needed for 2.3.0 compatibility:
        assertOutput(" a \n <#-- --> \n b ", " a \n  b ", " a \n  \n b ");
        assertOutput(" a \n\t<#-- --> \n b ", " a \n\t b ", " a \n\t \n b ");
    }
    
    private void assertOutput(String ftl, String expectedOutStripped, String expectedOutNonStripped)
            throws IOException, TemplateException {
        assertOutput(ftl, expectedOutStripped, cfgStripWS);
        assertOutput(ftl, expectedOutNonStripped, cfgNoStripWS);
    }

}
