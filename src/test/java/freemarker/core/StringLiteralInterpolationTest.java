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
package freemarker.core;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
public class StringLiteralInterpolationTest extends TemplateTest {

    @Test
    public void basics() throws IOException, TemplateException {
        addToDataModel("x", 1);
        assertOutput("${'${x}'}", "1");
        assertOutput("${'#{x}'}", "1");
        assertOutput("${'a${x}b${x*2}c'}", "a1b2c");
        assertOutput("${'a#{x}b#{x*2}c'}", "a1b2c");
        assertOutput("${'a#{x; m2}'}", "a1.00");
        assertOutput("${'${x} ${x}'}", "1 1");
        assertOutput("${'$\\{x}'}", "${x}");
        assertOutput("${'$\\{x} $\\{x}'}", "${x} ${x}");
        assertOutput("${'<#-- not a comment -->${x}'}", "<#-- not a comment -->1");
        assertOutput("${'<#-- not a comment -->$\\{x}'}", "<#-- not a comment -->${x}");
        assertOutput("${'<#assign x = 2> ${x} <#assign x = 2>'}", "<#assign x = 2> 1 <#assign x = 2>");
        assertOutput("${'<#assign x = 2> $\\{x} <#assign x = 2>'}", "<#assign x = 2> ${x} <#assign x = 2>");
        assertOutput("${'<@x/>${x}<@x/>'}", "<@x/>1<@x/>");
        assertOutput("${'<@x/>$\\{x}<@x/>'}", "<@x/>${x}<@x/>");
        assertOutput("${'<@ ${x}<@'}", "<@ 1<@");
        assertOutput("${'<@ $\\{x}<@'}", "<@ ${x}<@");
        assertOutput("${'</@x>${x}'}", "</@x>1");
        assertOutput("${'</@x>$\\{x}'}", "</@x>${x}");
        assertOutput("${'</@ ${x}</@'}", "</@ 1</@");
        assertOutput("${'</@ $\\{x}</@'}", "</@ ${x}</@");
        assertOutput("${'[@ ${x}'}", "[@ 1");
        assertOutput("${'[@ $\\{x}'}", "[@ ${x}");
    }

    /**
     * Broken behavior for backward compatibility.
     */
    @Test
    public void legacyEscapingBugStillPresent() throws IOException, TemplateException {
        addToDataModel("x", 1);
        assertOutput("${'$\\{x} ${x}'}", "1 1");
        assertOutput("${'${x} $\\{x}'}", "1 1");
    }
    
    @Test
    public void legacyLengthGlitch() throws IOException, TemplateException {
        assertOutput("${'${'}", "${");
        assertOutput("${'${1'}", "${1");
        assertOutput("${'${}'}", "${}");
        assertOutput("${'${1}'}", "1");
        assertErrorContains("${'${  '}", "");
    }
    
    @Test
    public void testErrors() {
        addToDataModel("x", 1);
        assertErrorContains("${'${noSuchVar}'}", InvalidReferenceException.class, "missing", "noSuchVar");
        assertErrorContains("${'${x/0}'}", "zero");
    }

    @Test
    public void escaping() throws IOException, TemplateException {
        assertOutput("<#escape x as x?html><#assign x = '&'>${x} ${'${x}'}</#escape> ${x}", "&amp; &amp; &");
    }
    
    @Test
    public void iciInheritanceBugFixed() throws Exception {
        // Broken behavior emulated:
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        assertOutput("${'&\\''?html} ${\"${'&\\\\\\''?html}\"}", "&amp;&#39; &amp;'");
        
        // Fix enabled:
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        assertOutput("${'&\\''?html} ${\"${'&\\\\\\''?html}\"}", "&amp;&#39; &amp;&#39;");
    }
    
    @Test
    public void markup() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setCustomNumberFormats(Collections.singletonMap("G", PrintfGTemplateNumberFormatFactory.INSTANCE));
        cfg.setNumberFormat("@G 3");
        
        assertOutput("${\"${1000}\"}", "1.00*10<sup>3</sup>");
        assertOutput("${\"&_${1000}\"}", "&amp;_1.00*10<sup>3</sup>");
        assertOutput("${\"${1000}_&\"}", "1.00*10<sup>3</sup>_&amp;");
        assertOutput("${\"${1000}, ${2000}\"}", "1.00*10<sup>3</sup>, 2.00*10<sup>3</sup>");
        assertOutput("${\"& ${'x'}, ${2000}\"}", "&amp; x, 2.00*10<sup>3</sup>");
        assertOutput("${\"& ${'x'}, #{2000}\"}", "& x, 2000");
        
        assertOutput("${\"${2000}\"?isMarkupOutput?c}", "true");
        assertOutput("${\"x ${2000}\"?isMarkupOutput?c}", "true");
        assertOutput("${\"${2000} x\"?isMarkupOutput?c}", "true");
        assertOutput("${\"#{2000}\"?isMarkupOutput?c}", "false");
        assertOutput("${\"${'x'}\"?isMarkupOutput?c}", "false");
        assertOutput("${\"x ${'x'}\"?isMarkupOutput?c}", "false");
        assertOutput("${\"${'x'} x\"?isMarkupOutput?c}", "false");
        
        addToDataModel("rtf", RTFOutputFormat.INSTANCE.fromMarkup("\\p"));
        assertOutput("${\"${rtf}\"?isMarkupOutput?c}", "true");
        assertErrorContains("${\"${1000}${rtf}\"}", TemplateException.class, "HTML", "RTF", "onversion");
        assertErrorContains("x${\"${1000}${rtf}\"}", TemplateException.class, "HTML", "RTF", "onversion");
    }
    
}
