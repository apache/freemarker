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
    public void testOutputFormatSettingLayers() throws Exception {
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

    @Test
    public void testAutoEscapingSettingLayers() throws Exception {
        addTemplate("t", "${'a&b'}");
        addTemplate("tWithHeaderFalse", "<#ftl autoEscaping=false>${'a&b'}");
        addTemplate("tWithHeaderTrue", "<#ftl autoEscaping=false>${'a&b'}");
        
        Configuration cfg = getConfiguration();
        
        assertTrue(cfg.getAutoEscaping());
        
        cfg.setOutputFormat(Configuration.XML_OUTPUT_FORMAT);
        
        for (boolean cfgAutoEscaping : new boolean[] { true, false }) {
            if (!cfgAutoEscaping) {
                cfg.setAutoEscaping(false);
            }
            
            {
                Template t = cfg.getTemplate("t");
                assertTrue(t.getAutoEscaping());
                if (cfgAutoEscaping) {
                    assertOutput(t, "a&b"); // TODO "a&amp;b"
                } else {
                    assertOutput(t, "a&b");
                }
            }
            
            {
                Template t = cfg.getTemplate("tWithHeaderFalse");
                assertFalse(t.getAutoEscaping());
                assertOutput(t, "a&b");
            }
            
            {
                Template t = cfg.getTemplate("tWithHeaderTrue");
                assertFalse(t.getAutoEscaping());
                assertOutput(t, "a&b"); // TODO "a&amp;b"
            }
        }
        
        cfg.clearTemplateCache();
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
