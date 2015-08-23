package freemarker.manual;

import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.core.TemplateConfigurer;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class TemplateConfigurerExamples extends ExamplesTest {

    @Test
    public void test() throws Exception {
        Configuration cfg = getConfiguration();

        addTemplate("t.xml", "");
        
        TemplateConfigurer tcUTF8XML = new TemplateConfigurer();
        tcUTF8XML.setEncoding("utf-8");
        tcUTF8XML.setOutputFormat(XMLOutputFormat.INSTANCE);

        {
            cfg.setTemplateConfigurers(new ConditionalTemplateConfigurerFactory(
                    new FileExtensionMatcher("xml"), tcUTF8XML));
            
            Template t = cfg.getTemplate("t.xml");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
        }

        {
            cfg.setTemplateConfigurers(null);
            cfg.setSettings(loadPropertiesFile("TemplateConfigurerExamples1.properties"));
            
            Template t = cfg.getTemplate("t.xml");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
        }
    }

}
