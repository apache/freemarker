/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package freemarker.manual;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.FirstMatchTemplateConfigurationFactory;
import freemarker.cache.MergingTemplateConfigurationFactory;
import freemarker.cache.OrMatcher;
import freemarker.cache.PathGlobMatcher;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.TemplateConfiguration;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.DateUtil;

public class TemplateConfigurationExamples extends ExamplesTest {

    @Test
    public void example1() throws Exception {
        Configuration cfg = getConfiguration();

        addTemplate("t.xml", "");
        
        TemplateConfiguration tcUTF8XML = new TemplateConfiguration();
        tcUTF8XML.setEncoding("utf-8");
        tcUTF8XML.setOutputFormat(XMLOutputFormat.INSTANCE);

        {
            cfg.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(
                    new FileExtensionMatcher("xml"), tcUTF8XML));
            
            Template t = cfg.getTemplate("t.xml");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
        }

        {
            cfg.setTemplateConfigurations(null);
            cfg.setSettings(loadPropertiesFile("TemplateConfigurationExamples1.properties"));
            
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

        TemplateConfiguration tcSubject = new TemplateConfiguration();
        tcSubject.setOutputFormat(PlainTextOutputFormat.INSTANCE);
        
        TemplateConfiguration tcBody = new TemplateConfiguration();
        tcBody.setOutputFormat(HTMLOutputFormat.INSTANCE);
        
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(
                        new PathGlobMatcher("mail/**"),
                        new FirstMatchTemplateConfigurationFactory(
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*.subject.*"),
                                        tcSubject),
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*.body.*"),
                                        tcBody)
                                )
                                .noMatchErrorDetails("Mail template names must contain \".subject.\" or \".body.\"!")
                        ));
        
        assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getTemplate("t.subject.ftl").getOutputFormat());
        assertEquals(PlainTextOutputFormat.INSTANCE, cfg.getTemplate("mail/t.subject.ftl").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("mail/t.body.ftl").getOutputFormat());
        
        // From properties:
        
        cfg.setTemplateConfigurations(null);
        cfg.setSettings(loadPropertiesFile("TemplateConfigurationExamples2.properties"));
        
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

        TemplateConfiguration tcStats = new TemplateConfiguration();
        tcStats.setDateTimeFormat("iso");
        tcStats.setDateFormat("iso");
        tcStats.setTimeFormat("iso");
        tcStats.setTimeZone(DateUtil.UTC);

        TemplateConfiguration tcMail = new TemplateConfiguration();
        tcMail.setEncoding("utf-8");
        
        TemplateConfiguration tcHTML = new TemplateConfiguration();
        tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
        
        TemplateConfiguration tcXML = new TemplateConfiguration();
        tcXML.setOutputFormat(XMLOutputFormat.INSTANCE);
        
        cfg.setTemplateConfigurations(
                new MergingTemplateConfigurationFactory(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*.stats.*"),
                                tcStats),
                        new ConditionalTemplateConfigurationFactory(
                                new PathGlobMatcher("mail/**"),
                                tcMail),
                        new FirstMatchTemplateConfigurationFactory(
                                new ConditionalTemplateConfigurationFactory(
                                        new FileExtensionMatcher("xml"),
                                        tcXML),
                                new ConditionalTemplateConfigurationFactory(
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
        
        cfg.setTemplateConfigurations(null);
        cfg.setSettings(loadPropertiesFile("TemplateConfigurationExamples3.properties"));
        
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.html").getOutputFormat());
        assertEquals("ISO-8859-1", cfg.getTemplate("t.html").getEncoding());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.htm").getOutputFormat());
        assertEquals(XMLOutputFormat.INSTANCE, cfg.getTemplate("t.xml").getOutputFormat());
        assertEquals(HTMLOutputFormat.INSTANCE, cfg.getTemplate("t.stats.html").getOutputFormat());
        assertOutputForNamed("t.stats.html", "2015-08-24T15:53:26.011Z 2015-08-24 15:53:26.011Z");
        assertEquals("utf-8", cfg.getTemplate("mail/t.html").getEncoding());
    }
    
}
