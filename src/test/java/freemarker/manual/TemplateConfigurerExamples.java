package freemarker.manual;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.FirstMatchTemplateConfigurerFactory;
import freemarker.cache.MergingTemplateConfigurerFactory;
import freemarker.cache.OrMatcher;
import freemarker.cache.PathGlobMatcher;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.TemplateConfigurer;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.DateUtil;

public class TemplateConfigurerExamples extends ExamplesTest {

    @Test
    public void example1() throws Exception {
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

    @Test
    public void example2() throws Exception {
        Configuration cfg = getConfiguration();
        
        addTemplate("t.subject.ftl", "");
        addTemplate("mail/t.subject.ftl", "");
        addTemplate("mail/t.body.ftl", "");

        TemplateConfigurer tcSubject = new TemplateConfigurer();
        tcSubject.setOutputFormat(PlainTextOutputFormat.INSTANCE);
        
        TemplateConfigurer tcBody = new TemplateConfigurer();
        tcBody.setOutputFormat(HTMLOutputFormat.INSTANCE);
        
        cfg.setTemplateConfigurers(
                new ConditionalTemplateConfigurerFactory(
                        new PathGlobMatcher("mail/**"),
                        new FirstMatchTemplateConfigurerFactory(
                                new ConditionalTemplateConfigurerFactory(
                                        new FileNameGlobMatcher("*.subject.*"),
                                        tcSubject),
                                new ConditionalTemplateConfigurerFactory(
                                        new FileNameGlobMatcher("*.body.*"),
                                        tcBody)
                                )
                                .noMatchErrorDetails("Mail template names must contain \".subject.\" or \".body.\"!")
                        ));
        
        assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getTemplate("t.subject.ftl").getOutputFormat());
        assertEquals(PlainTextOutputFormat.INSTANCE, cfg.getTemplate("mail/t.subject.ftl").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.body.ftl").getOutputFormat());
        
        // From properties:
        
        cfg.setTemplateConfigurers(null);
        cfg.setSettings(loadPropertiesFile("TemplateConfigurerExamples2.properties"));
        
        assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getTemplate("t.subject.ftl").getOutputFormat());
        assertEquals(PlainTextOutputFormat.INSTANCE, cfg.getTemplate("mail/t.subject.ftl").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.body.ftl").getOutputFormat());
    };
    
    @Test
    public void example3() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setDefaultEncoding("ISO-8859-1");
        cfg.setSharedVariable("ts", new Date(1440431606011L));
        
        addTemplate("t.stats.html", "${ts?datetime} ${ts?date} ${ts?time}");
        addTemplate("t.html", "");
        addTemplate("t.htm", "");
        addTemplate("t.xml", "");
        addTemplate("mail/t.html", "");

        TemplateConfigurer tcStats = new TemplateConfigurer();
        tcStats.setDateTimeFormat("iso");
        tcStats.setDateFormat("iso");
        tcStats.setTimeFormat("iso");
        tcStats.setTimeZone(DateUtil.UTC);

        TemplateConfigurer tcMail = new TemplateConfigurer();
        tcMail.setEncoding("utf-8");
        
        TemplateConfigurer tcHTML = new TemplateConfigurer();
        tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
        
        TemplateConfigurer tcXML = new TemplateConfigurer();
        tcXML.setOutputFormat(XMLOutputFormat.INSTANCE);
        
        cfg.setTemplateConfigurers(
                new MergingTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(
                                new FileNameGlobMatcher("*.stats.*"),
                                tcStats),
                        new ConditionalTemplateConfigurerFactory(
                                new PathGlobMatcher("mail/**"),
                                tcMail),
                        new FirstMatchTemplateConfigurerFactory(
                                new ConditionalTemplateConfigurerFactory(
                                        new FileExtensionMatcher("xml"),
                                        tcXML),
                                new ConditionalTemplateConfigurerFactory(
                                        new OrMatcher(
                                                new FileExtensionMatcher("html"),
                                                new FileExtensionMatcher("htm")),
                                        tcHTML)
                        ).allowNoMatch(true)
                )
        );
        
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
        assertEquals("ISO-8859-1", cfg.getTemplate("t.html").getEncoding());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
        assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.stats.html").getOutputFormat());
        assertOutputForNamed("t.stats.html", "2015-08-24T15:53:26.011Z 2015-08-24 15:53:26.011Z");
        assertEquals("utf-8", cfg.getTemplate("mail/t.html").getEncoding());
        
        // From properties:
        
        cfg.setTemplateConfigurers(null);
        cfg.setSettings(loadPropertiesFile("TemplateConfigurerExamples3.properties"));
        
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
        assertEquals("ISO-8859-1", cfg.getTemplate("t.html").getEncoding());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
        assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.stats.html").getOutputFormat());
        assertOutputForNamed("t.stats.html", "2015-08-24T15:53:26.011Z 2015-08-24 15:53:26.011Z");
        assertEquals("utf-8", cfg.getTemplate("mail/t.html").getEncoding());
    }
    
}
