package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class NumberFormatTest extends TemplateTest {
    
    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setLocale(Locale.US);
        
        cfg.setCustomNumberFormats(ImmutableMap.of(
                "hex", HexTemplateNumberFormatFactory.INSTANCE,
                "loc", LocaleSensitiveTemplateNumberFormatFactory.INSTANCE));
    }

    @Test
    public void testUnknownNumberFormat() throws Exception {
        {
            getConfiguration().setNumberFormat("@noSuchFormat");
            Throwable exc = assertErrorContains("${1}", "\"@noSuchFormat\"");
            assertThat(exc.getCause().getMessage(), containsString("\"noSuchFormat\""));
        }

        {
            getConfiguration().setNumberFormat("number");
            Throwable exc = assertErrorContains("${1?string('@noSuchFormat2')}", "\"@noSuchFormat2\"");
            assertThat(exc.getCause().getMessage(), containsString("\"noSuchFormat2\""));
        }
    }
    
    @Test
    public void testStringBI() throws Exception {
        assertOutput("${11} ${11?string.@hex} ${12} ${12?string.@hex}", "11 b 12 c");
    }

    @Test
    public void testSetting() throws Exception {
        getConfiguration().setNumberFormat("@hex");
        assertOutput("${11?string.number} ${11} ${12?string.number} ${12}", "11 b 12 c");
    }

    @Test
    public void testSetting2() throws Exception {
        assertOutput("<#setting numberFormat='@hex'>${11?string.number} ${11} ${12?string.number} ${12}", "11 b 12 c");
    }
    
    @Test
    public void testUnformattableNumber() throws Exception {
        getConfiguration().setNumberFormat("@hex");
        assertErrorContains("${1.1}", "hexadecimal int", "doesn't fit into an int");
    }

    @Test
    public void testLocaleSensitive() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@loc");
        assertOutput("${1.1}", "1.1_en_US");
        cfg.setLocale(Locale.GERMANY);
        assertOutput("${1.1}", "1.1_de_DE");
    }

    @Test
    public void testLocaleSensitive2() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@loc");
        assertOutput("${1.1} <#setting locale='de_DE'>${1.1}", "1.1_en_US 1.1_de_DE");
    }
    
}
