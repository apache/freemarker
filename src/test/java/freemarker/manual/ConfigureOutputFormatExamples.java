/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        Configuration cfg = getConfiguration();
        
        addTemplate("mail/t.ftl", "");
        addTemplate("t.html", "");
        addTemplate("t.htm", "");
        addTemplate("t.xml", "");
        addTemplate("t.rtf", "");
        
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
