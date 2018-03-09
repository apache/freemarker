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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class MacroCallerTemplateNameTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_28);
        return cfg;
    }

    @Test
    public void testNoCaller() throws Exception {
        assertErrorContains("${.macroCallerTemplateName}", "no macro caller", ".macroCallerTemplateName");
        assertErrorContains("${.macro_caller_template_name}", "no macro caller", ".macro_caller_template_name");

        assertErrorContains(""
                + "<#macro m><#nested></#macro>"
                + "<@m>${.macroCallerTemplateName}</@>",
                "nested", ".macroCallerTemplateName");

        assertErrorContains(""
                + "<#macro m><#nested></#macro>"
                + "<#macro m2><@m>${.macroCallerTemplateName}</@></#macro>"
                + "<@m2/>",
                "nested", ".macroCallerTemplateName");
        assertOutput(""
                + "<#macro m2>${.macroCallerTemplateName}</#macro>"
                + "[<@m2/>]",
                "[]");

        addTemplate("main.ftl", "${.macroCallerTemplateName}");
        assertErrorContainsForNamed("main.ftl", "no macro caller");
    }

    @Test
    public void testSameTemplateCaller() throws Exception {
        addTemplate("main.ftl", ""
                + "<#macro m>${.macroCallerTemplateName}</#macro>"
                + "<@m />, <#attempt>${.macroCallerTemplateName}<#recover>-</#attempt>");
        assertOutputForNamed("main.ftl", "main.ftl, -");
    }

    @Test
    public void testIncludedTemplateCaller() throws Exception {
        addTemplate("main.ftl", ""
                + "<#include 'lib/foo.ftl'>"
                + "<@m />, <@m2 />");
        addTemplate("lib/foo.ftl", ""
                + "<#macro m>${.macroCallerTemplateName}</#macro>"
                + "<#macro m2><@m3/></#macro>"
                + "<#macro m3>${.macroCallerTemplateName}</#macro>");
        assertOutputForNamed("main.ftl",
                "main.ftl, lib/foo.ftl");
    }

    @Test
    public void testImportedTemplateCaller() throws Exception {
        addTemplate("main.ftl", ""
                + "<#import 'lib/foo.ftl' as foo>"
                + "<@foo.m />, <@foo.m2 />");
        addTemplate("lib/foo.ftl", ""
                + "<#macro m>${.macroCallerTemplateName}</#macro>"
                + "<#macro m2><@m3/></#macro>"
                + "<#macro m3>${.macroCallerTemplateName}</#macro>");
        assertOutputForNamed("main.ftl",
                "main.ftl, lib/foo.ftl");
    }
    
    @Test
    public void testNestedIntoNonUserDirectives() throws Exception {
        addTemplate("main.ftl", ""
                + "<#macro m><#list 1..2 as _><#if true>${.macroCallerTemplateName}</#if>;</#list></#macro>"
                + "<@m/>");
        assertOutputForNamed("main.ftl", "main.ftl;main.ftl;");
    }

    @Test
    public void testMulitpleLevels() throws Exception {
        addTemplate("main.ftl", ""
                + "<#include 'inc1.ftl'>"
                + "<@m1 />");
        addTemplate("inc1.ftl", ""
                + "<#include 'inc2.ftl'>"
                + "<#macro m1>m1: ${.macroCallerTemplateName}; <@m2 /></#macro>");
        addTemplate("inc2.ftl", ""
                + "<#macro m2>m2: ${.macroCallerTemplateName};</#macro>");
        assertOutputForNamed("main.ftl", "m1: main.ftl; m2: inc1.ftl;");
    }

    @Test
    public void testUsedInArgument() throws Exception {
        addTemplate("main.ftl", ""
                + "<#include 'inc.ftl'>"
                + "<#macro start>"
                + "<@m .macroCallerTemplateName />"
                + "<@m2 />"
                + "</#macro>"
                + "<@start />");
        addTemplate("inc.ftl", ""
                + "<#macro m x y=.macroCallerTemplateName>"
                + "x: ${x}; y: ${y}; caller: ${.macroCallerTemplateName};"
                + "</#macro>"
                + "<#macro m2><@m .macroCallerTemplateName /></#macro>");
        
        assertOutputForNamed("main.ftl", ""
                + "x: main.ftl; y: main.ftl; caller: main.ftl;"
                + "x: main.ftl; y: inc.ftl; caller: inc.ftl;");
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_27);
        assertOutputForNamed("main.ftl", ""
                + "x: main.ftl; y: main.ftl; caller: main.ftl;"
                + "x: inc.ftl; y: inc.ftl; caller: inc.ftl;");
    }
    
    @Test
    public void testReturnsLookupName() throws Exception {
        addTemplate("main_en.ftl", ""
                + "<#macro m>${.macroCallerTemplateName}</#macro>"
                + "<@m />");
        assertOutputForNamed("main.ftl", "main.ftl"); // Not main_en.ftl
    }
    
    @Test
    public void testLegacyCall() throws Exception {
        addTemplate("main_en.ftl", ""
                + "<#macro m>${.macroCallerTemplateName}</#macro>"
                + "<#call m>");
        assertOutputForNamed("main.ftl", "main.ftl"); // Not main_en.ftl
    }
    
}
