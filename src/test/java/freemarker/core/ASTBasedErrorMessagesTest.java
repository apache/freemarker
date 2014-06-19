package freemarker.core;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

public class ASTBasedErrorMessagesTest {
    
    private Configuration cfg = new Configuration(new Version(2, 3, 0));
    private Map<String, Object> dataModel = new HashMap<String, Object>();
    {
        dataModel.put("map", Collections.singletonMap("key", "value"));
        dataModel.put("overloads", new Overloads());
    }

    @Test
    public void testOverloadSelectionError() {
        errorContains("${overloads.m(null)}", "2.3.21", "overloaded");
    }
    
    @Test
    public void testInvalidRefBasic() {
        errorContains("${foo}", "foo", "specify a default");
        errorContains("${map[foo]}", "foo", "\\!map[", "specify a default");
    }
    
    @Test
    public void testInvalidRefDollar() {
        errorContains("${$x}", "$x", "must not start with \"$\"", "specify a default");
        errorContains("${map.$x}", "map.$x", "must not start with \"$\"", "specify a default");
    }

    @Test
    public void testInvalidRefAfterDot() {
        errorContains("${map.foo.bar}", "map.foo", "\\!foo.bar", "after the last dot", "specify a default");
    }

    @Test
    public void testInvalidRefInSquareBrackets() {
        errorContains("${map['foo']}", "map", "final [] step", "specify a default");
    }

    @Test
    public void testInvalidRefSize() {
        errorContains("${map.size()}", "map.size", "?size", "specify a default");
        errorContains("${map.length()}", "map.length", "?length", "specify a default");
    }
    
    private void errorContains(String ftl, String... expectedSubstrings) {
        try {
            new Template("adhoc", ftl, cfg).process(dataModel, new StringWriter());
            fail("The tempalte had to fail");
        } catch (TemplateException e) {
            String msg = e.getMessageWithoutStackTop();
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
    
    public static class Overloads {
        
        public void m(String s) {}
        public void m(int i) {}
        
    }
    
}
