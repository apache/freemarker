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

import java.io.IOException;

import org.apache.freemarker.core.userpkg.AllFeaturesDirective;
import org.apache.freemarker.core.userpkg.NamedVarargsOnlyDirective;
import org.apache.freemarker.core.userpkg.PositionalVarargsOnlyDirective;
import org.apache.freemarker.core.userpkg.TwoNamedParamsDirective;
import org.apache.freemarker.core.userpkg.TwoPositionalParamsDirective;
import org.apache.freemarker.core.userpkg.UpperCaseDirective;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

public class TemplateCallableModelTest extends TemplateTest {

    @Before
    public void addCommonData() {
        addToDataModel("a", new AllFeaturesDirective());
        addToDataModel("p", TwoPositionalParamsDirective.INSTANCE);
        addToDataModel("n", TwoNamedParamsDirective.INSTANCE);
        addToDataModel("pvo", PositionalVarargsOnlyDirective.INSTANCE);
        addToDataModel("nvo", NamedVarargsOnlyDirective.INSTANCE);
    }

    @Test
    public void testArguments() throws IOException, TemplateException {
        assertOutput("<@p />",
                "#p(p1=null, p2=null)");
        assertOutput("<@p 1 />",
                "#p(p1=1, p2=null)");
        assertOutput("<@p 1, 2 />",
                "#p(p1=1, p2=2)");

        assertOutput("<@n />",
                "#n(n1=null, n2=null)");
        assertOutput("<@n n1=11/>",
                "#n(n1=11, n2=null)");
        assertOutput("<@n n1=11 n2=22/>",
                "#n(n1=11, n2=22)");

        assertOutput("<@pvo />",
                "#pvo(pVarargs=[])");
        assertOutput("<@pvo 1 />",
                "#pvo(pVarargs=[1])");
        assertOutput("<@pvo 1, 2 />",
                "#pvo(pVarargs=[1, 2])");

        assertOutput("<@nvo />",
                "#nvo(nVarargs={})");
        assertOutput("<@nvo n1=11 />",
                "#nvo(nVarargs={\"n1\": 11})");
        assertOutput("<@nvo n1=11 n2=22/>",
                "#nvo(nVarargs={\"n1\": 11, \"n2\": 22})");

        assertOutput("<@a />",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={})");
        assertOutput("<@a 1, 2 />",
                "#a(p1=1, p2=2, pVarargs=[], n1=null, n2=null, nVarargs={})");
        assertOutput("<@a n1=11 n2=22 />",
                "#a(p1=null, p2=null, pVarargs=[], n1=11, n2=22, nVarargs={})");

        assertOutput("<@a 1, 2 n1=11 n2=22 />",
                "#a(p1=1, p2=2, pVarargs=[], n1=11, n2=22, nVarargs={})");
        assertOutput("<@a 1 n1=11 />",
                "#a(p1=1, p2=null, pVarargs=[], n1=11, n2=null, nVarargs={})");
        assertOutput("<@a 1, 2, 3 n1=11 n2=22 n3=33 />",
                "#a(p1=1, p2=2, pVarargs=[3], n1=11, n2=22, nVarargs={\"n3\": 33})");
        assertOutput("<@a 1 n1=11 n3=33 />",
                "#a(p1=1, p2=null, pVarargs=[], n1=11, n2=null, nVarargs={\"n3\": 33})");
        assertOutput("<@a 1 n1=11 a=1 b=2 c=3 d=4 e=5 f=6 g=7 />",
                "#a(p1=1, p2=null, pVarargs=[], n1=11, n2=null, nVarargs={"
                        + "\"a\": 1, \"b\": 2, \"c\": 3, \"d\": 4, \"e\": 5, \"f\": 6, \"g\": 7})");

        assertOutput("<@a; a, b, c/>",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 3)");
        assertOutput("<@a 1, 2; a, b, c />",
                "#a(p1=1, p2=2, pVarargs=[], n1=null, n2=null, nVarargs={}; 3)");
        assertOutput("<@a n1=11 n2=22; a, b, c />",
                "#a(p1=null, p2=null, pVarargs=[], n1=11, n2=22, nVarargs={}; 3)");
        assertOutput("<@a 1, 2 n1=11 n2=22; a, b, c />",
                "#a(p1=1, p2=2, pVarargs=[], n1=11, n2=22, nVarargs={}; 3)");
    }

