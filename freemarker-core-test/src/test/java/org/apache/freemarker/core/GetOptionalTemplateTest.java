package org.apache.freemarker.core;

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class GetOptionalTemplateTest extends TemplateTest {

    private ByteArrayTemplateLoader byteArrayTemplateLoader = new ByteArrayTemplateLoader();
    
    @Override
    protected Configuration createDefaultConfiguration() throws Exception {
        return new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(
                        new MultiTemplateLoader(new TemplateLoader[] {
                                new StringTemplateLoader(), byteArrayTemplateLoader
                        })).build();
    }

    @Test
    public void testBasicsWhenTemplateExists() throws Exception {
        addTemplate("inc.f3ah", "<#assign x = (x!0) + 1>inc ${x}");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.f3ah')>"
                + "Exists: ${t.exists?c}; "
                + "Include: <@t.include />, <@t.include />; "
                + "Import: <#assign ns1 = t.import()><#assign ns2 = t.import()>${ns1.x}, ${ns2.x}; "
                + "Aliased: <#assign x = 9 in ns1>${ns1.x}, ${ns2.x}, <#import 'inc.f3ah' as ns3>${ns3.x}",
                "Exists: true; "
                + "Include: inc 1, inc 2; "
                + "Import: 1, 1; "
                + "Aliased: 9, 9, 9"
                );
    }

    @Test
    public void testBasicsWhenTemplateIsMissing() throws Exception {
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('missing.f3ah')>"
                + "Exists: ${t.exists?c}; "
                + "Include: ${t.include???c}; "
                + "Import: ${t.import???c}",
                "Exists: false; "
                + "Include: false; "
                + "Import: false"
                );
    }

    @Test
    public void testRelativeAndAbsolutePath() throws Exception {
        addTemplate("lib/inc.f3ah", "included");
        
        addTemplate("test1.f3ah", "<@.getOptionalTemplate('lib/inc.f3ah').include />");
        assertOutputForNamed("test1.f3ah", "included");
        
        addTemplate("lib/test2.f3ah", "<@.getOptionalTemplate('/lib/inc.f3ah').include />");
        assertOutputForNamed("lib/test2.f3ah", "included");
        
        addTemplate("lib/test3.f3ah", "<@.getOptionalTemplate('inc.f3ah').include />");
        assertOutputForNamed("lib/test3.f3ah", "included");
        
        addTemplate("sub/test4.f3ah", "<@.getOptionalTemplate('../lib/inc.f3ah').include />");
        assertOutputForNamed("sub/test4.f3ah", "included");
    }

    @Test
    public void testUseCase1() throws Exception {
        addTemplate("lib/inc.f3ah", "included");
        assertOutput(""
                + "<#macro test templateName{positional}>"
                + "<#local t = .getOptionalTemplate(templateName)>"
                + "<#if t.exists>"
                + "before <@t.include /> after"
                + "<#else>"
                + "missing"
                + "</#if>"
                + "</#macro>"
                + "<@test 'lib/inc.f3ah' />; "
                + "<@test 'inc.f3ah' />",
                "before included after; missing");
    }

    @Test
    public void testUseCase2() throws Exception {
        addTemplate("found.f3ah", "found");
        assertOutput(""
                + "<@("
                + ".getOptionalTemplate('missing1.f3ah').include!"
                + ".getOptionalTemplate('missing2.f3ah').include!"
                + ".getOptionalTemplate('found.f3ah').include!"
                + ".getOptionalTemplate('missing3.f3ah').include"
                + ") />",
                "found");
        assertOutput(""
                + "<#macro fallback>fallback</#macro>"
                + "<@("
                + ".getOptionalTemplate('missing1.f3ah').include!"
                + ".getOptionalTemplate('missing2.f3ah').include!"
                + "fallback"
                + ") />",
                "fallback");
    }
    
    @Test
    public void testWrongArguments() throws Exception {
        assertErrorContains("<#assign t = .getOptionalTemplate()>", "argument");
        assertErrorContains("<#assign t = .getOptionalTemplate('1', '2', '3')>", "1", "arguments", "more");
        assertErrorContains("<#assign t = .getOptionalTemplate(1)>", "1st argument", "string", "number");
    }
    
}
