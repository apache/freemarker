package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class CamelCaseTest extends TemplateTest {

    @Test
    public void camelCaseSpecialVars() throws IOException, TemplateException {
        getConfiguration().setOutputEncoding("utf-8");
        getConfiguration().setURLEscapingCharset("iso-8859-1");
        getConfiguration().setLocale(Locale.GERMANY);
        assertOutput("${.dataModel?isHash?c}", "true");
        assertOutput("${.data_model?is_hash?c}", "true");
        assertOutput("${.localeObject.toString()}", "de_DE");
        assertOutput("${.locale_object.toString()}", "de_DE");
        assertOutput("${.templateName?length}", "0");
        assertOutput("${.template_name?length}", "0");
        assertOutput("${.outputEncoding}", "utf-8");
        assertOutput("${.output_encoding}", "utf-8");
        assertOutput("${.urlEscapingCharset}", "iso-8859-1");
        assertOutput("${.url_escaping_charset}", "iso-8859-1");
        assertOutput("${.currentNode!'-'}", "-");
        assertOutput("${.current_node!'-'}", "-");
    }

    @Test
    public void camelCaseSpecialVarsInErrorMessage() throws IOException, TemplateException {
        assertErrorContains("${.fooBar}", "dataModel", "\\!data_model");
        assertErrorContains("${.foo_bar}", "data_model", "\\!dataModel");
        // [2.4] If camel case will be the recommended style, then this need to be inverted:
        assertErrorContains("${.foo}", "data_model", "\\!dataModel");
        
        assertErrorContains("<#if x><#elseIf y></#if>${.foo}", "dataModel", "\\!data_model");
        assertErrorContains("<#if x><#elseif y></#if>${.foo}", "data_model", "\\!dataModel");
        
        getConfiguration().setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        assertErrorContains("${.foo}", "dataModel", "\\!data_model");
        getConfiguration().setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
        assertErrorContains("${.foo}", "data_model", "\\!dataModel");
    }
    
    @Test
    public void specialVarsHasBothNamingStyle() throws IOException, TemplateException {
        assertContainsBothNamingStyles(
                new HashSet(Arrays.asList(BuiltinVariable.SPEC_VAR_NAMES)),
                new NamePairAssertion() { public void assertPair(String name1, String name2) { } });
    }
    
    @Test
    public void camelCaseBuiltIns() throws IOException, TemplateException {
        assertOutput("${'x'?upperCase}", "X");
        assertOutput("${'x'?upper_case}", "X");
    }

    @Test
    public void stringLiteralInterpolation() throws IOException, TemplateException {
        assertEquals(Configuration.AUTO_DETECT_NAMING_CONVENTION, getConfiguration().getNamingConvention());
        getConfiguration().setSharedVariable("x", "x");
        
        assertOutput("${'-${x?upperCase}-'} ${x?upperCase}", "-X- X");
        assertOutput("${x?upperCase} ${'-${x?upperCase}-'}", "X -X-");
        assertOutput("${'-${x?upper_case}-'} ${x?upper_case}", "-X- X");
        assertOutput("${x?upper_case} ${'-${x?upper_case}-'}", "X -X-");

        assertErrorContains("${'-${x?upper_case}-'} ${x?upperCase}",
                "naming convention", "legacy", "upperCase", "detection", "9");
        assertErrorContains("${x?upper_case} ${'-${x?upperCase}-'}",
                "naming convention", "legacy", "upperCase", "detection", "5");
        assertErrorContains("${'-${x?upperCase}-'} ${x?upper_case}",
                "naming convention", "camel", "upper_case");
        assertErrorContains("${x?upperCase} ${'-${x?upper_case}-'}",
                "naming convention", "camel", "upper_case");
        
        getConfiguration().setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        assertOutput("${'-${x?upperCase}-'} ${x?upperCase}", "-X- X");
        assertErrorContains("${'-${x?upper_case}-'}",
                "naming convention", "camel", "upper_case", "\\!detection");
        
        getConfiguration().setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
        assertOutput("${'-${x?upper_case}-'} ${x?upper_case}", "-X- X");
        assertErrorContains("${'-${x?upperCase}-'}",
                "naming convention", "legacy", "upperCase", "\\!detection");
    }
    
    @Test
    public void evalAndInterpret() throws IOException, TemplateException {
        assertEquals(Configuration.AUTO_DETECT_NAMING_CONVENTION, getConfiguration().getNamingConvention());
        // The naming convention detected doesn't affect the enclosing template's naming convention.
        // - ?eval:
        assertOutput("${\"'x'?upperCase\"?eval}${'x'?upper_case}", "XX");
        assertOutput("${\"'x'?upper_case\"?eval}${'x'?upperCase}", "XX");
        assertOutput("${'x'?upperCase}${\"'x'?upper_case\"?eval}", "XX");
        assertErrorContains("${\"'x'\n?upperCase\n?is_string\"?eval}",
                "naming convention", "camel", "upperCase", "is_string", "line 2", "line 3");
        // - ?interpret:
        assertOutput("<@r\"${'x'?upperCase}\"?interpret />${'x'?upper_case}", "XX");
        assertOutput("<@r\"${'x'?upper_case}\"?interpret />${'x'?upperCase}", "XX");
        assertOutput("${'x'?upper_case}<@r\"${'x'?upperCase}\"?interpret />", "XX");
        assertErrorContains("<@r\"${'x'\n?upperCase\n?is_string}\"?interpret />",
                "naming convention", "camel", "upperCase", "is_string", "line 2", "line 3");
        
        // Will be inherited by ?eval-ed/?interpreted fragments:
        getConfiguration().setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        // - ?eval:
        assertErrorContains("${\"'x'?upper_case\"?eval}", "naming convention", "camel", "upper_case");
        assertOutput("${\"'x'?upperCase\"?eval}", "X");
        // - ?interpret:
        assertErrorContains("<@r\"${'x'?upper_case}\"?interpret />", "naming convention", "camel", "upper_case");
        assertOutput("<@r\"${'x'?upperCase}\"?interpret />", "X");
        
        // Again, will be inherited by ?eval-ed/?interpreted fragments:
        getConfiguration().setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
        // - ?eval:
        assertErrorContains("${\"'x'?upperCase\"?eval}", "naming convention", "legacy", "upperCase");
        assertOutput("${\"'x'?upper_case\"?eval}", "X");
        // - ?interpret:
        assertErrorContains("<@r\"${'x'?upperCase}\"?interpret />", "naming convention", "legacy", "upperCase");
        assertOutput("<@r\"${'x'?upper_case}\"?interpret />", "X");
    }
    
    @Test
    public void camelCaseBuiltInErrorMessage() throws IOException, TemplateException {
        assertErrorContains("${'x'?upperCasw}", "upperCase", "\\!upper_case");
        assertErrorContains("${'x'?upper_casw}", "upper_case", "\\!upperCase");
        // [2.4] If camel case will be the recommended style, then this need to be inverted:
        assertErrorContains("${'x'?foo}", "upper_case", "\\!upperCase");
        
        assertErrorContains("<#if x><#elseIf y></#if> ${'x'?foo}", "upperCase", "\\!upper_case");
        assertErrorContains("<#if x><#elseif y></#if>${'x'?foo}", "upper_case", "\\!upperCase");
        
        getConfiguration().setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        assertErrorContains("${'x'?foo}", "upperCase", "\\!upper_case");
        getConfiguration().setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
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
            if (_CoreStringUtils.getIdentifierNamingConvention(name) == Configuration.CAMEL_CASE_NAMING_CONVENTION) {
                String underscoredName = correctIsoBIExceptions(_CoreStringUtils.camelCaseToUnderscored(name)); 
                assertTrue(
                        "Missing underscored variation \"" + underscoredName + "\" for \"" + name + "\".",
                        names.contains(underscoredName));
                assertTrue(underscoredNamesWithCamelCasePair.add(underscoredName));
                
                namePairAssertion.assertPair(name, underscoredName);
            }
        }
        for (String name : names) {
            if (_CoreStringUtils.getIdentifierNamingConvention(name) == Configuration.LEGACY_NAMING_CONVENTION) {
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

        assertOutput("<foreach x in 1..3>${x}</foreach> <#foreach x in 1..3>${x}</#foreach>",
                "123 123");
        assertErrorContains("<foreach x in 1..3>${x}</foreach> <#forEach x in 1..3>${x}</#forEach>",
                "naming convention", "legacy", "#forEach");
        assertErrorContains("<#forEach x in 1..3>${x}</#forEach> <foreach x in 1..3>${x}</foreach>",
                "naming convention", "camel", "foreach");
        
        camelCaseDirectives();
    }
    
    @Test
    public void camelCaseDirectives() throws IOException, TemplateException {
        camelCaseDirectives(false);
        getConfiguration().setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        camelCaseDirectives(true);
    }

    private void camelCaseDirectives(boolean squared) throws IOException, TemplateException {
        assertOutput(
                squared("<#list 1..4 as x><#if x == 1>one <#elseIf x == 2>two <#elseIf x == 3>three "
                        + "<#else>more</#if></#list>", squared),
                "one two three more");
        assertOutput(
                squared("<#list 1..4 as x><#if x == 1>one <#elseif x == 2>two <#elseif x == 3>three "
                        + "<#else>more</#if></#list>", squared),
                "one two three more");
        
        assertOutput(
                squared("<#escape x as x?upperCase>${'a'}<#noEscape>${'b'}</#noEscape></#escape>", squared),
                "Ab");
        assertOutput(
                squared("<#escape x as x?upper_case>${'a'}<#noescape>${'b'}</#noescape></#escape>", squared),
                "Ab");
        
        assertOutput(
                squared("<#noParse></#noparse></#noParse>", squared),
                squared("</#noparse>", squared));
        assertOutput(
                squared("<#noparse></#noParse></#noparse>", squared),
                squared("</#noParse>", squared));
        
        assertOutput(
                squared("<#forEach x in 1..3>${x}</#forEach>", squared),
                "123");
        assertOutput(
                squared("<#foreach x in 1..3>${x}</#foreach>", squared),
                "123");
    }
    
    private String squared(String ftl, boolean squared) {
        return squared ? ftl.replace('<', '[').replace('>', ']') : ftl;
    }

    @Test
    public void explicitNamingConvention() throws IOException, TemplateException {
        explicitNamingConvention(false);
        explicitNamingConvention(true);
    }
    
    private void explicitNamingConvention(boolean squared) throws IOException, TemplateException {
        if (squared) {
            getConfiguration().setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        }
        
        getConfiguration().setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        
        assertErrorContains(
                squared("<#if true>t<#elseif false>f</#if>", squared),
                "naming convention", "camel", "#elseif");
        assertOutput(
                squared("<#if true>t<#elseIf false>f</#if>", squared),
                "t");
        
        assertErrorContains(
                squared("<#noparse>${x}</#noparse>", squared),
                "naming convention", "camel", "#noparse");
        assertOutput(
                squared("<#noParse>${x}</#noParse>", squared),
                "${x}");
        
        assertErrorContains(
                squared("<#escape x as -x><#noescape>${1}</#noescape></#escape>", squared),
                "naming convention", "camel", "#noescape");
        assertOutput(
                squared("<#escape x as -x><#noEscape>${1}</#noEscape></#escape>", squared),
                "1");
        
        assertErrorContains(
                squared("<#foreach x in 1..3>${x}</#foreach>", squared),
                "naming convention", "camel", "#foreach");
        assertOutput(
                squared("<#forEach x in 1..3>${x}</#forEach>", squared),
                "123");
        
        // ---
        
        getConfiguration().setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
        
        assertErrorContains(
                squared("<#if true>t<#elseIf false>f</#if>", squared),
                "naming convention", "legacy", "#elseIf");
        assertOutput(
                squared("<#if true>t<#elseif false>f</#if>", squared),
                "t");
        
        assertErrorContains(
                squared("<#noParse>${x}</#noParse>", squared),
                "naming convention", "legacy", "#noParse");
        assertOutput(
                squared("<#noparse>${x}</#noparse>", squared),
                "${x}");
        
        assertErrorContains(
                squared("<#escape x as -x><#noEscape>${1}</#noEscape></#escape>", squared),
                "naming convention", "legacy", "#noEscape");
        assertOutput(
                squared("<#escape x as -x><#noescape>${1}</#noescape></#escape>", squared),
                "1");
        
        assertErrorContains(
                squared("<#forEach x in 1..3>${x}</#forEach>", squared),
                "naming convention", "legacy", "#forEach");
        assertOutput(
                squared("<#foreach x in 1..3>${x}</#foreach>", squared),
                "123");
    }
    
    @Test
    public void inconsistentAutoDetectedNamingConvention() {
        assertErrorContains(
                "<#if x><#elseIf y><#elseif z></#if>",
                "naming convention", "camel");
        assertErrorContains(
                "<#if x><#elseif y><#elseIf z></#if>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#if x><#elseIf y></#if><#noparse></#noparse>",
                "naming convention", "camel");
        assertErrorContains(
                "<#if x><#elseif y></#if><#noParse></#noParse>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#if x><#elseif y><#elseIf z></#if>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#escape x as x + 1><#noEscape></#noescape></#escape>",
                "naming convention", "camel");
        assertErrorContains(
                "<#escape x as x + 1><#noEscape></#noEscape><#noescape></#noescape></#escape>",
                "naming convention", "camel");
        assertErrorContains(
                "<#escape x as x + 1><#noescape></#noEscape></#escape>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#escape x as x + 1><#noescape></#noescape><#noEscape></#noEscape></#escape>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#forEach x in 1..3>${x}</#foreach>",
                "naming convention", "camel");
        assertErrorContains(
                "<#forEach x in 1..3>${x}</#forEach><#foreach x in 1..3>${x}</#foreach>",
                "naming convention", "camel");
        assertErrorContains(
                "<#foreach x in 1..3>${x}</#forEach>",
                "naming convention", "legacy");
        assertErrorContains(
                "<#foreach x in 1..3>${x}</#foreach><#forEach x in 1..3>${x}</#forEach>",
                "naming convention", "legacy");
        
        assertErrorContains("${x?upperCase?is_string}",
                "naming convention", "camel", "upperCase", "is_string");
        assertErrorContains("${x?upper_case?isString}",
                "naming convention", "legacy", "upper_case", "isString");

        /* TODO
        assertErrorContains("<#setting outputEncoding='utf-8'>${x?is_string}",
                "naming convention", "camel", "outputEncoding", "is_string");
        */
        assertErrorContains("<#setting output_encoding='utf-8'>${x?isString}",
                "naming convention", "legacy", "output_encoding", "isString");
        
        assertErrorContains("${x?isString}<#setting output_encoding='utf-8'>",
                "naming convention", "camel", "isString", "output_encoding");
        /* TODO
        assertErrorContains("${x?is_string}<#setting outputEncoding='utf-8'>",
                "naming convention", "legacy", "is_string", "outputEncoding");
        */
        
        assertErrorContains("${.outputEncoding}${x?is_string}",
                "naming convention", "camel", "outputEncoding", "is_string");
        assertErrorContains("${.output_encoding}${x?isString}",
                "naming convention", "legacy", "output_encoding", "isString");
        
        assertErrorContains("${x?upperCase}<#noparse></#noparse>",
                "naming convention", "camel", "upperCase", "noparse");
        assertErrorContains("${x?upper_case}<#noParse></#noParse>",
                "naming convention", "legacy", "upper_case", "noParse");
    }
    
    private interface NamePairAssertion {
        
        void assertPair(String name1, String name2);
        
    }
    
}