    @Test
    public void testNestedContent() throws IOException, TemplateException {
        assertOutput("<@a />",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={})");
        assertOutput("<@a></@a>",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={})");

        assertOutput("<@a>x</@a>",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}) {}");
        assertOutput("<@a 1>x</@a>",
                "#a(p1=1, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}) {x}");
        assertOutput("<@a 3>x</@a>",
                "#a(p1=3, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}) {xxx}");
        assertOutput("<@a 3; i, j, k>[${i}${j}${k}]</@a>",
                "#a(p1=3, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 3) {[123][246][369]}");
        assertOutput("<#assign i = '-'>${i} <@a 3; i>${i}</@a> ${i}",
                "- #a(p1=3, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 1) {123} -");
    }

    @Test
    public void testSyntaxEdgeCases() throws IOException, TemplateException {
        assertOutput("<@a; x/>",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 1)");
        assertOutput("<@a;x/>",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 1)");
        assertOutput("<@a;x />",
                "#a(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={}; 1)");

        assertOutput("<@a  1  ,  2 n1 = 11  n2 = 22  ;  a  ,  b  ,  c  />",
                "#a(p1=1, p2=2, pVarargs=[], n1=11, n2=22, nVarargs={}; 3)");
        assertOutput("<@a 1<#-- -->,<#-- -->2<#-- -->n1<#-- -->=<#-- -->11<#-- -->n2=22<#-- -->;"
                        + "<#-- -->a<#-- -->,<#-- -->b<#-- -->,<#-- -->c<#-- -->/>",
                "#a(p1=1, p2=2, pVarargs=[], n1=11, n2=22, nVarargs={}; 3)");
        assertOutput("<@a\t1,2\tn1=11\tn2=22;a,b,c/>",
                "#a(p1=1, p2=2, pVarargs=[], n1=11, n2=22, nVarargs={}; 3)");

        assertOutput("<@a + 1 />",
                "#a(p1=1, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={})");
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void testParsingErrors() throws IOException, TemplateException {
        assertErrorContains("<@a, n1=1 />", "Remove comma", "between", "by position");
        assertErrorContains("<@a n1=1, n2=1 />", "Remove comma", "between", "by position");
        assertErrorContains("<@a n1=1, 2 />", "Remove comma", "between", "by position");
        assertErrorContains("<@a, 1 />", "Remove comma", "between", "by position");
        assertErrorContains("<@a 1, , 2 />", "Two commas");
        assertErrorContains("<@a 1 2 />", "Missing comma");
        assertErrorContains("<@a n1=1 2 />", "must be earlier than arguments passed by name");
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void testRuntimeErrors() throws IOException, TemplateException {
        assertErrorContains("<@p 9, 9, 9 />", "can only have 2", "3", "by position");
        assertErrorContains("<@n 9 />", "can't have arguments passed by position");
        assertErrorContains("<@n n3=9 />", "has no", "\"n3\"", "supported", "\"n1\", \"n2\"");
        assertErrorContains("<@p n1=9 />", "doesn't have any by-name-passed");
        assertErrorContains("<@a 1; i, j, k, l>x</@a>", "(4: \"i\", \"j\", \"k\", \"l\")", "(3)");
    }

    @Test
    public void testMacros() throws IOException, TemplateException {
        assertOutput("<#macro m a b=22><#list 1..2 as n>[<#nested a * n, b * n>]</#list></#macro>"
                + "<@m 11; i, j>${i} ${j}</@m> <@m a=1 b=2; i, j>${i} ${j}</@m>",
                "[11 22][22 44] [1 2][2 4]");
        assertOutput("<#macro m a b others...>[a=${a}, b=${b}<#if others?hasContent>, </#if>"
                        + "<#if others?isSequence>"
                        + "<#list others as v>${v}<#sep>, </#list>"
                        + "<#else>"
                        + "<#list others as k, v>${k}=${v}<#sep>, </#list>"
                        + "</#if>]"
                        + "</#macro>"
                        + "<@m 1, 2 /> <@m 1, 2, 3, 4 /> <@m a=1 b=2 /> <@m a=1 b=2 c=3 d=4 />",
                "[a=1, b=2] [a=1, b=2, 3, 4] [a=1, b=2] [a=1, b=2, c=3, d=4]");
    }

    @Test
    public void testFilterDirective() throws IOException, TemplateException {
        addToDataModel("uc", UpperCaseDirective.INSTANCE);
        assertOutput("<@uc>foo ${1 + 1}</@>", "FOO 2");
    }

}
