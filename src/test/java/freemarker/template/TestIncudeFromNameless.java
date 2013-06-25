package freemarker.template;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;
import freemarker.cache.StringTemplateLoader;

public class TestIncudeFromNameless extends TestCase {

    public TestIncudeFromNameless(String name) {
        super(name);
    }
    
    public void test() throws IOException, TemplateException {
        Configuration cfg = new Configuration();
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("i.ftl", "[i]");
        tl.putTemplate("sub/i.ftl", "[sub/i]");
        tl.putTemplate("import.ftl", "<#assign x = 1>");
        cfg.setTemplateLoader(tl);
        
        Template t = new Template(null, new StringReader(
                    "<#include 'i.ftl'>\n"
                    + "<#include '/i.ftl'>\n"
                    + "<#include 'sub/i.ftl'>\n"
                    + "<#include '/sub/i.ftl'>"
                    + "<#import 'import.ftl' as i>${i.x}"
                ),
                cfg);
        StringWriter out = new StringWriter();
        t.process(null, out);
        assertEquals("[i][i][sub/i][sub/i]1", out.toString());
    }

}
