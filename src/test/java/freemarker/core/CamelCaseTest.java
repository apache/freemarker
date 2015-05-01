package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class CamelCaseTest extends TemplateTest {

    @Test
    public void camelCaseBuiltIns() throws IOException, TemplateException {
        assertOutput("${'x'?upperCase} ${'x'?upper_case}", "X X");
    }

    @Test
    public void camelCaseBuiltInErrorMessage() throws IOException, TemplateException {
        assertErrorContains("${'x'?upperCasw}", "upperCase", "\\!upper_case");
        assertErrorContains("${'x'?upper_casw}", "upper_case", "\\!upperCase");
        // [2.4] If camel case will be the recommended style, then this need to be inverted:
        assertErrorContains("${'x'?foo}", "upper_case", "\\!upperCase");
    }
    
    @Test
    public void builtInsHasBothNamingStyle() throws IOException, TemplateException {
        assertContainsBothNamingStyles(getConfiguration().getSupportedBuiltInNames(), new NamePairAssertion() {

            public void assertPair(String name1, String name2) {
                BuiltIn bi1  = (BuiltIn) BuiltIn.builtins.get(name1);
                BuiltIn bi2 = (BuiltIn) BuiltIn.builtins.get(name2);
                assertTrue("\"" + name1 + "\" and \"" + name2 + "\" doesn't belong to the same BI object.",
                        bi1 == bi2);
            }
            
        });
    }

    private void assertContainsBothNamingStyles(Set<String> names, NamePairAssertion namePairAssertion) {
        Set<String> underscoredNamesWithCamelCasePair = new HashSet<String>();
        for (String name : names) {
            if (_CoreStringUtils.isCamelCaseIdentifier(name)) {
                String underscoredName = correctIsoBIExceptions(_CoreStringUtils.camelCaseToUnderscored(name)); 
                assertTrue(
                        "Missing underscored variation \"" + underscoredName + "\" for \"" + name + "\".",
                        names.contains(underscoredName));
                assertTrue(underscoredNamesWithCamelCasePair.add(underscoredName));
                
                namePairAssertion.assertPair(name, underscoredName);
            }
        }
        for (String name : names) {
            if (_CoreStringUtils.isUnderscoredIdentifier(name)) {
                assertTrue("Missing camel case variation for \"" + name + "\".",
                        underscoredNamesWithCamelCasePair.contains(name));
            }
        }
    }
    
    private String correctIsoBIExceptions(String underscoredName) {
        return underscoredName.replace("_n_z", "_nz").replace("_f_z", "_fz");
    }

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
    
    private interface NamePairAssertion {
        
        void assertPair(String name1, String name2);
        
    }
    
}
