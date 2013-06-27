package freemarker.template;

import java.io.IOException;

import junit.framework.TestCase;

public class ActualTagSyntaxTest extends TestCase {
    
    public ActualTagSyntaxTest(String name) {
        super(name);
    }

    public void testWithFtlHeader() throws IOException {
        testWithFtlHeader(Configuration.AUTO_DETECT_TAG_SYNTAX);
        testWithFtlHeader(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        testWithFtlHeader(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    }
    
    private void testWithFtlHeader(int cfgTagSyntax) throws IOException {
        assertEquals(getActualTagSyntax("[#ftl]foo", cfgTagSyntax), Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("<#ftl>foo", cfgTagSyntax), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
    }

    public void testUndecidable() throws IOException {
        assertEquals(getActualTagSyntax("foo", Configuration.AUTO_DETECT_TAG_SYNTAX), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo", Configuration.ANGLE_BRACKET_TAG_SYNTAX), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo", Configuration.SQUARE_BRACKET_TAG_SYNTAX), Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    }

    public void testDecidableWithoutFtlHeader() throws IOException {
        assertEquals(getActualTagSyntax("foo<#if true></#if>", Configuration.AUTO_DETECT_TAG_SYNTAX), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo<#if true></#if>", Configuration.ANGLE_BRACKET_TAG_SYNTAX), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo<#if true></#if>", Configuration.SQUARE_BRACKET_TAG_SYNTAX), Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", Configuration.AUTO_DETECT_TAG_SYNTAX), Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", Configuration.ANGLE_BRACKET_TAG_SYNTAX), Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", Configuration.SQUARE_BRACKET_TAG_SYNTAX), Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    }
    
    private int getActualTagSyntax(String ftl, int cfgTagSyntax) throws IOException {
        Configuration cfg = new Configuration();
        cfg.setTagSyntax(cfgTagSyntax);
        return new Template(null, ftl, cfg).getActualTagSyntax();
    }
    
}
