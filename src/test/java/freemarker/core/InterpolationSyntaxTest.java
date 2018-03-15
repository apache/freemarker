package freemarker.core;

import static freemarker.template.Configuration.*;

import org.junit.Test;

import freemarker.test.TemplateTest;

public class InterpolationSyntaxTest extends TemplateTest {

    @Test
    public void legacyInterpolationSyntaxTest() throws Exception {
        // The default is: getConfiguration().setInterpolationSyntax(Configuration.LEGACY_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "1 1 [=1]");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "1 1 [={'x': 1}['x']]");
    }

    @Test
    public void dollarInterpolationSyntaxTest() throws Exception {
        getConfiguration().setInterpolationSyntax(DOLLAR_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "1 #{1} [=1]");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "1 #{{'x': 1}['x']} [={'x': 1}['x']]");
    }

    @Test
    public void squareBracketInterpolationSyntaxTest() throws Exception {
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "${1} #{1} 1");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "${{'x': 1}['x']} #{{'x': 1}['x']} 1");

        assertOutput("[=1]][=2]]", "1]2]");
        assertOutput("[= 1 ][= <#-- c --> 2 <#-- c --> ]", "12");
        assertOutput("[ =1]", "[ =1]");
        
        assertErrorContains("<#if [true][0]]></#if>", "\"]\"", "nothing open");
        assertOutput("[#ftl][#if [true][0]]>[/#if]", ">");
        
        assertOutput("[='a[=1]b']", "a1b");
    }

    @Test
    public void squareBracketTagSyntaxStillWorks() throws Exception {
        getConfiguration().setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
        for (int syntax : new int[] {
                LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX, SQUARE_BRACKET_INTERPOLATION_SYNTAX }) {
            assertOutput("[#if [true][0]]t[#else]f[/#if]", "t");
        }
    }
    
    @Test
    public void legacyTagSyntaxGlitchStillWorksTest() throws Exception {
        String ftl = "<#if [true][0]]t<#else]f</#if]";
        
        for (int syntax : new int[] { LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX }) {
            getConfiguration().setInterpolationSyntax(syntax);
            assertOutput(ftl, "t");
        }
        
        // Glitch is not emulated with this:
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        assertErrorContains(ftl, "\"]\"");
    }

    @Test
    public void errorMessagesAreSquareBracketInterpolationSyntaxAwareTest() throws Exception {
        assertErrorContains("<#if ${x}></#if>", "${...}", "${myExpression}");
        assertErrorContains("<#if #{x}></#if>", "#{...}", "#{myExpression}");
        assertErrorContains("<#if [=x]></#if>", "[=...]", "[=myExpression]");
    }

    @Test
    public void unclosedSyntaxErrorTest() throws Exception {
        assertErrorContains("${1", "unclosed \"{\"");
        
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        assertErrorContains("[=1", "unclosed \"[\"");
    }
    
}
