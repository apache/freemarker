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
import freemarker.cache.OrMatcher;
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
    public void testStandardFileExtensions() throws Exception {
        String commonContent = "${.outputFormat}";
        addTemplate("t", commonContent);
        addTemplate("t.ftl", commonContent);
        addTemplate("t.ftlh", commonContent);
        addTemplate("t.FTLH", commonContent);
        addTemplate("t.fTlH", commonContent);
        addTemplate("t.ftlx", commonContent);
        addTemplate("t.FTLX", commonContent);
        addTemplate("t.fTlX", commonContent);
        addTemplate("tWithHeader.ftlx", "<#ftl outputFormat='HTML'>" + commonContent);
        
        Configuration cfg = getConfiguration();
        for (int setupNumber = 1; setupNumber <= 5; setupNumber++) {
            final String cfgOutputFormat;
            final String ftlhOutputFormat;
            final String ftlxOutputFormat;
            switch (setupNumber) {
            case 1:
                cfgOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                ftlhOutputFormat = Configuration.HTML_OUTPUT_FORMAT;
                ftlxOutputFormat = Configuration.XML_OUTPUT_FORMAT;
                break;
            case 2:
                cfgOutputFormat = Configuration.RTF_OUTPUT_FORMAT;
                cfg.setOutputFormat(cfgOutputFormat);
                ftlhOutputFormat = Configuration.HTML_OUTPUT_FORMAT;
                ftlxOutputFormat = Configuration.XML_OUTPUT_FORMAT;
                break;
            case 3:
                cfgOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                cfg.unsetOutputFormat();
                TemplateConfigurer tcXml = new TemplateConfigurer();
                tcXml.setOutputFormat(Configuration.XML_OUTPUT_FORMAT);
                cfg.setTemplateConfigurers(
                        new ConditionalTemplateConfigurerFactory(
                                new OrMatcher(
                                        new FileNameGlobMatcher("*.ftlh"),
                                        new FileNameGlobMatcher("*.FTLH"),
                                        new FileNameGlobMatcher("*.fTlH")),
                                tcXml));
                ftlhOutputFormat = Configuration.XML_OUTPUT_FORMAT;
                ftlxOutputFormat = Configuration.XML_OUTPUT_FORMAT;
                break;
            case 4:
                cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
                cfgOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                ftlhOutputFormat = Configuration.XML_OUTPUT_FORMAT;
                ftlxOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                break;
            case 5:
                cfg.setTemplateConfigurers(null);
                cfgOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                ftlhOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                ftlxOutputFormat = Configuration.RAW_OUTPUT_FORMAT;
                break;
            default:
                throw new AssertionError();
            }
            
            assertEquals(cfgOutputFormat, cfg.getOutputFormat());
            
            {
                Template t = cfg.getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            {
                Template t = cfg.getTemplate("t.ftl");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            for (String name : new String[] { "t.ftlh", "t.FTLH", "t.fTlH" }) {
                Template t = cfg.getTemplate(name);
                assertEquals(ftlhOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }
            
            for (String name : new String[] { "t.ftlx", "t.FTLX", "t.fTlX" }) {
                Template t = cfg.getTemplate(name);
                assertEquals(ftlxOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat());
            }

            {
                Template t = cfg.getTemplate("tWithHeader.ftlx");
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
        addTemplate("tWithHeaderTrue", "<#ftl autoEscaping=true>${'a&b'}");
        
        Configuration cfg = getConfiguration();
        
        assertTrue(cfg.getAutoEscaping());
        
        cfg.setOutputFormat(Configuration.XML_OUTPUT_FORMAT);
        
        for (boolean cfgAutoEscaping : new boolean[] { true, false }) {
            if (!cfgAutoEscaping) {
                cfg.setAutoEscaping(false);
            }
            
            {
                Template t = cfg.getTemplate("t");
                if (cfgAutoEscaping) {
                    assertTrue(t.getAutoEscaping());
                    assertOutput(t, "a&amp;b");
                } else {
                    assertFalse(t.getAutoEscaping());
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
                assertTrue(t.getAutoEscaping());
                assertOutput(t, "a&amp;b");
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
