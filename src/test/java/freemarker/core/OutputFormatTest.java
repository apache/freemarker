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
import java.util.Collections;

import org.junit.Test;

import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.MergingTemplateConfigurerFactory;
import freemarker.cache.OrMatcher;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import freemarker.test.TemplateTest;

public class OutputFormatTest extends TemplateTest {

    @Test
    public void testOutputFormatSettingLayers() throws Exception {
        addTemplate("t", "${.outputFormat}");
        addTemplate("t.xml", "${.outputFormat}");
        addTemplate("tWithHeader", "<#ftl outputFormat='HTML'>${.outputFormat}");
        
        Configuration cfg = getConfiguration();
        for (OutputFormat cfgOutputFormat
                : new OutputFormat[] { UndefinedOutputFormat.INSTANCE, RTFOutputFormat.INSTANCE } ) {
            if (!cfgOutputFormat.equals(UndefinedOutputFormat.INSTANCE)) {
                cfg.setOutputFormat(cfgOutputFormat);
            }
            
            assertEquals(cfgOutputFormat, cfg.getOutputFormat());
            
            {
                Template t = cfg.getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = cfg.getTemplate("t.xml");
                assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = cfg.getTemplate("tWithHeader");
                assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
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
            final OutputFormat cfgOutputFormat;
            final OutputFormat ftlhOutputFormat;
            final OutputFormat ftlxOutputFormat;
            switch (setupNumber) {
            case 1:
                cfgOutputFormat = UndefinedOutputFormat.INSTANCE;
                ftlhOutputFormat = HTMLOutputFormat.INSTANCE;
                ftlxOutputFormat = XMLOutputFormat.INSTANCE;
                break;
            case 2:
                cfgOutputFormat = RTFOutputFormat.INSTANCE;
                cfg.setOutputFormat(cfgOutputFormat);
                ftlhOutputFormat = HTMLOutputFormat.INSTANCE;
                ftlxOutputFormat = XMLOutputFormat.INSTANCE;
                break;
            case 3:
                cfgOutputFormat = UndefinedOutputFormat.INSTANCE;
                cfg.unsetOutputFormat();
                TemplateConfigurer tcXml = new TemplateConfigurer();
                tcXml.setOutputFormat(XMLOutputFormat.INSTANCE);
                cfg.setTemplateConfigurers(
                        new ConditionalTemplateConfigurerFactory(
                                new OrMatcher(
                                        new FileNameGlobMatcher("*.ftlh"),
                                        new FileNameGlobMatcher("*.FTLH"),
                                        new FileNameGlobMatcher("*.fTlH")),
                                tcXml));
                ftlhOutputFormat = XMLOutputFormat.INSTANCE;
                ftlxOutputFormat = XMLOutputFormat.INSTANCE;
                break;
            case 4:
                cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
                cfgOutputFormat = UndefinedOutputFormat.INSTANCE;
                ftlhOutputFormat = XMLOutputFormat.INSTANCE;
                ftlxOutputFormat = UndefinedOutputFormat.INSTANCE;
                break;
            case 5:
                cfg.setTemplateConfigurers(null);
                cfgOutputFormat = UndefinedOutputFormat.INSTANCE;
                ftlhOutputFormat = UndefinedOutputFormat.INSTANCE;
                ftlxOutputFormat = UndefinedOutputFormat.INSTANCE;
                break;
            default:
                throw new AssertionError();
            }
            
            assertEquals(cfgOutputFormat, cfg.getOutputFormat());
            
            {
                Template t = cfg.getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = cfg.getTemplate("t.ftl");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            for (String name : new String[] { "t.ftlh", "t.FTLH", "t.fTlH" }) {
                Template t = cfg.getTemplate(name);
                assertEquals(ftlhOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            for (String name : new String[] { "t.ftlx", "t.FTLX", "t.fTlX" }) {
                Template t = cfg.getTemplate(name);
                assertEquals(ftlxOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }

            {
                Template t = cfg.getTemplate("tWithHeader.ftlx");
                assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            cfg.clearTemplateCache();
        }
    }
    
    @Test
    public void testStandardFileExtensionsSettingOverriding() throws Exception {
        addTemplate("t.ftlx",
                "${\"'\"} ${\"'\"?esc} ${\"'\"?noEsc}");
        addTemplate("t.ftl",
                "${'{}'} ${'{}'?esc} ${'{}'?noEsc}");
        
        TemplateConfigurer tcHTML = new TemplateConfigurer();
        tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
        ConditionalTemplateConfigurerFactory tcfHTML = new ConditionalTemplateConfigurerFactory(
                new FileNameGlobMatcher("t.*"), tcHTML);

        TemplateConfigurer tcNoAutoEsc = new TemplateConfigurer();
        tcNoAutoEsc.setAutoEscaping(false);
        ConditionalTemplateConfigurerFactory tcfNoAutoEsc = new ConditionalTemplateConfigurerFactory(
                new FileNameGlobMatcher("t.*"), tcNoAutoEsc);

        Configuration cfg = getConfiguration();
        
        for (int i = 0; i < 3; i++) {
            if (i == 1) {
                cfg.setTemplateConfigurers(null);
                cfg.setOutputFormat(RTFOutputFormat.INSTANCE);
                // Just to prove that the settings are as expected:
                assertOutputForNamed("t.ftl", "\\{\\} \\{\\} {}");
            } else if (i == 2) {
                cfg.setTemplateConfigurers(null);
                cfg.setAutoEscaping(false);
                // Just to prove that the settings are as expected:
                assertOutputForNamed("t.ftl", "{} \\{\\} {}");
            }
            
            assertOutputForNamed("t.ftlx", "&apos; &apos; '");
            
            cfg.setTemplateConfigurers(tcfHTML);
            assertOutputForNamed("t.ftlx", "&#39; &#39; '");
            
            cfg.setTemplateConfigurers(tcfNoAutoEsc);
            assertOutputForNamed("t.ftlx", "' &apos; '");
            
            cfg.setTemplateConfigurers(
                    new MergingTemplateConfigurerFactory(tcfHTML, tcfNoAutoEsc));
            assertOutputForNamed("t.ftlx", "' &#39; '");
        }
    }
    
    @Test
    public void testStandardFileExtensionsFormatterImplOverriding() throws Exception {
        addTemplate("t.ftlh", "${'a&x'}");
        assertOutputForNamed("t.ftlh", "a&amp;x");
        getConfiguration().setRegisteredCustomOutputFormats(Collections.singleton(CustomHTMLOutputFormat.INSTANCE));
        assertOutputForNamed("t.ftlh", "a&amp;X");
        getConfiguration().setRegisteredCustomOutputFormats(Collections.<OutputFormat>emptyList());
        assertOutputForNamed("t.ftlh", "a&amp;x");
    }
    
    @Test
    public void testAutoEscapingSettingLayers() throws Exception {
        addTemplate("t", "${'a&b'}");
        addTemplate("tWithHeaderFalse", "<#ftl autoEsc=false>${'a&b'}");
        addTemplate("tWithHeaderTrue", "<#ftl autoEsc=true>${'a&b'}");
        
        Configuration cfg = getConfiguration();
        
        assertTrue(cfg.getAutoEscaping());
        
        cfg.setOutputFormat(XMLOutputFormat.INSTANCE);
        
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
        getConfiguration().setRegisteredCustomOutputFormats(Collections.singleton(DummyOutputFormat.INSTANCE));
        assertOutput(
                "<#ftl outputFormat='dummy'>#{1.5}; #{1.5; m3}; ${'a.b'}",
                "1\\.5; 1\\.500; a\\.b");
        assertOutput(
                "<#ftl outputFormat='dummy' autoEsc=false>#{1.5}; #{1.5; m3}; ${'a.b'}; ${'a.b'?esc}",
                "1.5; 1.500; a.b; a\\.b");
        assertOutput("<#ftl outputFormat='plainText'>#{1.5}", "1.5");
        assertOutput("<#ftl outputFormat='HTML'>#{1.5}", "1.5");
        assertOutput("#{1.5}", "1.5");
    }
    
    @Test
    public void testUndefinedOutputFormat() throws IOException, TemplateException {
        assertOutput("${'a < b'}; ${htmlPlain}; ${htmlMarkup}", "a < b; a &lt; {h&#39;}; <p>c");
        assertErrorContains("${'x'?esc}", "undefined", "escaping", "?esc");
        assertErrorContains("${'x'?noEsc}", "undefined", "escaping", "?noEsc");
    }

    @Test
    public void testPlainTextOutputFormat() throws IOException, TemplateException {
        assertOutput("<#ftl outputFormat='plainText'>${'a < b'}; ${htmlPlain}", "a < b; a < {h'}");
        assertErrorContains("<#ftl outputFormat='plainText'>${htmlMarkup}", "plainText", "HTML", "conversion");
        assertErrorContains("<#ftl outputFormat='plainText'>${'x'?esc}", "plainText", "escaping", "?esc");
        assertErrorContains("<#ftl outputFormat='plainText'>${'x'?noEsc}", "plainText", "escaping", "?noEsc");
    }
    
    @Test
    public void testAutoEscapingOnMOs() throws IOException, TemplateException {
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
                    + "a < \\{h'\\} "
                    + "a < \\{x'\\}");
            assertOutput(
                    "<#ftl outputFormat='HTML'>"
                    + "${htmlPlain} ${htmlMarkup} "
                    + "${xmlPlain} "
                    + "${rtfPlain}",
                    "a &lt; {h&#39;} <p>c "
                    + "a &lt; {x&#39;} "
                    + "\\par a &amp; b");
            assertOutput(
                    "<#ftl outputFormat='XML'>"
                    + "${xmlPlain} ${xmlMarkup} "
                    + "${htmlPlain} "
                    + "${rtfPlain}",
                    "a &lt; {x&apos;} <p>c</p> "
                    + "a &lt; {h&apos;} "
                    + "\\par a &amp; b");
            assertErrorContains("<#ftl outputFormat='RTF'>${htmlMarkup}", "output format", "RTF", "HTML");
            assertErrorContains("<#ftl outputFormat='RTF'>${xmlMarkup}", "output format", "RTF", "XML");
            assertErrorContains("<#ftl outputFormat='HTML'>${rtfMarkup}", "output format", "HTML", "RTF");
            assertErrorContains("<#ftl outputFormat='HTML'>${xmlMarkup}", "output format", "HTML", "XML");
            assertErrorContains("<#ftl outputFormat='XML'>${rtfMarkup}", "output format", "XML", "RTF");
            assertErrorContains("<#ftl outputFormat='XML'>${htmlMarkup}", "output format", "XML", "HTML");
            
            for (int hasHeader = 0; hasHeader < 2; hasHeader++) {
                assertOutput(
                        (hasHeader == 1 ? "<#ftl outputFormat='undefined'>" : "")
                        + "${xmlPlain} ${xmlMarkup} "
                        + "${htmlPlain} ${htmlMarkup} "
                        + "${rtfPlain} ${rtfMarkup}",
                        "a &lt; {x&apos;} <p>c</p> "
                        + "a &lt; {h&#39;} <p>c "
                        + "\\\\par a & b \\par c");
            }
        }
    }

    @Test
    public void testStringLiteralsUseUndefinedOF() throws IOException, TemplateException {
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
            assertEquals(UndefinedOutputFormat.INSTANCE, t.getOutputFormat());
        }
        
        {
            getConfiguration().setOutputFormat(HTMLOutputFormat.INSTANCE);
            Template t = Template.getPlainTextTemplate("x", content, getConfiguration());
            Writer sw = new StringWriter();
            t.process(null, sw);
            assertEquals(content, sw.toString());
            assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
        }
    }

    @Test
    public void testStringLiteralTemplateModificationBug() throws IOException, TemplateException {
        Template t = new Template(null, "<#ftl outputFormat='XML'>${'&'} ${\"(${'&'})\"?noEsc}", getConfiguration());
        assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
        assertOutput("${.outputFormat} ${'${.outputFormat}'} ${.outputFormat}", "undefined plainText undefined");
        assertOutput("${'foo ${xmlPlain}'}", "foo a < {x'}");
        assertErrorContains("${'${xmlMarkup}'}", "plainText", "XML", "conversion");
    }
    
    @Test
    public void testStringBIsFail() {
        assertErrorContains("<#ftl outputFormat='HTML'>${'<b>foo</b>'?esc?upperCase}", "string", "markup_output");
    }

    @Test
    public void testConcatWithMOs() throws IOException, TemplateException {
        assertOutput(
                "${'\\'' + htmlMarkup} ${htmlMarkup + '\\''} ${htmlMarkup + htmlMarkup}",
                "&#39;<p>c <p>c&#39; <p>c<p>c");
        assertOutput(
                "${'\\'' + htmlPlain} ${htmlPlain + '\\''} ${htmlPlain + htmlPlain}",
                "&#39;a &lt; {h&#39;} a &lt; {h&#39;}&#39; a &lt; {h&#39;}a &lt; {h&#39;}");
        assertErrorContains(
                "<#ftl outputFormat='XML'>${'\\'' + htmlMarkup}",
                "HTML", "XML", "conversion");
        assertErrorContains(
                "${xmlMarkup + htmlMarkup}",
                "HTML", "XML", "Conversion", "common");
        assertOutput(
                "${xmlMarkup + htmlPlain}",
                "<p>c</p>a &lt; {h&apos;}");
        assertOutput(
                "${xmlPlain + htmlMarkup}",
                "a &lt; {x&#39;}<p>c");
        assertOutput(
                "${xmlPlain + htmlPlain}",
                "a &lt; {x&apos;}a &lt; {h&apos;}");
        assertOutput(
                "${xmlPlain + htmlPlain + '\\''}",
                "a &lt; {x&apos;}a &lt; {h&apos;}&apos;");
        assertOutput(
                "${htmlPlain + xmlPlain + '\\''}",
                "a &lt; {h&#39;}a &lt; {x&#39;}&#39;");
        assertOutput(
                "${xmlPlain + htmlPlain + '\\''}",
                "a &lt; {x&apos;}a &lt; {h&apos;}&apos;");
        assertOutput(
                "<#ftl outputFormat='XML'>${htmlPlain + xmlPlain + '\\''}",
                "a &lt; {h&apos;}a &lt; {x&apos;}&apos;");
        assertOutput(
                "<#ftl outputFormat='RTF'>${htmlPlain + xmlPlain + '\\''}",
                "a < \\{h'\\}a < \\{x'\\}'");
        assertOutput(
                "<#ftl outputFormat='XML'>${'\\'' + htmlPlain}",
                "&apos;a &lt; {h&apos;}");
        assertOutput(
                "<#ftl outputFormat='HTML'>${'\\'' + htmlPlain}",
                "&#39;a &lt; {h&#39;}");
        assertOutput(
                "<#ftl outputFormat='HTML'>${'\\'' + xmlPlain}",
                "&#39;a &lt; {x&#39;}");
        assertOutput(
                "<#ftl outputFormat='RTF'>${'\\'' + xmlPlain}",
                "'a < \\{x'\\}");
        
        assertOutput(
                "<#assign x = '\\''><#assign x += xmlMarkup>${x}",
                "&apos;<p>c</p>");
        assertOutput(
                "<#assign x = xmlMarkup><#assign x += '\\''>${x}",
                "<p>c</p>&apos;");
        assertOutput(
                "<#assign x = xmlMarkup><#assign x += htmlPlain>${x}",
                "<p>c</p>a &lt; {h&apos;}");
        assertErrorContains(
                "<#assign x = xmlMarkup><#assign x += htmlMarkup>${x}",
                "HTML", "XML", "Conversion", "common");
    }
    
    @Test
    public void testBlockAssignment() throws Exception {
        for (String d : new String[] { "assign", "global", "local" }) {
            String commonFTL =
                    "<#macro m>"
                    + "<#" + d + " x><p>${'&'}</#" + d + ">${x?isString?c} ${x} ${'&'} "
                    + "<#" + d + " x></#" + d + ">${x?isString?c}"
                    + "</#macro><@m />";
            assertOutput(
                    commonFTL,
                    "true <p>& & true");
            assertOutput(
                    "<#ftl outputFormat='HTML'>" + commonFTL,
                    "false <p>&amp; &amp; false");
        }
    }

    @Test
    public void testSpecialVariables() throws Exception {
        String commonFTL = "${.outputFormat} ${.autoEsc?c}";
        
        addTemplate("t.ftlx", commonFTL);
        assertOutputForNamed("t.ftlx", "XML true");
        
        addTemplate("t.ftlh", commonFTL);
        assertOutputForNamed("t.ftlh", "HTML true");

        addTemplate("t.ftl", commonFTL);
        assertOutputForNamed("t.ftl", "undefined true");
        
        addTemplate("tX.ftl", "<#ftl outputFormat='XML'>" + commonFTL);
        addTemplate("tX.ftlx", commonFTL);
        assertOutputForNamed("t.ftlx", "XML true");
        
        addTemplate("tN.ftl", "<#ftl outputFormat='RTF' autoEsc=false>" + commonFTL);
        assertOutputForNamed("tN.ftl", "RTF false");
        
        assertOutput("${.output_format} ${.auto_esc?c}", "undefined true");
    }
    
    @Test
    public void testEscAndNoEscBIBasics() throws IOException, TemplateException {
        String commonFTL = "${'<x>'} ${'<x>'?esc} ${'<x>'?noEsc}";
        addTemplate("t.ftlh", commonFTL);
        addTemplate("t-noAuto.ftlh", "<#ftl autoEsc=false>" + commonFTL);
        addTemplate("t.ftl", commonFTL);
        assertOutputForNamed("t.ftlh", "&lt;x&gt; &lt;x&gt; <x>");
        assertOutputForNamed("t-noAuto.ftlh", "<x> &lt;x&gt; <x>");
        assertErrorContainsForNamed("t.ftl", "output format", "undefined");
    }

    @Test
    public void testEscAndNoEscBIsOnMOs() throws IOException, TemplateException {
        String xmlHdr = "<#ftl outputFormat='XML'>";
        
        assertOutput(
                xmlHdr + "${'&'?esc?esc} ${'&'?esc?noEsc} ${'&'?noEsc?esc} ${'&'?noEsc?noEsc}",
                "&amp; &amp; & &");
        
        for (String bi : new String[] { "esc", "noEsc" } ) {
            assertOutput(
                    xmlHdr + "${rtfPlain?" + bi + "}",
                    "\\par a &amp; b");
            assertOutput(
                    xmlHdr + "<#setting numberFormat='0.0'>${1?" + bi + "}",
                    "1.0");
            assertOutput(
                    xmlHdr + "<#setting booleanFormat='&y,&n'>${true?" + bi + "}",
                    bi.equals("esc") ? "&amp;y" : "&y");
            assertErrorContains(
                    xmlHdr + "${rtfMarkup?" + bi + "}",
                    "?" + bi, "output format", "RTF", "XML");
            assertErrorContains(
                    xmlHdr + "${noSuchVar?" + bi + "}",
                    "noSuchVar", "null or missing");
            assertErrorContains(
                    xmlHdr + "${[]?" + bi + "}",
                    "?" + bi, "xpected", "string", "sequence");
        }
    }

    @Test
    public void testMarkupBI() throws Exception {
        assertOutput(
                "${htmlPlain?markup} ${htmlMarkup?markup}",
                "a &lt; {h&#39;} <p>c");
        assertErrorContains(
                "${noSuchVar?markup}",
                "noSuchVar", "null or missing");
        assertErrorContains(
                "${'x'?markup}",
                "xpected", "markup output", "string");
    }

    @Test
    public void testOutputFormatDirective() throws Exception {
        assertOutput(
                "${.outputFormat}${'\\''} "
                + "<#outputFormat 'HTML'>"
                + "${.outputFormat}${'\\''} "
                + "<#outputFormat 'XML'>${.outputFormat}${'\\''}</#outputFormat> "
                + "${.outputFormat}${'\\''} "
                + "</#outputFormat>"
                + "${.outputFormat}${'\\''}",
                "undefined' HTML&#39; XML&apos; HTML&#39; undefined'");
        assertOutput(
                "<#ftl output_format='XML'>"
                + "${.output_format}${'\\''} "
                + "<#outputformat 'HTML'>${.output_format}${'\\''}</#outputformat> "
                + "${.output_format}${'\\''}",
                "XML&apos; HTML&#39; XML&apos;");
        
        // Custom format:
        assertErrorContains(
                "<#outputFormat 'dummy'></#outputFormat>",
                "dummy", "nregistered");
        getConfiguration().setRegisteredCustomOutputFormats(Collections.singleton(DummyOutputFormat.INSTANCE));
        assertOutput(
                "<#outputFormat 'dummy'>${.outputFormat}</#outputFormat>",
                "dummy");
        
        // Parse-time param expression:
        assertOutput(
                "<#outputFormat 'plain' + 'Text'>${.outputFormat}</#outputFormat>",
                "plainText");
        assertErrorContains(
                "<#outputFormat 'plain' + someVar + 'Text'></#outputFormat>",
                "someVar", "parse-time");
        assertErrorContains(
                "<#outputFormat 'plainText'?upperCase></#outputFormat>",
                "?upperCase", "parse-time");
        assertErrorContains(
                "<#outputFormat true></#outputFormat>",
                "string", "boolean");
        
        // Naming convention:
        assertErrorContains(
                "<#outputFormat 'HTML'></#outputformat>",
                "convention", "#outputFormat", "#outputformat");
        assertErrorContains(
                "<#outputformat 'HTML'></#outputFormat>",
                "convention", "#outputFormat", "#outputformat");
        
        // Empty block:
        assertOutput(
                "${.output_format} "
                + "<#outputformat 'HTML'></#outputformat>"
                + "${.output_format}",
                "undefined undefined");
        
        // WS stripping:
        assertOutput(
                "${.output_format}\n"
                + "<#outputformat 'HTML'>\n"
                + "  x\n"
                + "</#outputformat>\n"
                + "${.output_format}",
                "undefined\n  x\nundefined");
    }

    @Test
    public void testAutoEscAndNoAutoEscDirectives() throws Exception {
        assertOutput(
                "<#ftl outputFormat='XML'>"
                + "${.autoEsc?c}${'&'} "
                + "<#noAutoEsc>"
                + "${.autoEsc?c}${'&'} "
                + "<#autoEsc>${.autoEsc?c}${'&'}</#autoEsc> "
                + "${.autoEsc?c}${'&'} "
                + "</#noAutoEsc>"
                + "${.autoEsc?c}${'&'}",
                "true&amp; false& true&amp; false& true&amp;");
        assertOutput(
                "<#ftl auto_esc=false output_format='XML'>"
                + "${.auto_esc?c}${'&'} "
                + "<#autoesc>${.auto_esc?c}${'&'}</#autoesc> "
                + "${.auto_esc?c}${'&'}",
                "false& true&amp; false&");
        
        // Bad came case:
        assertErrorContains(
                "<#noAutoesc></#noAutoesc>",
                "Unknown directive");
        assertErrorContains(
                "<#noautoEsc></#noautoEsc>",
                "Unknown directive");
        
        // Empty block:
        assertOutput(
                "${.auto_esc?c} "
                + "<#noautoesc></#noautoesc>"
                + "${.auto_esc?c}",
                "true true");
        
        // WS stripping:
        assertOutput(
                "${.auto_esc?c}\n"
                + "<#noautoesc>\n"
                + "  x\n"
                + "</#noautoesc>\n"
                + "${.auto_esc?c}",
                "true\n  x\ntrue");
        
        
        // Naming convention:
        getConfiguration().setOutputFormat(XMLOutputFormat.INSTANCE);
        assertErrorContains(
                "<#autoEsc></#autoesc>",
                "convention", "#autoEsc", "#autoesc");
        assertErrorContains(
                "<#autoesc></#autoEsc>",
                "convention", "#autoEsc", "#autoesc");
        assertErrorContains(
                "<#noAutoEsc></#noautoesc>",
                "convention", "#noAutoEsc", "#noautoesc");
        assertErrorContains(
                "<#noautoesc></#noAutoEsc>",
                "convention", "#noAutoEsc", "#noautoesc");
    }
    
    @Test
    public void testExplicitAutoEscBannedForNonMarkup() throws Exception {
        // While this restriction is technically unnecessary, we can catch a dangerous and probably common user
        // misunderstanding.
        assertErrorContains("<#ftl autoEsc=true>", "can't do escaping", "undefined");
        assertErrorContains("<#ftl outputFormat='plainText' autoEsc=true>", "can't do escaping", "plainText");
        assertErrorContains("<#ftl autoEsc=true outputFormat='plainText'>", "can't do escaping", "plainText");
        assertOutput("<#ftl autoEsc=true outputFormat='HTML'>", "");
        assertOutput("<#ftl outputFormat='HTML' autoEsc=true>", "");
        assertOutput("<#ftl autoEsc=false>", "");
        
        assertErrorContains("<#autoEsc></#autoEsc>", "can't do escaping", "undefined");
        assertErrorContains("<#ftl outputFormat='plainText'><#autoEsc></#autoEsc>", "can't do escaping", "plainText");
        assertOutput("<#ftl outputFormat='plainText'><#outputFormat 'XML'><#autoEsc></#autoEsc></#outputFormat>", "");
        assertOutput("<#ftl outputFormat='HTML'><#autoEsc></#autoEsc>", "");
        assertOutput("<#noAutoEsc></#noAutoEsc>", "");
    }
    
    @Test
    public void testDynamicParsingBIsInherticContextOutputFormat() throws Exception {
        // Dynamic parser BI-s are supposed to use the parserConfiguration of the calling template, and ignore anything
        // inside the calling template itself. Except, the outputFormat has to come from the calling lexical context.
        
        String commonFTL
                = "Eval: ${'.outputFormat'?eval}; "
                  + "Interpret: <#assign ipd = r\"${.outputFormat} ${'{&}'}\"?interpret><@ipd/>";
        addTemplate("t.ftlh", commonFTL);
        addTemplate("t2.ftlh", "<#outputFormat 'RTF'>" + commonFTL + "</#outputFormat>");
        
        assertOutputForNamed(
                "t.ftlh",
                "Eval: HTML; Interpret: HTML {&amp;}");
        assertOutputForNamed(
                "t2.ftlh",
                "Eval: RTF; Interpret: RTF \\{&\\}");
        assertOutput(
                commonFTL,
                "Eval: undefined; Interpret: undefined {&}");
        assertOutput(
                "<#ftl outputFormat='RTF'>" + commonFTL + "\n"
                + "<#outputFormat 'XML'>" + commonFTL + "</#outputFormat>",
                "Eval: RTF; Interpret: RTF \\{&\\}\n"
                + "Eval: XML; Interpret: XML {&amp;}");
        assertOutput(
                "<#ftl autoEsc=false outputFormat='XML'>"
                + "<#noAutoEsc>" + commonFTL + " ${'.autoEsc'?eval?c}</#noAutoEsc>",
                "Eval: XML; Interpret: XML {&amp;} true");
    }

    @Test
    public void testBannedBIsWhenAutoEscaping() throws Exception {
        for (String biName : new String[] { "html", "xhtml", "rtf", "xml" }) {
            for (Version ici : new Version[] { Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_24 }) {
                getConfiguration().setIncompatibleImprovements(ici);
                
                String commonFTL = "${'x'?" + biName + "}";
                assertOutput(commonFTL, "x");
                assertErrorContains("<#ftl outputFormat='HTML'>" + commonFTL,
                        "?" + biName, "HTML", "double-escaping");
                assertOutput("<#ftl outputFormat='plainText'>" + commonFTL, "x");
                assertOutput("<#ftl outputFormat='HTML' autoEsc=false>" + commonFTL, "x");
                assertOutput("<#ftl outputFormat='HTML'><#noAutoEsc>" + commonFTL + "</#noAutoEsc>", "x");
                assertOutput("<#ftl outputFormat='HTML'><#outputFormat 'plainText'>" + commonFTL + "</#outputFormat>", "x");
                assertOutput("<#ftl outputFormat='HTML'>${'${\"x\"?" + biName + "}'}", "x");
            }
        }
    }

    @Test
    public void testLegacyEscaperBIsBypassMOs() throws Exception {
        assertOutput("${htmlPlain?html} ${htmlMarkup?html}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${xmlPlain?html}", "?html", "string", "markup_output", "XML");
        assertErrorContains("${xmlMarkup?html}", "?html", "string", "markup_output", "XML");
        assertErrorContains("${rtfPlain?html}", "?html", "string", "markup_output", "RTF");
        assertErrorContains("${rtfMarkup?html}", "?html", "string", "markup_output", "RTF");

        assertOutput("${htmlPlain?xhtml} ${htmlMarkup?xhtml}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${xmlPlain?xhtml}", "?xhtml", "string", "markup_output", "XML");
        assertErrorContains("${xmlMarkup?xhtml}", "?xhtml", "string", "markup_output", "XML");
        assertErrorContains("${rtfPlain?xhtml}", "?xhtml", "string", "markup_output", "RTF");
        assertErrorContains("${rtfMarkup?xhtml}", "?xhtml", "string", "markup_output", "RTF");
        
        assertOutput("${xmlPlain?xml} ${xmlMarkup?xml}", "a &lt; {x&apos;} <p>c</p>");
        assertOutput("${htmlPlain?xml} ${htmlMarkup?xml}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${rtfPlain?xml}", "?xml", "string", "markup_output", "RTF");
        assertErrorContains("${rtfMarkup?xml}", "?xml", "string", "markup_output", "RTF");
        
        assertOutput("${rtfPlain?rtf} ${rtfMarkup?rtf}", "\\\\par a & b \\par c");
        assertErrorContains("${xmlPlain?rtf}", "?rtf", "string", "markup_output", "XML");
        assertErrorContains("${xmlMarkup?rtf}", "?rtf", "string", "markup_output", "XML");
        assertErrorContains("${htmlPlain?rtf}", "?rtf", "string", "markup_output", "HTML");
        assertErrorContains("${htmlMarkup?rtf}", "?rtf", "string", "markup_output", "HTML");
    }
    
    @Test
    public void testBannedDirectivesIsWhenAutoEscaping() throws Exception {
        String commonFTL = "<#escape x as x?html>x</#escape>";
        assertOutput(commonFTL, "x");
        assertErrorContains("<#ftl outputFormat='HTML'>" + commonFTL, "escape", "HTML", "double-escaping");
        assertOutput("<#ftl outputFormat='plainText'>" + commonFTL, "x");
        assertOutput("<#ftl outputFormat='HTML' autoEsc=false>" + commonFTL, "x");
        assertOutput("<#ftl outputFormat='HTML'><#noAutoEsc>" + commonFTL + "</#noAutoEsc>", "x");
        assertOutput("<#ftl outputFormat='HTML'><#outputFormat 'plainText'>" + commonFTL + "</#outputFormat>", "x");
    }
    
    @Test
    public void testCombinedOutputFormats() throws Exception {
        assertOutput(
                "<#outputFormat 'XML{HTML}'>${'\\''}</#outputFormat>",
                "&amp;#39;");
        assertOutput(
                "<#outputFormat 'HTML{RTF{XML}}'>${'<a=\\'{}\\' />'}</#outputFormat>",
                "&amp;lt;a=&amp;apos;\\{\\}&amp;apos; /&amp;gt;");
        
        String commonFtl = "${'\\''} <#outputFormat '{HTML}'>${'\\''}</#outputFormat>";
        String commonOutput = "&apos; &amp;#39;";
        assertOutput(
                "<#outputFormat 'XML'>" + commonFtl + "</#outputFormat>",
                commonOutput);
        assertOutput(
                "<#ftl outputFormat='XML'>" + commonFtl,
                commonOutput);
        addTemplate("t.ftlx", commonFtl);
        assertOutputForNamed(
                "t.ftlx",
                commonOutput);
        
        assertErrorContains(
                commonFtl,
                ParseException.class, "{...}", "markup", UndefinedOutputFormat.INSTANCE.getName());
        assertErrorContains(
                "<#ftl outputFormat='plainText'>" + commonFtl,
                ParseException.class, "{...}", "markup", PlainTextOutputFormat.INSTANCE.getName());
        assertErrorContains(
                "<#ftl outputFormat='RTF'><#outputFormat '{plainText}'></#outputFormat>",
                ParseException.class, "{...}", "markup", PlainTextOutputFormat.INSTANCE.getName());
        assertErrorContains(
                "<#ftl outputFormat='RTF'><#outputFormat '{noSuchFormat}'></#outputFormat>",
                ParseException.class, "noSuchFormat", "registered");
        assertErrorContains(
                "<#outputFormat 'noSuchFormat{HTML}'></#outputFormat>",
                ParseException.class, "noSuchFormat", "registered");
        assertErrorContains(
                "<#outputFormat 'HTML{noSuchFormat}'></#outputFormat>",
                ParseException.class, "noSuchFormat", "registered");
    }
    
    @Override
    protected Configuration createConfiguration() throws TemplateModelException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_24);
        
        TemplateConfigurer xmlTC = new TemplateConfigurer();
        xmlTC.setOutputFormat(XMLOutputFormat.INSTANCE);
        cfg.setTemplateConfigurers(
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.xml"), xmlTC));

        cfg.setSharedVariable("rtfPlain", RTFOutputFormat.INSTANCE.fromPlainTextByEscaping("\\par a & b"));
        cfg.setSharedVariable("rtfMarkup", RTFOutputFormat.INSTANCE.fromMarkup("\\par c"));
        cfg.setSharedVariable("htmlPlain", HTMLOutputFormat.INSTANCE.fromPlainTextByEscaping("a < {h'}"));
        cfg.setSharedVariable("htmlMarkup", HTMLOutputFormat.INSTANCE.fromMarkup("<p>c"));
        cfg.setSharedVariable("xmlPlain", XMLOutputFormat.INSTANCE.fromPlainTextByEscaping("a < {x'}"));
        cfg.setSharedVariable("xmlMarkup", XMLOutputFormat.INSTANCE.fromMarkup("<p>c</p>"));
        
        return cfg;
    }
    
}
