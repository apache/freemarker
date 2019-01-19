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

import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class CallerTemplateNameTest  extends TemplateTest {

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.localizedTemplateLookup(true);
    }

    @Test
    public void testBaics() throws Exception {
        addTemplate("main.f3ah", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<#function f()><#return .callerTemplateName></#function>"
                + "<@m /> ${f()} [<#include 'other.f3ah'>] <@m /> ${f()}");
        addTemplate("other.f3ah", ""
                + "<@m /> ${f()} [<#include 'yet-another.f3ah'>] <@m /> ${f()}");
        addTemplate("yet-another.f3ah", ""
                + "<@m /> ${f()}");
        
        assertOutputForNamed("main.f3ah", ""
                + "main.f3ah main.f3ah "
                + "[other.f3ah other.f3ah "
                + "[yet-another.f3ah yet-another.f3ah] "
                + "other.f3ah other.f3ah] "
                + "main.f3ah main.f3ah");
    }
    
    @Test
    public void testNoCaller() throws Exception {
        assertErrorContains("${.callerTemplateName}", "no macro or function");

        assertErrorContains(""
                + "<#macro m><#nested></#macro>"
                + "<@m>${.callerTemplateName}</@>",
                "no macro or function");

        addTemplate("main.f3ah", "${.callerTemplateName}");
        assertErrorContainsForNamed("main.f3ah", "no macro or function");
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
        addTemplate("main.f3ah", ""
                + "<#include 'lib1.f3ah'>"
                + "<#include 'lib2.f3ah'>"
                + "<@m1 />");
        addTemplate("lib1.f3ah", ""
                + "<#macro m1>"
                + "${.callerTemplateName} [<@m2>${.callerTemplateName}</@m2>] ${.callerTemplateName}"
                + "</#macro>");
        addTemplate("lib2.f3ah", ""
                + "<#macro m2>"
                + "${.callerTemplateName} [<#nested>] ${.callerTemplateName}"
                + "</#macro>");
        assertOutputForNamed("main.f3ah", ""
                + "main.f3ah [lib1.f3ah [main.f3ah] lib1.f3ah] main.f3ah");
    }
    
    @Test
    public void testSelfCaller() throws Exception {
        addTemplate("main.f3ah", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<@m />");
        assertOutputForNamed("main.f3ah", "main.f3ah");
    }

    @Test
    public void testImportedTemplateCaller() throws Exception {
        addTemplate("main.f3ah", ""
                + "<#import 'lib/foo.f3ah' as foo>"
                + "<@foo.m />, <@foo.m2 />");
        addTemplate("lib/foo.f3ah", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<#macro m2><@m3/></#macro>"
                + "<#macro m3>${.callerTemplateName}</#macro>");
        assertOutputForNamed("main.f3ah",
                "main.f3ah, lib/foo.f3ah");
    }
    
    @Test
    public void testNestedIntoNonUserDirectives() throws Exception {
        addTemplate("main.f3ah", ""
                + "<#macro m><#list 1..2 as _><#if true>${.callerTemplateName}</#if>;</#list></#macro>"
                + "<@m/>");
        assertOutputForNamed("main.f3ah", "main.f3ah;main.f3ah;");
    }

    @Test
    public void testUsedInArgument() throws Exception {
        addTemplate("main.f3ah", ""
                + "<#include 'inc.f3ah'>"
                + "<#macro start>"
                + "<@m .callerTemplateName />"
                + "<@m2 />"
                + "</#macro>"
                + "<@start />");
        addTemplate("inc.f3ah", ""
                + "<#macro m x{positional}, y{positional}=.callerTemplateName>"
                + "x: ${x}; y: ${y}; caller: ${.callerTemplateName};"
                + "</#macro>"
                + "<#macro m2><@m .callerTemplateName /></#macro>");
        
        assertOutputForNamed("main.f3ah", ""
                + "x: main.f3ah; y: main.f3ah; caller: main.f3ah;"
                + "x: main.f3ah; y: inc.f3ah; caller: inc.f3ah;");
    }
    
    @Test
    public void testReturnsLookupName() throws Exception {
        addTemplate("main_en.f3ah", ""
                + "<#macro m>${.callerTemplateName}</#macro>"
                + "<@m />");
        assertOutputForNamed("main.f3ah", "main.f3ah"); // Not main_en.f3ah
    }
    
}
