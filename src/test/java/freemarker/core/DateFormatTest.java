package freemarker.core;

import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
public class DateFormatTest extends TemplateTest {
    
    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setLocale(Locale.US);
        
        cfg.setCustomDateFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE));
    }

    @Test
    public void testWrongFormatStrings() throws Exception {
        getConfiguration().setDateTimeFormat("x1");
        assertErrorContains("${.now}", "\"x1\"", "'x'");
        assertErrorContains("${.now?string}", "\"x1\"", "'x'");
        getConfiguration().setDateTimeFormat("short");
        assertErrorContains("${.now?string('x2')}", "\"x2\"", "'x'");
    }
    
    @Test
    public void testNullInNumberModel() throws Exception {
        addToDataModel("n", new MutableTemplateDateModel());
        assertErrorContains("${n}", "nothing inside it");
        assertErrorContains("${n?string}", "nothing inside it");
    }
    
    private static class MutableTemplateDateModel implements TemplateDateModel {
        
        private Date date;

        public void setDate(Date date) {
            this.date = date;
        }

        public Date getAsDate() throws TemplateModelException {
            return date;
        }

        public int getDateType() {
            return DATETIME;
        }
        
    }
    
}
