package freemarker.core;

import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.test.TemplateTest;

public class OutputFormatTest extends TemplateTest {

    @Test
    public void testSettingLayers() throws Exception {
        addTemplate("t", "${.outputFormat}");
        addTemplate("t.xml", "${.outputFormat}");
        addTemplate("tWithHeader", "<#ftl outputFormat='HTML'>${.outputFormat}");
        
        Configuration cfg = getConfiguration();
        for (String cfgOutputFormat
                : new String[] { Configuration.RAW_OUTPUT_FORMAT, Configuration.RTF_OUTPUT_FORMAT } ) {
            if (!cfgOutputFormat.equals(Configuration.RAW_OUTPUT_FORMAT)) {
                cfg.setOutputFormat(cfgOutputFormat);
            }
            
            assertEquals(cfgOutputFormat, cfg.getOutputFormat());
            
            {
                Template t = cfg.getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            {
                Template t = cfg.getTemplate("t.xml");
                assertEquals(Configuration.XML_OUTPUT_FORMAT, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            {
                Template t = cfg.getTemplate("tWithHeader");
                assertEquals(Configuration.HTML_OUTPUT_FORMAT, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            cfg.clearTemplateCache();
        }
    }
    
    @Override
    protected Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        
        TemplateConfigurer xmlTC = new TemplateConfigurer();
        xmlTC.setOutputFormat(Configuration.XML_OUTPUT_FORMAT);
        cfg.setTemplateConfigurers(
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.xml"), xmlTC));
        
        return cfg;
    }
    
}
