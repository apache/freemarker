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

public class CallerTemplateNameTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_28);
        return cfg;
    }

    @Test
    public void testBaics() throws Exception {
        addTemplate("main.ftl", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<#function f()><#return .callerTemplateName></#function>"
                + "<@m /> ${f()} [<#include 'other.ftl'>] <@m /> ${f()}");
        addTemplate("other.ftl", ""
                + "<@m /> ${f()} [<#include 'yet-another.ftl'>] <@m /> ${f()}");
        addTemplate("yet-another.ftl", ""
                + "<@m /> ${f()}");
        
        assertOutputForNamed("main.ftl", ""
                + "main.ftl main.ftl "
                + "[other.ftl other.ftl "
                + "[yet-another.ftl yet-another.ftl] "
                + "other.ftl other.ftl] "
                + "main.ftl main.ftl");
    }

    @Test
    public void testNoCaller() throws Exception {
        assertErrorContains("${.callerTemplateName}", "no macro or function", ".callerTemplateName");
        assertErrorContains("${.caller_template_name}", "no macro or function", ".caller_template_name");

        assertErrorContains(""
                + "<#macro m><#nested></#macro>"
                + "<@m>${.callerTemplateName}</@>",
                "no macro or function", ".callerTemplateName");

        addTemplate("main.ftl", "${.callerTemplateName}");
        assertErrorContainsForNamed("main.ftl", "no macro or function");
    }

    @Test
    public void testNamelessCaller() throws Exception {
        assertOutput(""
                + "<#macro m2>${.callerTemplateName}</#macro>"
                + "[<@m2/>]",
                "[]");
    }

    @Test
    public void testNested() throws Exception {
        addTemplate("main.ftl", ""
                + "<#include 'lib1.ftl'>"
                + "<#include 'lib2.ftl'>"
                + "<@m1 />");
        addTemplate("lib1.ftl", ""
                + "<#macro m1>"
                + "${.callerTemplateName} [<@m2>${.callerTemplateName}</@m2>] ${.callerTemplateName}"
                + "</#macro>");
        addTemplate("lib2.ftl", ""
                + "<#macro m2>"
                + "${.callerTemplateName} [<#nested>] ${.callerTemplateName}"
                + "</#macro>");
        assertOutputForNamed("main.ftl", ""
                + "main.ftl [lib1.ftl [main.ftl] lib1.ftl] main.ftl");
    }
    
    @Test
    public void testSelfCaller() throws Exception {
        addTemplate("main.ftl", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<@m />");
        assertOutputForNamed("main.ftl", "main.ftl");
    }

    @Test
    public void testImportedTemplateCaller() throws Exception {
        addTemplate("main.ftl", ""
                + "<#import 'lib/foo.ftl' as foo>"
                + "<@foo.m />, <@foo.m2 />");
        addTemplate("lib/foo.ftl", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<#macro m2><@m3/></#macro>"
                + "<#macro m3>${.callerTemplateName}</#macro>");
        assertOutputForNamed("main.ftl",
                "main.ftl, lib/foo.ftl");
    }
    
    @Test
    public void testNestedIntoNonUserDirectives() throws Exception {
        addTemplate("main.ftl", ""
                + "<#macro m><#list 1..2 as _><#if true>${.callerTemplateName}</#if>;</#list></#macro>"
                + "<@m/>");
        assertOutputForNamed("main.ftl", "main.ftl;main.ftl;");
    }

    @Test
    public void testUsedInArgument() throws Exception {
        addTemplate("main.ftl", ""
                + "<#include 'inc.ftl'>"
                + "<#macro start>"
                + "<@m .callerTemplateName />"
                + "<@m2 />"
                + "</#macro>"
                + "<@start />");
        addTemplate("inc.ftl", ""
                + "<#macro m x y=.callerTemplateName>"
                + "x: ${x}; y: ${y}; caller: ${.callerTemplateName};"
                + "</#macro>"
                + "<#macro m2><@m .callerTemplateName /></#macro>");
        
        for (int i = 0; i < 2; i++) {
            assertOutputForNamed("main.ftl", ""
                    + "x: main.ftl; y: main.ftl; caller: main.ftl;"
                    + "x: main.ftl; y: inc.ftl; caller: inc.ftl;");
            getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_27); // Has no effect
        }
    }
    
    @Test
    public void testReturnsLookupName() throws Exception {
        addTemplate("main_en.ftl", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<@m />");
        assertOutputForNamed("main.ftl", "main.ftl"); // Not main_en.ftl
    }
    
    @Test
    public void testLegacyCall() throws Exception {
        addTemplate("main_en.ftl", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<#call m>");
        assertOutputForNamed("main.ftl", "main.ftl"); // Not main_en.ftl
    }
    
}
