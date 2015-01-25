package freemarker.core;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import freemarker.test.utility.FileTestCase;

public class CanonicalFormTest extends FileTestCase {

    public CanonicalFormTest(String name) {
        super(name);
    }

    public void testIdentifierEscapingCanonicalForm() throws Exception {
        assertCanonicalFormOf("identifier-escaping.ftl");
    }

    private void assertCanonicalFormOf(String ftlFileName)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassForTemplateLoading(CanonicalFormTest.class, "");
        StringWriter sw = new StringWriter();
        cfg.getTemplate(ftlFileName).dump(sw);

        int lastDotIdx = ftlFileName.lastIndexOf('.');
        String canonicalFtlName = ftlFileName.substring(0, lastDotIdx) + "-canonical"
                + ftlFileName.substring(lastDotIdx);
        assertExpectedFileEqualsString(canonicalFtlName, sw.toString());
    }

}
