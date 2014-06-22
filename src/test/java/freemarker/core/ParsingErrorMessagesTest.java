package freemarker.core;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

public class ParsingErrorMessagesTest {

    private Configuration cfg = new Configuration(new Version(2, 3, 21));
    {
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
    }
    
    @Test
    public void testNeedlessInterpolation() {
        errorContains("<#if ${x} == 3></#if>", "instead of ${");
        errorContains("<#if ${x == 3}></#if>", "instead of ${");
        errorContains("<@foo ${x == 3} />", "instead of ${");
    }

    @Test
    public void testWrongDirectiveNames() {
        errorContains("<#foo />", "nknown directive", "#foo");
        errorContains("<#set x = 1 />", "nknown directive", "#set", "#assign");
        errorContains("<#iterator></#iterator>", "nknown directive", "#iterator", "#list");
    }

    @Test
    public void testBug402() {
        errorContains("<#list 1..i as k>${k}<#list>", "parameters", "start-tag", "#list");
        errorContains("<#assign>", "parameters", "start-tag", "#assign");
    }

    @Test
    public void testUnclosedDirectives() {
        errorContains("<#macro x>", "#macro", "unclosed");
        errorContains("<#function x>", "#macro", "unclosed");
        errorContains("<#assign x>", "#assign", "unclosed");
        errorContains("<#macro m><#local x>", "#local", "unclosed");
        errorContains("<#global x>", "#global", "unclosed");
        errorContains("<@foo>", "@...", "unclosed");
        errorContains("<#list xs as x>", "#list", "unclosed");
        errorContains("<#list xs as x><#if x>", "#if", "unclosed");
        errorContains("<#list xs as x><#if x><#if q><#else>", "#if", "unclosed");
        errorContains("<#list xs as x><#if x><#if q><#else><#macro x>qwe", "#macro", "unclosed");
        errorContains("${(blah", "\"(\"", "unclosed");
        errorContains("${blah", "\"{\"", "unclosed");
    }
    
    private void errorContains(String ftl, String... expectedSubstrings) {
        errorContains(false, ftl, expectedSubstrings);
        errorContains(true, ftl, expectedSubstrings);
    }

    private void errorContains(boolean squareTags, String ftl, String... expectedSubstrings) {
        try {
            if (squareTags) {
                ftl = ftl.replace('<', '[').replace('>', ']');
            }
            new Template("adhoc", ftl, cfg);
            fail("The tempalte had to fail");
        } catch (ParseException e) {
            String msg = e.getMessage();
            for (String needle: expectedSubstrings) {
                if (needle.startsWith("\\!")) {
                    String netNeedle = needle.substring(2); 
                    if (msg.contains(netNeedle)) {
                        fail("The message shouldn't contain substring " + StringUtil.jQuote(netNeedle) + ":\n" + msg);
                    }
                } else if (!msg.contains(needle)) {
                    fail("The message didn't contain substring " + StringUtil.jQuote(needle) + ":\n" + msg);
                }
            }
            showError(e);
        } catch (IOException e) {
            // Won't happen
            throw new RuntimeException(e);
        }
    }
    
    private void showError(Throwable e) {
        //System.out.println(e);
    }

}
