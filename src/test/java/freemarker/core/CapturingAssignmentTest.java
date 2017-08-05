package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class CapturingAssignmentTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setTemplateLoader(new StringTemplateLoader());
        return cfg;
    }

    @Test
    public void testAssign() throws IOException, TemplateException {
        assertOutput("<#assign x></#assign>[${x}]", "[]");
        assertOutput("<#assign x><p>${1 + 1}</#assign>${x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'><#assign x><p>${1 + 1}</#assign>${x + '&'}", "<p>2&amp;");
    }

    @Test
    public void testAssignNs() throws IOException, TemplateException {
        addTemplate("lib.ftl", "");
        assertOutput("<#import 'lib.ftl' as lib>"
                + "<#assign x in lib></#assign>[${lib.x}]", "[]");
        assertOutput("<#import 'lib.ftl' as lib>"
                + "<#assign x in lib><p>${1 + 1}</#assign>${lib.x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'>"
                + "<#import 'lib.ftl' as lib>"
                + "<#assign x in lib><p>${1 + 1}</#assign>${lib.x + '&'}", "<p>2&amp;");
    }
    
    @Test
    public void testGlobal() throws IOException, TemplateException {
        assertOutput("<#global x></#global>[${.globals.x}]", "[]");
        assertOutput("<#global x><p>${1 + 1}</#global>${.globals.x + '&'}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'><#global x><p>${1 + 1}</#global>${.globals.x + '&'}", "<p>2&amp;");
    }

    @Test
    public void testLocal() throws IOException, TemplateException {
        assertOutput("<#macro m><#local x></#local>[${x}]</#macro><@m/>${x!}", "[]");
        assertOutput("<#macro m><#local x><p>${1 + 1}</#local>${x + '&'}</#macro><@m/>${x!}", "<p>2&");
        assertOutput("<#ftl outputFormat='HTML'>"
                + "<#macro m><#local x><p>${1 + 1}</#local>${x + '&'}</#macro><@m/>${x!}", "<p>2&amp;");
    }
    
}
