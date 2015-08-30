package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
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
        assertOutput(
                "<#setting numberFormat='@hex'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}"
                + "<#setting numberFormat='@loc'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}",
                "11 b 12 c d"
                + "11 11_en_US 12 12_en_US 13_en_US");
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

    /**
     * ?string formats lazily (at least in 2.3.x), so it must make a snapshot of the format inputs when it's called.
     */
    @Test
    public void testStringBIDoesSnapshot() throws Exception {
        // TemplateNumberModel-s shouldn't change, but we have to keep BC when that still happens.
        final MutableTemplateNumberModel nm = new MutableTemplateNumberModel();
        nm.setNumber(123);
        addToDataModel("n", nm);
        addToDataModel("incN", new TemplateDirectiveModel() {
            
            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                nm.setNumber(nm.getAsNumber().intValue() + 1);
            }
        });
        assertOutput(
                "<#assign s1 = n?string>"
                + "<#setting numberFormat='@loc'>"
                + "<#assign s2 = n?string>"
                + "<#setting numberFormat='@hex'>"
                + "<#assign s3 = n?string>"
                + "${s1} ${s2} ${s3}",
                "123 123_en_US 7b");
        assertOutput(
                "<#assign s1 = n?string>"
                + "<@incN />"
                + "<#assign s2 = n?string>"
                + "${s1} ${s2}",
                "123 124");
    }
    
    private static class MutableTemplateNumberModel implements TemplateNumberModel {
        
        private Number number;

        public void setNumber(Number number) {
            this.number = number;
        }

        public Number getAsNumber() throws TemplateModelException {
            return number;
        }
        
    }
    
}
