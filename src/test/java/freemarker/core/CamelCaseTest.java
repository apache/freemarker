package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;


public class CamelCaseTest extends TemplateTest {

    @Test
    public void camelCaseDirectivesNonStrict() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setStrictSyntaxMode(false);
        setConfiguration(cfg);
        assertOutput(
                "<list 1..4 as x><if x == 1>one <elseIf x == 2>two <elseif x == 3>three <else>other </if></list>",
                "one <elseIf x == 2>two other three other ");
        assertOutput(
                "<escape x as x?upper_case>${'a'}<noEscape>${'b'}</noEscape></escape> "
                + "<escape x as x?upper_case>${'a'}<noescape>${'b'}</noescape></escape>",
                "A<noEscape>B</noEscape> Ab");
        assertOutput(
                "<noParse>${1}</noParse> <noparse>${1}</noparse>",
                "<noParse>1</noParse> ${1}");
        assertOutput(
                "<forEach x in 1..3>${x!'?'}</forEach> <foreach x in 1..3>${x}</foreach>",
                "<forEach x in 1..3>?</forEach> 123");
        
        camelCaseDirectives();
    }
    
    @Test
    public void camelCaseDirectives() throws IOException, TemplateException {
        //camelCaseDirectives(false);
        getConfiguration().setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        camelCaseDirectives(true);
    }

    private void camelCaseDirectives(boolean squared) throws IOException, TemplateException {
        assertOutput(
                squared("<#list 1..4 as x><#if x == 1>one <#elseIf x == 2>two <#elseif x == 3>three "
                        + "<#else>more</#if></#list>", squared),
                "one two three more");
        assertOutput(
                squared("<#escape x as x?upper_case>${'a'}<#noEscape>${'b'}</#noEscape></#escape> "
                        + "<#escape x as x?upper_case>${'a'}<#noescape>${'b'}</#noescape></#escape>", squared),
                "Ab Ab");
        assertOutput(
                squared("<#noParse></#noparse></#noParse>", squared),
                squared("</#noparse>", squared));
        assertOutput(
                squared("<#noparse></#noParse></#noparse>", squared),
                squared("</#noParse>", squared));
        assertOutput(
                squared("<#forEach x in 1..3>${x}</#forEach> <#foreach x in 1..3>${x}</#foreach>", squared),
                "123 123");
    }
    
    private String squared(String ftl, boolean squared) {
        return squared ? ftl.replace('<', '[').replace('>', ']') : ftl;
    }

    @Test
    public void nonMatchingEndTag() {
        assertErrorContains(
                "<#escape x as x?upper_case>${'a'}<#noEscape>${'b'}</#noescape></#escape>",
                "noEscape", "noescape", "camel");
        assertErrorContains(
                "<#escape x as x?upper_case>${'a'}<#noescape>${'b'}</#noEscape></#escape>",
                "noEscape", "noescape", "camel");
        assertErrorContains(
                "<#forEach x in 1..3>${x}</#foreach>",
                "forEach", "foreach", "camel");
        assertErrorContains(
                "<#foreach x in 1..3>${x}</#forEach>",
                "forEach", "foreach", "camel");
    }
    
}
