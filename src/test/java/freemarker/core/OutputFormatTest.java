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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.OrMatcher;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
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
    
    @Test
    public void testNumericalInterpolation() throws IOException, TemplateException {
        getConfiguration().addOutputFormat(DummyOutputFormat.INSTANCE.getCommonName(), DummyOutputFormat.INSTANCE);
        assertOutput(
                "<#ftl outputFormat='dummy'>#{1.5}; #{1.5; m3}; ${'a.b'}",
                "1\\.5; 1\\.500; a\\.b");
        assertOutput(
                "<#ftl outputFormat='dummy' autoEscaping=false>#{1.5}; #{1.5; m3}; ${'a.b'}; ${'a.b'?esc}",
                "1.5; 1.500; a.b; a\\.b");
        assertOutput("<#ftl outputFormat='plainText'>#{1.5}", "1.5");
        assertOutput("<#ftl outputFormat='HTML'>#{1.5}", "1.5");
        assertOutput("#{1.5}", "1.5");
    }
    
    @Test
    public void testRawOutputFormat() throws IOException, TemplateException {
        assertOutput("${'a < b'}; ${htmlPlain}; ${htmlMarkup}", "a < b; a &lt; {h}; <p>c");
        assertErrorContains("${'x'?esc}", "raw", "escaping", "?esc");
        assertErrorContains("${'x'?noEsc}", "raw", "escaping", "?noEsc");
    }

    @Test
    public void testPlainTextOutputFormat() throws IOException, TemplateException {
        assertOutput("<#ftl outputFormat='plainText'>${'a < b'}; ${htmlPlain}", "a < b; a < {h}");
        assertErrorContains("<#ftl outputFormat='plainText'>${htmlMarkup}", "plainText", "HTML", "conversion");
        assertErrorContains("<#ftl outputFormat='plainText'>${'x'?esc}", "plainText", "escaping", "?esc");
        assertErrorContains("<#ftl outputFormat='plainText'>${'x'?noEsc}", "plainText", "escaping", "?noEsc");
    }
    
    @Test
    public void testAutoEscapingOnTOMs() throws IOException, TemplateException {
        for (int autoEsc = 0; autoEsc < 2; autoEsc++) {
            String commonAutoEscFtl = "<#ftl outputFormat='HTML'>${'&'}";
            if (autoEsc == 0) {
                // Cfg default is autoEscaping true
                assertOutput(commonAutoEscFtl, "&amp;");
            } else {
                getConfiguration().setAutoEscaping(false);
                assertOutput(commonAutoEscFtl, "&");
            }
            
            assertOutput(
                    "<#ftl outputFormat='RTF'>"
                    + "${rtfPlain} ${rtfMarkup} "
                    + "${htmlPlain} "
                    + "${xmlPlain}",
                    "\\\\par a & b \\par c "
                    + "a < \\{h\\} "
                    + "a < \\{x\\}");
            assertOutput(
                    "<#ftl outputFormat='HTML'>"
                    + "${htmlPlain} ${htmlMarkup} "
                    + "${xmlPlain} "
                    + "${rtfPlain}",
                    "a &lt; {h} <p>c "
                    + "a &lt; {x} "
                    + "\\par a &amp; b");
            assertOutput(
                    "<#ftl outputFormat='XML'>"
                    + "${xmlPlain} ${xmlMarkup} "
                    + "${htmlPlain} "
                    + "${rtfPlain}",
                    "a &lt; {x} <p>c</p> "
                    + "a &lt; {h} "
                    + "\\par a &amp; b");
            assertErrorContains("<#ftl outputFormat='RTF'>${htmlMarkup}", "output format", "RTF", "HTML");
            assertErrorContains("<#ftl outputFormat='RTF'>${xmlMarkup}", "output format", "RTF", "XML");
            assertErrorContains("<#ftl outputFormat='HTML'>${rtfMarkup}", "output format", "HTML", "RTF");
            assertErrorContains("<#ftl outputFormat='HTML'>${xmlMarkup}", "output format", "HTML", "XML");
            assertErrorContains("<#ftl outputFormat='XML'>${rtfMarkup}", "output format", "XML", "RTF");
            assertErrorContains("<#ftl outputFormat='XML'>${htmlMarkup}", "output format", "XML", "HTML");
            
            for (int hasHeader = 0; hasHeader < 2; hasHeader++) {
                assertOutput(
                        (hasHeader == 1 ? "<#ftl outputFormat='raw'>" : "")
                        + "${xmlPlain} ${xmlMarkup} "
                        + "${htmlPlain} ${htmlMarkup} "
                        + "${rtfPlain} ${rtfMarkup}",
                        "a &lt; {x} <p>c</p> "
                        + "a &lt; {h} <p>c "
                        + "\\\\par a & b \\par c");
            }
        }
    }

    @Test
    public void testStringLiteralsUseRawOF() throws IOException, TemplateException {
        String expectedOut = "&amp; (&) &amp;";
        String ftl = "<#ftl outputFormat='XML'>${'&'} ${\"(${'&'})\"?noEsc} ${'&'}";
        
        assertOutput(ftl, expectedOut);
        
        addTemplate("t.xml", ftl);
        assertOutputForNamed("t.xml", expectedOut);
    }
    
    @Test
    public void testUnparsedTemplate() throws IOException, TemplateException {
        String content = "<#ftl>a<#foo>b${x}";
        {
            Template t = Template.getPlainTextTemplate("x", content, getConfiguration());
            Writer sw = new StringWriter();
            t.process(null, sw);
            assertEquals(content, sw.toString());
            assertEquals(Configuration.RAW_OUTPUT_FORMAT, t.getOutputFormat());
        }
        
        {
            getConfiguration().setOutputFormat(Configuration.HTML_OUTPUT_FORMAT);
            Template t = Template.getPlainTextTemplate("x", content, getConfiguration());
            Writer sw = new StringWriter();
            t.process(null, sw);
            assertEquals(content, sw.toString());
            assertEquals(Configuration.HTML_OUTPUT_FORMAT, t.getOutputFormat());
        }
    }

    @Test
    public void testStringLiteralTemplateModificationBug() throws IOException, TemplateException {
        Template t = new Template(null, "<#ftl outputFormat='XML'>${'&'} ${\"(${'&'})\"?noEsc}", getConfiguration());
        assertEquals(Configuration.XML_OUTPUT_FORMAT, t.getOutputFormat());
    }
    
    @Test
    public void testEscAndNoEscBIBasics() throws IOException, TemplateException {
        String commonFTL = "${'<x>'} ${'<x>'?esc} ${'<x>'?noEsc}";
        addTemplate("t.ftlh", commonFTL);
        addTemplate("t-noAuto.ftlh", "<#ftl autoEscaping=false>" + commonFTL);
        addTemplate("t.ftl", commonFTL);
        assertOutputForNamed("t.ftlh", "&lt;x&gt; &lt;x&gt; <x>");
        assertOutputForNamed("t-noAuto.ftlh", "<x> &lt;x&gt; <x>");
        assertErrorContainsForNamed("t.ftl", "output format", "raw");
    }

    @Test
    public void testStringBIsFail() {
        assertErrorContains("<#ftl outputFormat='HTML'>${'<b>foo</b>'?esc?upperCase}", "string", "output_fragment");
    }

    @Test
    public void testEscAndNoEscBIsOnTOMs() throws IOException, TemplateException {
        assertOutput(
                "<#ftl outputFormat='XML'>${'&'?esc?esc} ${'&'?esc?noEsc} ${'&'?noEsc?esc} ${'&'?noEsc?noEsc}",
                "&amp; &amp; & &");
        
        for (String bi : new String[] { "esc", "noEsc" } ) {
            assertOutput(
                    "<#ftl outputFormat='XML'>${rtfPlain?" + bi + "}",
                    "\\par a &amp; b");
            assertErrorContains(
                    "<#ftl outputFormat='XML'>${rtfMarkup?" + bi + "}",
                    "?" + bi, "output format", "RTF", "XML");
        }
    }
    
    @Override
    protected Configuration createConfiguration() throws TemplateModelException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        
        TemplateConfigurer xmlTC = new TemplateConfigurer();
        xmlTC.setOutputFormat(Configuration.XML_OUTPUT_FORMAT);
        cfg.setTemplateConfigurers(
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.xml"), xmlTC));

        cfg.setSharedVariable("rtfPlain", RTFOutputFormat.INSTANCE.escapePlainText("\\par a & b"));
        cfg.setSharedVariable("rtfMarkup", RTFOutputFormat.INSTANCE.fromMarkup("\\par c"));
        cfg.setSharedVariable("htmlPlain", HTMLOutputFormat.INSTANCE.escapePlainText("a < {h}"));
        cfg.setSharedVariable("htmlMarkup", HTMLOutputFormat.INSTANCE.fromMarkup("<p>c"));
        cfg.setSharedVariable("xmlPlain", XMLOutputFormat.INSTANCE.escapePlainText("a < {x}"));
        cfg.setSharedVariable("xmlMarkup", XMLOutputFormat.INSTANCE.fromMarkup("<p>c</p>"));
        
        return cfg;
    }

    static class DummyTemplateOutputModel extends EscapingTemplateOutputModel<DummyTemplateOutputModel> {

        DummyTemplateOutputModel(String plainTextContent, String markupContet) {
            super(plainTextContent, markupContet);
        }

        @Override
        public OutputFormat<DummyTemplateOutputModel> getOutputFormat() {
            return DummyOutputFormat.INSTANCE;
        }
        
    }
    
    static class DummyOutputFormat extends EscapingOutputFormat<DummyTemplateOutputModel> {
        
        private static final DummyOutputFormat INSTANCE = new DummyOutputFormat();

        @Override
        protected String escapePlainTextToString(String plainTextContent) {
            return plainTextContent.replaceAll("(\\.|\\\\)", "\\\\$1");
        }

        @Override
        protected DummyTemplateOutputModel newTOM(String plainTextContent, String markupContent) {
            return new DummyTemplateOutputModel(plainTextContent, markupContent);
        }

        @Override
        public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
            out.write(escapePlainTextToString(textToEsc));
        }

        @Override
        public boolean isLegacyBuiltInBypassed(String builtInName) {
            return false;
        }

        @Override
        public String getCommonName() {
            return "dummy";
        }

        @Override
        public String getMimeType() {
            return "text/dummy";
        }
        
    }
    
}
