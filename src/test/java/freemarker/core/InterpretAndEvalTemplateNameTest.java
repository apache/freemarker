package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

/**
 * Test template names returned by special variables and relative path resolution in {@code ?interpret}-ed and
 * {@code ?eval}-ed parts.  
 */
public class InterpretAndEvalTemplateNameTest extends TemplateTest {
    
    @Test
    public void testInterpret() throws IOException, TemplateException {
        for (String getTemplateNames : new String[] {
                "c=${.current_template_name}, m=${.main_template_name}",
                "c=${\".current_template_name\"?eval}, m=${\".main_template_name\"?eval}"
                }) {
            StringTemplateLoader tl = new StringTemplateLoader();
            tl.putTemplate(
                    "main.ftl",
                    getTemplateNames + " "
                    + "{<#include 'sub/t.ftl'>}");
            tl.putTemplate(
                    "sub/t.ftl",
                    getTemplateNames + " "
                    + "i{<@r'" + getTemplateNames + " {<#include \"a.ftl\">'?interpret />}} "
                    + "i{<@[r'" + getTemplateNames + " {<#include \"a.ftl\">','named_interpreted']?interpret />}}");
            tl.putTemplate("sub/a.ftl", "In sub/a.ftl, " + getTemplateNames);
            tl.putTemplate("a.ftl", "In a.ftl");
            
            getConfiguration().setTemplateLoader(tl);
            
            assertOutputForNamed("main.ftl",
                    "c=main.ftl, m=main.ftl "
                    + "{"
                        + "c=sub/t.ftl, m=main.ftl "
                        + "i{c=sub/t.ftl->anonymous_interpreted, m=main.ftl {In sub/a.ftl, c=sub/a.ftl, m=main.ftl}} "
                        + "i{c=sub/t.ftl->named_interpreted, m=main.ftl {In sub/a.ftl, c=sub/a.ftl, m=main.ftl}}"
                    + "}");
            
            assertOutputForNamed("sub/t.ftl",
                    "c=sub/t.ftl, m=sub/t.ftl "
                    + "i{c=sub/t.ftl->anonymous_interpreted, m=sub/t.ftl {In sub/a.ftl, c=sub/a.ftl, m=sub/t.ftl}} "
                    + "i{c=sub/t.ftl->named_interpreted, m=sub/t.ftl {In sub/a.ftl, c=sub/a.ftl, m=sub/t.ftl}}");
        }
    }
    
}
