package freemarker.manual;

import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.cache.FirstMatchTemplateConfigurerFactory;
import freemarker.cache.OrMatcher;
import freemarker.cache.PathGlobMatcher;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.RTFOutputFormat;
import freemarker.core.TemplateConfigurer;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;

public class ConfigureOutputFormatExamples extends ExamplesTest {
    
    @Test
    public void test() throws Exception {
        // Example 1:
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        // Or:
        cfg.setRecognizeStandardFileExtensions(true);

        setupConfiguration(cfg);
        
        // Example 2/a:
        {
            TemplateConfigurer tcHTML = new TemplateConfigurer();
            tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
            
            cfg.setTemplateConfigurers(
                    new ConditionalTemplateConfigurerFactory(
                            new PathGlobMatcher("mail/**"),
                            tcHTML));
            
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.ftl").getOutputFormat());
        }

        // Example 2/b:
        {
            cfg.setTemplateConfigurers(null); // Just to be sure...
            
            cfg.setSettings(loadPropertiesFile("ConfigureOutputFormatExamples1.properties"));
                
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.ftl").getOutputFormat());
        }
        
        // Example 3/a:
        {
            TemplateConfigurer tcHTML = new TemplateConfigurer();
            tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
            
            TemplateConfigurer tcXML = new TemplateConfigurer();
            tcXML.setOutputFormat(XMLOutputFormat.INSTANCE);

            TemplateConfigurer tcRTF = new TemplateConfigurer();
            tcRTF.setOutputFormat(RTFOutputFormat.INSTANCE);
            
            cfg.setTemplateConfigurers(
                    new FirstMatchTemplateConfigurerFactory(
                            new ConditionalTemplateConfigurerFactory(
                                    new FileExtensionMatcher("xml"),
                                    tcXML),
                            new ConditionalTemplateConfigurerFactory(
                                    new OrMatcher(
                                            new FileExtensionMatcher("html"),
                                            new FileExtensionMatcher("htm")),
                                    tcHTML),
                            new ConditionalTemplateConfigurerFactory(
                                    new FileExtensionMatcher("rtf"),
                                    tcRTF)
                    ).allowNoMatch(true)
            );
            
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
            assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
            assertEquals(RTFOutputFormat.INSTANCE, cfg.getTemplate("t.rtf").getOutputFormat());
        }

        // Example 3/b:
        {
            cfg.setTemplateConfigurers(null); // Just to be sure...
            
            cfg.setSettings(loadPropertiesFile("ConfigureOutputFormatExamples2.properties"));
            
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
            assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
            assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
            assertEquals(RTFOutputFormat.INSTANCE, cfg.getTemplate("t.rtf").getOutputFormat());
        }
        
    }
    
}
