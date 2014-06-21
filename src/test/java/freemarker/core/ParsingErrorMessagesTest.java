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
    
    @Test
    public void testNeedlessInterpolation() {
        errorContains("<#if ${x} == 3></#if>", "instead of ${");
        errorContains("<#if ${x == 3}></#if>", "instead of ${");
        errorContains("<@foo ${x == 3} />", "instead of ${");
    }

    @Test
    public void testWrongDirectiveNames() {
        errorContains("<#foo />", "nknown directive", "foo");
        errorContains("<#set x = 1 />", "nknown directive", "set", "assign");
    }
    
    private void errorContains(String ftl, String... expectedSubstrings) {
        try {
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
