package freemarker.core;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

@Ignore
public abstract class TemplateErrorMessageTest {
    
    protected final Configuration cfg = new Configuration(new Version(2, 3, 0));
    private Map<String, Object> dataModel = new HashMap<String, Object>();
    {
        buildDataModel(dataModel);
    }
    
    protected void assertErrorContains(String ftl, String... expectedSubstrings) {
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
        } catch (IOException e) {
            // Won't happen
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("boxing")
    protected void buildDataModel(Map<String, Object> dataModel) {
        dataModel.put("map", Collections.singletonMap("key", "value"));
        dataModel.put("list", Collections.singletonList("item"));
        dataModel.put("s", "text");
        dataModel.put("n", 1);
        dataModel.put("b", true);
        dataModel.put("bean", new TestBean());
    }
    
    public static class TestBean {
        private int x;
        private boolean b;
        
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public boolean isB() {
            return b;
        }
        public void setB(boolean b) {
            this.b = b;
        }

        public int intM() {
            return 1;
        }

        public int intMP(int x) {
            return x;
        }
        
        public void voidM() {
            
        }
        
    }

}
