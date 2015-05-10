package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class StringLiteralInterpolationTest extends TemplateTest {

    @Test
    public void basics() throws IOException, TemplateException {
        assertOutput("<#assign x = 1>${'${x}'}", "1");
        assertOutput("<#assign x = 1>${'${x} ${x}'}", "1 1");
        assertOutput("<#assign x = 1>${'$\\{x}'}", "${x}");
        assertOutput("<#assign x = 1>${'$\\{x} $\\{x}'}", "${x} ${x}");
    }

    /**
     * Broken behavior for backward compatibility.
     */
    @Test
    public void legacyBug() throws IOException, TemplateException {
        assertOutput("<#assign x = 1>${'$\\{x} ${x}'}", "1 1");
        assertOutput("<#assign x = 1>${'${x} $\\{x}'}", "1 1");
    }

    @Test
    public void escaping() throws IOException, TemplateException {
        assertOutput("<#escape x as x?html><#assign x = '&'>${x} ${'${x}'}</#escape> ${x}", "&amp; &amp; &");
    }
    
}
