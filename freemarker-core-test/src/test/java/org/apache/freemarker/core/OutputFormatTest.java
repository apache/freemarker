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
package org.apache.freemarker.core;

import com.google.common.collect.ImmutableList;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.*;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.OrMatcher;
import org.apache.freemarker.core.templateresolver.impl.NullCacheStorage;
import org.apache.freemarker.core.userpkg.CustomHTMLOutputFormat;
import org.apache.freemarker.core.userpkg.DummyOutputFormat;
import org.apache.freemarker.core.userpkg.SeldomEscapedOutputFormat;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.function.Consumer;

import static org.apache.freemarker.core.AutoEscapingPolicy.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class OutputFormatTest extends TemplateTest {

    @Test
    public void testOutputFormatSettingLayers() throws Exception {
        addTemplate("t", "${.outputFormat}");
        addTemplate("t.xml", "${.outputFormat}");
        addTemplate("tWithHeader", "<#ftl outputFormat='HTML'>${.outputFormat}");
        
        for (OutputFormat cfgOutputFormat
                : new OutputFormat[] { UndefinedOutputFormat.INSTANCE, RTFOutputFormat.INSTANCE } ) {
            Configuration.ExtendableBuilder<?> cfgB = newConfigurationBuilder();
            if (!cfgOutputFormat.equals(UndefinedOutputFormat.INSTANCE)) {
                cfgB.setOutputFormat(cfgOutputFormat);
            }
            setConfiguration(cfgB);

            assertEquals(cfgOutputFormat, getConfiguration().getOutputFormat());
            
            {
                Template t = getConfiguration().getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = getConfiguration().getTemplate("t.xml");
                assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = getConfiguration().getTemplate("tWithHeader");
                assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            getConfiguration().clearTemplateCache();
        }
    }
    
    @Test
    public void testStandardFileExtensions() throws Exception {
        String commonContent = "${.outputFormat}";
        addTemplate("t", commonContent);
        addTemplate("t.f3ac", commonContent);
        addTemplate("t.f3ah", commonContent);
        addTemplate("t.F3AH", commonContent);
        addTemplate("t.f3aH", commonContent);
        addTemplate("t.f3ax", commonContent);
        addTemplate("t.F3AX", commonContent);
        addTemplate("t.f3aX", commonContent);
        addTemplate("tWithHeader.f3ax", "<#ftl outputFormat='HTML'>" + commonContent);
        
        for (int setupNumber = 1; setupNumber <= 3; setupNumber++) {
            Configuration.ExtendableBuilder<?> cfgB = newConfigurationBuilder();
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
                cfgB.setOutputFormat(cfgOutputFormat);
                ftlhOutputFormat = HTMLOutputFormat.INSTANCE;
                ftlxOutputFormat = XMLOutputFormat.INSTANCE;
                break;
            case 3:
                cfgOutputFormat = UndefinedOutputFormat.INSTANCE;
                cfgB.unsetOutputFormat();
                TemplateConfiguration.Builder tcbXML = new TemplateConfiguration.Builder();
                tcbXML.setOutputFormat(XMLOutputFormat.INSTANCE);
                cfgB.setTemplateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new OrMatcher(
                                        new FileNameGlobMatcher("*.f3ah"),
                                        new FileNameGlobMatcher("*.F3AH"),
                                        new FileNameGlobMatcher("*.f3aH")),
                                tcbXML.build()));
                ftlhOutputFormat = HTMLOutputFormat.INSTANCE; // can't be overidden
                ftlxOutputFormat = XMLOutputFormat.INSTANCE;
                break;
            default:
                throw new AssertionError();
            }

            setConfiguration(cfgB);
            assertEquals(cfgOutputFormat, getConfiguration().getOutputFormat());
            
            {
                Template t = getConfiguration().getTemplate("t");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            {
                Template t = getConfiguration().getTemplate("t.f3ac");
                assertEquals(cfgOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            for (String name : new String[] { "t.f3ah", "t.F3AH", "t.f3aH" }) {
                Template t = getConfiguration().getTemplate(name);
                assertEquals(ftlhOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }
            
            for (String name : new String[] { "t.f3ax", "t.F3AX", "t.f3aX" }) {
                Template t = getConfiguration().getTemplate(name);
                assertEquals(ftlxOutputFormat, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }

            {
                Template t = getConfiguration().getTemplate("tWithHeader.f3ax");
                assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
                assertOutput(t, t.getOutputFormat().getName());
            }

            getConfiguration().clearTemplateCache();
        }
    }
    
    @Test
    public void testStandardFileExtensionsSettingOverriding() throws Exception {
        addTemplate("t.f3ax",
                "${\"'\"} ${\"'\"?esc} ${\"'\"?noEsc}");
        addTemplate("t.f3ah",
                "${'{}'} ${'{}'?esc} ${'{}'?noEsc}");
        
        ConditionalTemplateConfigurationFactory tcfHTML = new ConditionalTemplateConfigurationFactory(
                new FileNameGlobMatcher("t.*"),
                new TemplateConfiguration.Builder()
                        .outputFormat(HTMLOutputFormat.INSTANCE)
                        .build());

        ConditionalTemplateConfigurationFactory tcfNoAutoEsc = new ConditionalTemplateConfigurationFactory(
                new FileNameGlobMatcher("t.*"),
                new TemplateConfiguration.Builder()
                        .autoEscapingPolicy(DISABLE)
                        .build());

        {
            setConfiguration(newConfigurationBuilder().outputFormat(HTMLOutputFormat.INSTANCE));
            assertOutputForNamed("t.f3ax", "&apos; &apos; '");  // Can't override it
            setConfiguration(newConfigurationBuilder().templateConfigurations(tcfHTML));
            assertOutputForNamed("t.f3ax", "&apos; &apos; '");  // Can't override it
            setConfiguration(newConfigurationBuilder().templateConfigurations(tcfNoAutoEsc));
            assertOutputForNamed("t.f3ax", "&apos; &apos; '");  // Can't override it
        }

        {
            setConfiguration(newConfigurationBuilder().recognizeStandardFileExtensions(false));
            assertErrorContainsForNamed("t.f3ax", UndefinedOutputFormat.INSTANCE.getName());

            setConfiguration(newConfigurationBuilder()
                    .recognizeStandardFileExtensions(false)
                    .outputFormat(HTMLOutputFormat.INSTANCE));
            assertOutputForNamed("t.f3ax", "&#39; &#39; '");

            setConfiguration(newConfigurationBuilder()
                    .recognizeStandardFileExtensions(false)
                    .outputFormat(XMLOutputFormat.INSTANCE));
            assertOutputForNamed("t.f3ax", "&apos; &apos; '");

            setConfiguration(newConfigurationBuilder()
                    .recognizeStandardFileExtensions(false)
                    .templateConfigurations(tcfHTML));
            assertOutputForNamed("t.f3ax", "&#39; &#39; '");

            setConfiguration(newConfigurationBuilder()
                    .recognizeStandardFileExtensions(false)
                    .templateConfigurations(tcfNoAutoEsc)
                    .outputFormat(XMLOutputFormat.INSTANCE));
            assertOutputForNamed("t.f3ax", "' &apos; '");
        }
    }

    @Test
    public void testStandardFileExtensionsWithConstructor() throws Exception {
        Configuration cfg = getConfiguration();
        String commonFTL = "${'\\''}";
        for (String ext : new String[] { "f3au", "f3ac" } ){
            Template t = new Template("foo." + ext, commonFTL, cfg);
            assertSame(UndefinedOutputFormat.INSTANCE, t.getOutputFormat());
            StringWriter out = new StringWriter();
            t.process(null, out);
            assertEquals("'", out.toString());
        }
        {
            Template t = new Template("foo.f3ax", commonFTL, cfg);
            assertSame(XMLOutputFormat.INSTANCE, t.getOutputFormat());
            StringWriter out = new StringWriter();
            t.process(null, out);
            assertEquals("&apos;", out.toString());
        }
        {
            Template t = new Template("foo.f3ah", commonFTL, cfg);
            assertSame(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
            StringWriter out = new StringWriter();
            t.process(null, out);
            assertEquals("&#39;", out.toString());
        }
    }
    
    @Test
    public void testStandardFileExtensionsFormatterImplOverriding() throws Exception {
        addTemplate("t.f3ah", "${'a&x'}");

        assertOutputForNamed("t.f3ah", "a&amp;x");

        setConfiguration(newConfigurationBuilder()
                .registeredCustomOutputFormats(Collections.singleton(CustomHTMLOutputFormat.INSTANCE)));
        assertOutputForNamed("t.f3ah", "a&amp;X");

        setConfiguration(newConfigurationBuilder()
                .registeredCustomOutputFormats(Collections.emptyList()));
        assertOutputForNamed("t.f3ah", "a&amp;x");
    }
    
    @Test
    public void testAutoEscapingSettingLayers() throws Exception {
        addTemplate("t", "${'a&b'}");
        addTemplate("tWithHeaderFalse", "<#ftl autoEsc=false>${'a&b'}");
        addTemplate("tWithHeaderTrue", "<#ftl autoEsc=true>${'a&b'}");
        
        for (boolean cfgAutoEscaping : new boolean[] { true, false }) {
            Configuration.ExtendableBuilder<?> cfgB = newConfigurationBuilder().outputFormat(XMLOutputFormat.INSTANCE);
            assertEquals(ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

            if (!cfgAutoEscaping) {
                cfgB.setAutoEscapingPolicy(DISABLE);
            }
            setConfiguration(cfgB);

            {
                Template t = getConfiguration().getTemplate("t");
                if (cfgAutoEscaping) {
                    assertEquals(ENABLE_IF_DEFAULT, t.getAutoEscapingPolicy());
                    assertOutput(t, "a&amp;b");
                } else {
                    assertEquals(DISABLE, t.getAutoEscapingPolicy());
                    assertOutput(t, "a&b");
                }
            }
            
            {
                Template t = getConfiguration().getTemplate("tWithHeaderFalse");
                assertEquals(DISABLE, t.getAutoEscapingPolicy());
                assertOutput(t, "a&b");
            }
            
            {
                Template t = getConfiguration().getTemplate("tWithHeaderTrue");
                assertEquals(ENABLE_IF_SUPPORTED, t.getAutoEscapingPolicy());
                assertOutput(t, "a&amp;b");
            }

            getConfiguration().clearTemplateCache();
        }
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
        for (boolean cfgAutoEscaping : new boolean[] { true, false }) {
            String commonAutoEscFtl = "<#ftl outputFormat='HTML'>${'&'}";
            if (cfgAutoEscaping) {
                // Cfg default is autoEscaping true
                assertOutput(commonAutoEscFtl, "&amp;");
            } else {
                setConfiguration(newConfigurationBuilder().autoEscapingPolicy(DISABLE));
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
        TemplateConfiguration tc = new TemplateConfiguration.Builder()
                .templateLanguage(new UnparsedTemplateLanguage("x", null))
                .recognizeStandardFileExtensions(false)
                .build(); 
        
        String content = "<#ftl>a<#foo>b${x}";
        
        {
            Template t = new Template("x", content, getConfiguration(), tc);
            Writer sw = new StringWriter();
            t.process(null, sw);
            assertEquals(content, sw.toString());
            assertEquals(UndefinedOutputFormat.INSTANCE, t.getOutputFormat());
        }
        {
            setConfiguration(newConfigurationBuilder().outputFormat(HTMLOutputFormat.INSTANCE));
            Template t = new Template("x", content, getConfiguration(), tc);
            Writer sw = new StringWriter();
            t.process(null, sw);
            assertEquals(content, sw.toString());
            assertEquals(HTMLOutputFormat.INSTANCE, t.getOutputFormat());
        }
    }

    @Test
    public void testStringLiteralInterpolation() throws IOException, TemplateException {
        Template t = new Template(null, "<#ftl outputFormat='XML'>${'&'} ${\"(${'&'})\"?noEsc}", getConfiguration());
        assertEquals(XMLOutputFormat.INSTANCE, t.getOutputFormat());
        
        assertOutput("${.outputFormat} ${'${.outputFormat}'} ${.outputFormat}",
                "undefined undefined undefined");
        assertOutput("<#ftl outputFormat='HTML'>${.outputFormat} ${'${.outputFormat}'} ${.outputFormat}",
                "HTML HTML HTML");
        assertOutput("${.outputFormat} <#outputFormat 'XML'>${'${.outputFormat}'}</#outputFormat> ${.outputFormat}",
                "undefined XML undefined");
        assertOutput("${'foo ${xmlPlain}'}", "foo a &lt; {x&apos;}");
        assertOutput("${'${xmlMarkup}'}", "<p>c</p>");
        assertErrorContains("${'${\"x\"?esc}'}", "?esc", "undefined");
        assertOutput("<#ftl outputFormat='XML'>${'${xmlMarkup?esc} ${\"<\"?esc} ${\">\"} ${\"&amp;\"?noEsc}'}",
                "<p>c</p> &lt; &gt; &amp;");
    }
    
    @Test
    public void testStringBIsFail() {
        assertErrorContains("<#ftl outputFormat='HTML'>${'<b>foo</b>'?esc?upperCase}", "string", "markupOutput");
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
        
        addTemplate("t.f3ax", commonFTL);
        assertOutputForNamed("t.f3ax", "XML true");
        
        addTemplate("t.f3ah", commonFTL);
        assertOutputForNamed("t.f3ah", "HTML true");

        addTemplate("t.f3au", commonFTL);
        assertOutputForNamed("t.f3au", "undefined false");
        
        addTemplate("tX.f3ah", "<#ftl outputFormat='XML'>" + commonFTL);
        assertOutputForNamed("tX.f3ah", "XML true");
        addTemplate("tX.f3ax", commonFTL);
        assertOutputForNamed("t.f3ax", "XML true");
        
        addTemplate("tN.f3ac", "<#ftl outputFormat='RTF' autoEsc=false>" + commonFTL);
        assertOutputForNamed("tN.f3ac", "RTF false");
        
        assertOutput("${.outputFormat} ${.autoEsc?c}", "undefined false");
    }
    
    @Test
    public void testEscAndNoEscBIBasics() throws IOException, TemplateException {
        String commonFTL = "${'<x>'} ${'<x>'?esc} ${'<x>'?noEsc}";
        addTemplate("t.f3ah", commonFTL);
        addTemplate("t-noAuto.f3ah", "<#ftl autoEsc=false>" + commonFTL);
        addTemplate("t.f3au", commonFTL);
        assertOutputForNamed("t.f3ah", "&lt;x&gt; &lt;x&gt; <x>");
        assertOutputForNamed("t-noAuto.f3ah", "<x> &lt;x&gt; <x>");
        assertErrorContainsForNamed("t.f3au", "output format", "undefined");
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
    public void testMarkupStringBI() throws Exception {
        assertOutput(
                "${htmlPlain?markupString} ${htmlMarkup?markupString}",
                "a &lt; {h&#39;} <p>c");
        assertErrorContains(
                "${noSuchVar?markupString}",
                "noSuchVar", "null or missing");
        assertErrorContains(
                "${'x'?markupString}",
                "xpected", "markupOutput", "string");
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
                "<#ftl outputFormat='XML'>"
                + "${.outputFormat}${'\\''} "
                + "<#outputFormat 'HTML'>${.outputFormat}${'\\''}</#outputFormat> "
                + "${.outputFormat}${'\\''}",
                "XML&apos; HTML&#39; XML&apos;");
        
        // Custom format:
        assertErrorContains(
                "<#outputFormat 'dummy'></#outputFormat>",
                "dummy", "nregistered");
        setConfiguration(newConfigurationBuilder()
                .registeredCustomOutputFormats(Collections.singleton(DummyOutputFormat.INSTANCE)));
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
        
        // Empty block:
        assertOutput(
                "${.outputFormat} "
                + "<#outputFormat 'HTML'></#outputFormat>"
                + "${.outputFormat}",
                "undefined undefined");
        
        // WS stripping:
        assertOutput(
                "${.outputFormat}\n"
                + "<#outputFormat 'HTML'>\n"
                + "  x\n"
                + "</#outputFormat>\n"
                + "${.outputFormat}",
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
                "<#ftl autoEsc=false outputFormat='XML'>"
                + "${.autoEsc?c}${'&'} "
                + "<#autoEsc>${.autoEsc?c}${'&'}</#autoEsc> "
                + "${.autoEsc?c}${'&'}",
                "false& true&amp; false&");
        
        // Bad camel case:
        assertErrorContains(
                "<#noAutoesc></#noAutoesc>",
                "Unknown directive");
        assertErrorContains(
                "<#noautoEsc></#noautoEsc>",
                "Unknown directive");

        setConfiguration(newConfigurationBuilder()
                .outputFormat(XMLOutputFormat.INSTANCE));
        
        // Empty block:
        assertOutput(
                "${.autoEsc?c} "
                + "<#noAutoEsc></#noAutoEsc>"
                + "${.autoEsc?c}",
                "true true");
        
        // WS stripping:
        assertOutput(
                "${.autoEsc?c}\n"
                + "<#noAutoEsc>\n"
                + "  x\n"
                + "</#noAutoEsc>\n"
                + "${.autoEsc?c}",
                "true\n  x\ntrue");
    }

    @Test
    public void testMixedContent() throws Exception {
        setConfiguration(newConfigurationBuilder().outputFormat(DummyOutputFormat.INSTANCE));
        addToDataModel("m1", HTMLOutputFormat.INSTANCE.fromMarkup("x"));
        addToDataModel("m2", XMLOutputFormat.INSTANCE.fromMarkup("y"));
        assertOutput("${m1}", "x");
        assertErrorContains("${m2}", "is incompatible with");
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
    public void testAutoEscPolicy() throws Exception {
        assertEquals(ENABLE_IF_DEFAULT, newConfigurationBuilder().getAutoEscapingPolicy());
        
        String commonFTL = "${'.'} ${.autoEsc?c}";
        String notEsced = ". false";
        String esced = "\\. true";
        for (AutoEscapingPolicy autoEscPolicy : new AutoEscapingPolicy[] {
                ENABLE_IF_DEFAULT,
                ENABLE_IF_SUPPORTED,
                DISABLE }) {
            String sExpted = autoEscPolicy == ENABLE_IF_SUPPORTED ? esced : notEsced;
            setConfiguration(testAutoEscPolicy_createCfg(autoEscPolicy, SeldomEscapedOutputFormat.INSTANCE));
            assertOutput(commonFTL, sExpted);
            setConfiguration(testAutoEscPolicy_createCfg(autoEscPolicy, UndefinedOutputFormat.INSTANCE));
            assertOutput("<#ftl outputFormat='seldomEscaped'>" + commonFTL, sExpted);
            assertOutput("<#outputFormat 'seldomEscaped'>" + commonFTL + "</#outputFormat>", sExpted);
            
            String dExpted = autoEscPolicy == DISABLE ? notEsced : esced;
            setConfiguration(testAutoEscPolicy_createCfg(autoEscPolicy, DummyOutputFormat.INSTANCE));
            assertOutput(commonFTL, dExpted);
            setConfiguration(testAutoEscPolicy_createCfg(autoEscPolicy, UndefinedOutputFormat.INSTANCE));
            assertOutput("<#ftl outputFormat='dummy'>" + commonFTL, dExpted);
            assertOutput("<#outputFormat 'dummy'>" + commonFTL + "</#outputFormat>", dExpted);
            
            setConfiguration(testAutoEscPolicy_createCfg(autoEscPolicy, DummyOutputFormat.INSTANCE));
            assertOutput(
                    commonFTL
                    + "<#outputFormat 'seldomEscaped'>"
                        + commonFTL
                        + "<#outputFormat 'dummy'>"
                            + commonFTL
                        + "</#outputFormat>"
                        + commonFTL
                        + "<#outputFormat 'plainText'>"
                            + commonFTL
                        + "</#outputFormat>"
                        + commonFTL
                        + "<#noAutoEsc>"
                            + commonFTL
                        + "</#noAutoEsc>"
                        + commonFTL
                        + "<#autoEsc>"
                            + commonFTL
                        + "</#autoEsc>"
                        + commonFTL
                    + "</#outputFormat>"
                    + commonFTL
                    + "<#noAutoEsc>"
                        + commonFTL
                    + "</#noAutoEsc>"
                    + commonFTL
                    + "<#autoEsc>"
                        + commonFTL
                    + "</#autoEsc>"
                    + commonFTL
                    ,
                    dExpted
                        + sExpted
                            + dExpted
                        + sExpted
                            + notEsced
                        + sExpted
                            + notEsced
                        + sExpted
                            + esced
                        + sExpted
                    + dExpted
                        + notEsced
                    + dExpted
                        + esced
                    + dExpted);
        }
    }

    private Configuration testAutoEscPolicy_createCfg(AutoEscapingPolicy autoEscPolicy,
            OutputFormat outpoutFormat)
            throws TemplateException {
        return super.newConfigurationBuilder()
                .registeredCustomOutputFormats(ImmutableList.of(
                        SeldomEscapedOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE))
                .autoEscapingPolicy(autoEscPolicy)
                .outputFormat(outpoutFormat)
                .build();
    }

    @Test
    public void testForcedAutoEsc() throws Exception {
        String commonFTL = "${'.'} ${.autoEsc?c}";
        String esced = "\\. true";

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(SeldomEscapedOutputFormat.INSTANCE));
        assertOutput(commonFTL, esced);

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(DummyOutputFormat.INSTANCE));
        assertOutput(commonFTL, esced);

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(PlainTextOutputFormat.INSTANCE));
        assertOutput("<#ftl outputFormat='seldomEscaped'>" + commonFTL, esced);

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(HTMLOutputFormat.INSTANCE));
        assertOutput("<#outputFormat 'seldomEscaped'>" + commonFTL + "</#outputFormat>", esced);

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(PlainTextOutputFormat.INSTANCE));
        assertErrorContains("", IllegalArgumentException.class,
                "plainText", "autoEscapingPolicy", "force");

        setConfigurationForTestForceAutoEsc(cfgBuilder -> cfgBuilder.setOutputFormat(DummyOutputFormat.INSTANCE));
        assertErrorContains("<#ftl autoEsc=false>", ParseException.class,
                "autoEsc=false", "autoEscapingPolicy", "force");
        assertErrorContains("<#outputFormat 'plainText'></#outputformat>", ParseException.class,
                "plainText", "autoEscapingPolicy", "force");
        assertErrorContains("<#noAutoEsc></#noAutoEsc>", ParseException.class,
                "noAutoEsc", "autoEscapingPolicy", "force");
        assertErrorContains("<#noAutoEsc></#noautoesc>", ParseException.class,
                "noAutoEsc", "autoEscapingPolicy", "force");
        assertErrorContains("<#assign foo='bar'>${foo?noEsc}", ParseException.class,
                "?noEsc", "autoEscapingPolicy", "force");
        assertErrorContains("<#assign foo='bar'>${foo?noEsc}", ParseException.class,
                "?noEsc", "autoEscapingPolicy", "force");
    }

    private void setConfigurationForTestForceAutoEsc(Consumer<Configuration.ExtendableBuilder<?>> cfgBuilderAdjuster) {
        setConfiguration(cfgBuilder -> {
            cfgBuilder.setRegisteredCustomOutputFormats(ImmutableList.of(
                    SeldomEscapedOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE));
            cfgBuilder.setAutoEscapingPolicy(AutoEscapingPolicy.FORCE);
            cfgBuilderAdjuster.accept(cfgBuilder);
        });
    }

    @Test
    public void testDynamicParsingBIsInherticContextOutputFormat() throws Exception {
        // Dynamic parser BI-s are supposed to use the ParsingConfiguration of the calling template, and ignore anything
        // inside the calling template itself. Except, the outputFormat and autoEscapingPolicy has to come from the
        // calling lexical context.
        
        String commonFTL
                = "Eval: ${'.outputFormat'?eval}; "
                  + "Interpret: <#assign ipd = r\"${.outputFormat} ${'{&}'}\"?interpret><@ipd/>";
        addTemplate("t.f3ah", commonFTL);
        addTemplate("t2.f3ah", "<#outputFormat 'RTF'>" + commonFTL + "</#outputFormat>");
        
        assertOutputForNamed(
                "t.f3ah",
                "Eval: HTML; Interpret: HTML {&amp;}");
        assertOutputForNamed(
                "t2.f3ah",
                "Eval: RTF; Interpret: RTF \\{&\\}");
        assertOutput(
                commonFTL,
                "Eval: undefined; Interpret: undefined {&}");
        assertOutput(
                "<#ftl outputFormat='RTF'>" + commonFTL + "\n"
                + "<#outputFormat 'XML'>" + commonFTL + "</#outputFormat>",
                "Eval: RTF; Interpret: RTF \\{&\\}\n"
                + "Eval: XML; Interpret: XML {&amp;}");
        
        // parser.autoEscapingPolicy is inherited too:
        assertOutput(
                "<#ftl autoEsc=false outputFormat='XML'>"
                + commonFTL + " ${'.autoEsc'?eval?c}",
                "Eval: XML; Interpret: XML {&} false");
        assertOutput(
                "<#ftl outputFormat='XML'>"
                + "<#noAutoEsc>" + commonFTL + " ${'.autoEsc'?eval?c}</#noAutoEsc>",
                "Eval: XML; Interpret: XML {&} false");
        assertOutput(
                "<#ftl autoEsc=false outputFormat='XML'>"
                + "<#noAutoEsc>" + commonFTL + " ${'.autoEsc'?eval?c}</#noAutoEsc>",
                "Eval: XML; Interpret: XML {&} false");
        assertOutput(
                "<#ftl autoEsc=false outputFormat='XML'>"
                + "<#autoEsc>" + commonFTL + " ${'.autoEsc'?eval?c}</#autoEsc>",
                "Eval: XML; Interpret: XML {&amp;} true");
        assertOutput(
                "${.outputFormat}<#assign ftl='<#ftl outputFormat=\\'RTF\\'>$\\{.outputFormat}'> <@ftl?interpret/>",
                "undefined RTF");
        assertOutput(
                "${.outputFormat}<#outputFormat 'RTF'>"
                + "<#assign ftl='$\\{.outputFormat}'> <@ftl?interpret/> ${'.outputFormat'?eval}"
                + "</#outputFormat>",
                "undefined RTF RTF");
    }

    @Test
    public void testBannedBIsWhenAutoEscaping() throws Exception {
        for (String biName : new String[] { "html", "xhtml", "rtf", "xml" }) {
            String commonFTL = "${'x'?" + biName + "}";
            assertOutput(commonFTL, "x");
            assertErrorContains("<#ftl outputFormat='HTML'>" + commonFTL,
                    "?" + biName, "HTML", "double-escaping");
            assertErrorContains("<#ftl outputFormat='HTML'>${'${\"x\"?" + biName + "}'}",
                    "?" + biName, "HTML", "double-escaping");
            assertOutput("<#ftl outputFormat='plainText'>" + commonFTL, "x");
            assertOutput("<#ftl outputFormat='HTML' autoEsc=false>" + commonFTL, "x");
            assertOutput("<#ftl outputFormat='HTML'><#noAutoEsc>" + commonFTL + "</#noAutoEsc>", "x");
            assertOutput("<#ftl outputFormat='HTML'><#outputFormat 'plainText'>" + commonFTL + "</#outputFormat>",
                    "x");
        }
    }

    @Test
    public void testLegacyEscaperBIsBypassMOs() throws Exception {
        assertOutput("${htmlPlain?html} ${htmlMarkup?html}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${xmlPlain?html}", "?html", "string", "markupOutput", "XML");
        assertErrorContains("${xmlMarkup?html}", "?html", "string", "markupOutput", "XML");
        assertErrorContains("${rtfPlain?html}", "?html", "string", "markupOutput", "RTF");
        assertErrorContains("${rtfMarkup?html}", "?html", "string", "markupOutput", "RTF");

        assertOutput("${htmlPlain?xhtml} ${htmlMarkup?xhtml}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${xmlPlain?xhtml}", "?xhtml", "string", "markupOutput", "XML");
        assertErrorContains("${xmlMarkup?xhtml}", "?xhtml", "string", "markupOutput", "XML");
        assertErrorContains("${rtfPlain?xhtml}", "?xhtml", "string", "markupOutput", "RTF");
        assertErrorContains("${rtfMarkup?xhtml}", "?xhtml", "string", "markupOutput", "RTF");
        
        assertOutput("${xmlPlain?xml} ${xmlMarkup?xml}", "a &lt; {x&apos;} <p>c</p>");
        assertOutput("${htmlPlain?xml} ${htmlMarkup?xml}", "a &lt; {h&#39;} <p>c");
        assertErrorContains("${rtfPlain?xml}", "?xml", "string", "markupOutput", "RTF");
        assertErrorContains("${rtfMarkup?xml}", "?xml", "string", "markupOutput", "RTF");
        
        assertOutput("${rtfPlain?rtf} ${rtfMarkup?rtf}", "\\\\par a & b \\par c");
        assertErrorContains("${xmlPlain?rtf}", "?rtf", "string", "markupOutput", "XML");
        assertErrorContains("${xmlMarkup?rtf}", "?rtf", "string", "markupOutput", "XML");
        assertErrorContains("${htmlPlain?rtf}", "?rtf", "string", "markupOutput", "HTML");
        assertErrorContains("${htmlMarkup?rtf}", "?rtf", "string", "markupOutput", "HTML");
    }
    
    @Test
    public void testBannedDirectivesWhenAutoEscaping() throws Exception {
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
        addTemplate("t.f3ax", commonFtl);
        assertOutputForNamed(
                "t.f3ax",
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
    
    @Test
    public void testHasContentBI() throws Exception {
        assertOutput("${htmlMarkup?hasContent?c} ${htmlPlain?hasContent?c}", "true true");
        assertOutput("<#ftl outputFormat='HTML'>${''?esc?hasContent?c} ${''?noEsc?hasContent?c}", "false false");
    }
    
    @Test
    public void testMissingVariables() throws Exception {
        for (String ftl : new String[] {
                "${noSuchVar}",
                "<#ftl outputFormat='XML'>${noSuchVar}",
                "<#ftl outputFormat='XML'>${noSuchVar?esc}",
                "<#ftl outputFormat='XML'>${'x'?esc + noSuchVar}"
                }) {
            assertErrorContains(ftl, InvalidReferenceException.class, "noSuchVar", "null or missing");
        }
    }

    @Test
    public void testIsMarkupOutputBI() throws Exception {
        addToDataModel("m1", HTMLOutputFormat.INSTANCE.fromPlainTextByEscaping("x"));
        addToDataModel("m2", HTMLOutputFormat.INSTANCE.fromMarkup("x"));
        addToDataModel("s", "x");
        assertOutput("${m1?isMarkupOutput?c} ${m2?isMarkupOutput?c} ${s?isMarkupOutput?c}", "true true false");
    }

    @Before
    public void addCommonDataModelVariables() throws TemplateException {
        addToDataModel("rtfPlain", RTFOutputFormat.INSTANCE.fromPlainTextByEscaping("\\par a & b"));
        addToDataModel("rtfMarkup", RTFOutputFormat.INSTANCE.fromMarkup("\\par c"));
        addToDataModel("htmlPlain", HTMLOutputFormat.INSTANCE.fromPlainTextByEscaping("a < {h'}"));
        addToDataModel("htmlMarkup", HTMLOutputFormat.INSTANCE.fromMarkup("<p>c"));
        addToDataModel("xmlPlain", XMLOutputFormat.INSTANCE.fromPlainTextByEscaping("a < {x'}"));
        addToDataModel("xmlMarkup", XMLOutputFormat.INSTANCE.fromMarkup("<p>c</p>"));
    }

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*.xml"),
                                new TemplateConfiguration.Builder()
                                        .outputFormat(XMLOutputFormat.INSTANCE)
                                        .build()))
                .templateCacheStorage(NullCacheStorage.INSTANCE); // Prevent caching as we change the cfgB between build().
    }

}
