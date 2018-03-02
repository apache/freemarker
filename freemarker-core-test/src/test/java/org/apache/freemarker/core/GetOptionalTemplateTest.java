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
        addTemplate("inc.ftl", "<#assign x = (x!0) + 1>inc ${x}");
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('inc.ftl')>"
                + "Exists: ${t.exists?c}; "
                + "Include: <@t.include />, <@t.include />; "
                + "Import: <#assign ns1 = t.import()><#assign ns2 = t.import()>${ns1.x}, ${ns2.x}; "
                + "Aliased: <#assign x = 9 in ns1>${ns1.x}, ${ns2.x}, <#import 'inc.ftl' as ns3>${ns3.x}",
                "Exists: true; "
                + "Include: inc 1, inc 2; "
                + "Import: 1, 1; "
                + "Aliased: 9, 9, 9"
                );
    }

    @Test
    public void testBasicsWhenTemplateIsMissing() throws Exception {
        assertOutput(""
                + "<#assign t = .getOptionalTemplate('missing.ftl')>"
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
        addTemplate("lib/inc.ftl", "included");
        
        addTemplate("test1.ftl", "<@.getOptionalTemplate('lib/inc.ftl').include />");
        assertOutputForNamed("test1.ftl", "included");
        
        addTemplate("lib/test2.ftl", "<@.getOptionalTemplate('/lib/inc.ftl').include />");
        assertOutputForNamed("lib/test2.ftl", "included");
        
        addTemplate("lib/test3.ftl", "<@.getOptionalTemplate('inc.ftl').include />");
        assertOutputForNamed("lib/test3.ftl", "included");
        
        addTemplate("sub/test4.ftl", "<@.getOptionalTemplate('../lib/inc.ftl').include />");
        assertOutputForNamed("sub/test4.ftl", "included");
    }

    @Test
    public void testUseCase1() throws Exception {
        addTemplate("lib/inc.ftl", "included");
        assertOutput(""
                + "<#macro test templateName{positional}>"
                + "<#local t = .getOptionalTemplate(templateName)>"
                + "<#if t.exists>"
                + "before <@t.include /> after"
                + "<#else>"
                + "missing"
                + "</#if>"
                + "</#macro>"
                + "<@test 'lib/inc.ftl' />; "
                + "<@test 'inc.ftl' />",
                "before included after; missing");
    }

    @Test
    public void testUseCase2() throws Exception {
        addTemplate("found.ftl", "found");
        assertOutput(""
                + "<@("
                + ".getOptionalTemplate('missing1.ftl').include!"
                + ".getOptionalTemplate('missing2.ftl').include!"
                + ".getOptionalTemplate('found.ftl').include!"
                + ".getOptionalTemplate('missing3.ftl').include"
                + ") />",
                "found");
        assertOutput(""
                + "<#macro fallback>fallback</#macro>"
                + "<@("
                + ".getOptionalTemplate('missing1.ftl').include!"
                + ".getOptionalTemplate('missing2.ftl').include!"
                + "fallback"
                + ") />",
                "fallback");
    }
    
    @Test
    public void testWrongArguments() throws Exception {
        assertErrorContains("<#assign t = .getOptionalTemplate()>", "argument");
        assertErrorContains("<#assign t = .getOptionalTemplate('1', '2', '3')>", "arguments", "3");
        assertErrorContains("<#assign t = .getOptionalTemplate(1)>", "1st argument", "string", "number");
    }
    
}
