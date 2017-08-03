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
import org.apache.freemarker.core.userpkg.AllFeaturesFunction;
import org.apache.freemarker.core.userpkg.NamedVarargsOnlyDirective;
import org.apache.freemarker.core.userpkg.PositionalVarargsOnlyDirective;
import org.apache.freemarker.core.userpkg.PositionalVarargsOnlyFunction;
import org.apache.freemarker.core.userpkg.TwoNamedParamsDirective;
import org.apache.freemarker.core.userpkg.TwoNestedContentParamsDirective;
import org.apache.freemarker.core.userpkg.TwoPositionalParamsDirective;
import org.apache.freemarker.core.userpkg.TwoPositionalParamsFunction;
import org.apache.freemarker.core.userpkg.UpperCaseDirective;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

public class TemplateCallableModelTest extends TemplateTest {

    @Before
    public void addCommonData() {
        addToDataModel("a", AllFeaturesDirective.INSTANCE);
        addToDataModel("p", TwoPositionalParamsDirective.INSTANCE);
        addToDataModel("n", TwoNamedParamsDirective.INSTANCE);
        addToDataModel("pvo", PositionalVarargsOnlyDirective.INSTANCE);
        addToDataModel("nvo", NamedVarargsOnlyDirective.INSTANCE);
        addToDataModel("uc", UpperCaseDirective.INSTANCE);

        addToDataModel("fa", AllFeaturesFunction.INSTANCE);
        addToDataModel("fp", TwoPositionalParamsFunction.INSTANCE);
        addToDataModel("fpvo", PositionalVarargsOnlyFunction.INSTANCE);
    }

    @Test
    public void testDirectiveArguments() throws IOException, TemplateException {
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
    public void testFunctionArguments() throws IOException, TemplateException {
        // TODO [FM3] Add more tests as named parameters become supported

        assertOutput("${fp()}",
                "fp(p1=null, p2=null)");
        assertOutput("${fp(1)}",
                "fp(p1=1, p2=null)");
        assertOutput("${fp(1, 2)}",
                "fp(p1=1, p2=2)");

        assertOutput("${fpvo()}",
                "fpvo(pVarargs=[])");
        assertOutput("${fpvo(1)}",
                "fpvo(pVarargs=[1])");
        assertOutput("${fpvo(1, 2)}",
                "fpvo(pVarargs=[1, 2])");

        assertOutput("${fa()}",
                "fa(p1=null, p2=null, pVarargs=[], n1=null, n2=null, nVarargs={})");
        assertOutput("${fa(1, 2)}",
                "fa(p1=1, p2=2, pVarargs=[], n1=null, n2=null, nVarargs={})");
        assertOutput("${fa(1, 2, 3)}",
                "fa(p1=1, p2=2, pVarargs=[3], n1=null, n2=null, nVarargs={})");
        assertOutput("${fa(1, 2, 3, 4)}",
                "fa(p1=1, p2=2, pVarargs=[3, 4], n1=null, n2=null, nVarargs={})");
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

        assertOutput("<@nvo a=x! 1 b=2 />",
                "#nvo(nVarargs={\"a\": 1, \"b\": 2})");
        assertOutput("<@nvo a=x! b=2 />",
                "#nvo(nVarargs={\"a\": \"\", \"b\": 2})");
        assertOutput("<@nvo a=x!b=2 />",
                "#nvo(nVarargs={\"a\": \"\", \"b\": 2})");

        assertOutput("<@nvo a=(1)b=2 />",
                "#nvo(nVarargs={\"a\": 1, \"b\": 2})");
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void testRuntimeErrors() throws IOException, TemplateException {
        assertErrorContains("<@p 9, 9, 9 />", "can only have 2", "3", "by position");
        assertErrorContains("<@n 9 />", "can't have arguments passed by position");
        assertErrorContains("<@n n3=9 />", "has no", "\"n3\"", "supported", "\"n1\", \"n2\"");
        assertErrorContains("<@p n1=9 />", "directive", "can't have arguments that are passed by name", "\"n1\"");
        assertErrorContains("<@uc n1=9 />", "directive", "doesn't support any parameters");
        assertErrorContains("<@uc 9 />", "directive", "doesn't support any parameters");

        addToDataModel("tncp", TwoNestedContentParamsDirective.INSTANCE);
        assertErrorContains("<@tncp />", " no ", " 2 ");
        assertErrorContains("<@tncp ; i>${i}</@>", " 1 ", "\"i\"", " 2 ");
        assertOutput("<@tncp ; i, j>${i} ${j}</@>", "1 2");
        assertErrorContains("<@tncp ; i, j, k>${i}</@>", " 3 ", "\"i\", \"j\", \"k\"", " 2 ");

        assertOutput("<@p></@p>",
                "#p(p1=null, p2=null)");
        assertErrorContains("<@p> </@p>", "Nested content", "not supported");
    }

    @Test
    public void testMacros() throws IOException, TemplateException {
        assertOutput("<#macro m>text</#macro><@m /> <@m />",
                "text text");
        assertOutput("<#macro m a>text ${a}</#macro><@m a=1 /> <@m a=2 />",
                "text 1 text 2");
        assertOutput("<#macro m a b=2>text ${a} ${b}</#macro><@m a=1 /> <@m a=11 b=22 />",
                "text 1 2 text 11 22");
        assertOutput("<#macro m a{positional}>text ${a}</#macro><@m 1 /> <@m 2 />",
                "text 1 text 2");
        assertOutput("<#macro m a{positional}, b{positional}=2>text ${a} ${b}</#macro><@m 1 /> <@m 11, 22 />",
                "text 1 2 text 11 22");
        assertOutput("<#macro m a{positional}, b{positional}=2 c d=4>text ${a} ${b} ${c} ${d}</#macro>"
                        + "<@m 1 c=3 /> <@m 11, 22 c=33 d=44 />",
                "text 1 2 3 4 text 11 22 33 44");

        assertOutput("<#macro m>[<#nested>]</#macro><@m>text</@m>",
                "[text]");
        assertOutput("<#macro m>[<#nested 1>, <#nested 2>]</#macro><@m ; i>text ${i}</@m>",
                "[text 1, text 2]");
        assertOutput("<#macro m>[<#nested 1, 2>]</#macro><@m ; i, j>${i} ${j}</@m>",
                "[1 2]");
        assertOutput("<#macro m a b=22><#list 1..2 as n>[<#nested a * n, b * n>]</#list></#macro>"
                + "<@m a=11; i, j>${i} ${j}</@m> <@m a=1 b=2; i, j>${i} ${j}</@m>",
                "[11 22][22 44] [1 2][2 4]");
        assertOutput("<#macro m1><#local x = 1>${x}{<@m2>${x}<<#list [3] as x>${x}</#list>>${x}</@m2>}${x}</#macro>"
                        + "<#macro m2><#local x = 2>${x}[<#nested>]${x}</#macro>"
                        + "<#assign x = 0>${x}(<@m1 />)${x}",
                "0(1{2[1<3>1]2}1)0");
        assertOutput("<#macro m1>"
                        + "<#local x = 0>${x} <#list [1, 2] as x>${x}{<@m2>${x}</@m2>}${x}<#sep> </#list> ${x}"
                        + "</#macro>"
                        + "<#macro m2><#list [3, 4] as x>${x}[<#nested>]${x}<#sep> </#list></#macro>"
                        + "<@m1 />",
                "0 1{3[1]3 4[1]4}1 2{3[2]3 4[2]4}2 0");

        assertOutput("<#macro m a b=0 others...>["
                        + "a=${a}, b=${b}"
                        + "<#list others>, <#items as k, v>${k}=${v}<#sep>, </#items></#list>"
                        + "]</#macro>"
                        + "<@m a=1 /> <@m a=1 b=2 /> <@m a=1 b=2 c=3 d=4 />",
                "[a=1, b=0] [a=1, b=2] [a=1, b=2, c=3, d=4]");
        assertOutput("<#macro m a{positional}, b{positional}=0, others{positional}...>["
                        + "a=${a}, b=${b}"
                        + "<#list others>, <#items as v>${v}<#sep>, </#items></#list>"
                        + "]</#macro>"
                        + "<@m 1 /> <@m 1, 2 /> <@m 1, 2, 3, 4 />",
                "[a=1, b=0] [a=1, b=2] [a=1, b=2, 3, 4]");
        assertOutput("<#macro m pVarargs{positional}... nVarargs...>"
                        + "[<#list pVarargs as v>${v}<#sep>, </#list>]"
                        + "{<#list nVarargs as k, v>${k}=${v}<#sep>, </#list>}"
                        + "</#macro>"
                        + "<@m 1, 2 a=1 b=2 /> <@m 1, 2 /> <@m a=1 b=2 />",
                "[1, 2]{a=1, b=2} [1, 2]{} []{a=1, b=2}");

        assertOutput("<#macro m x{positional}, y{positional}><#local y++><#local z = x + y>${x} + ${y} = ${z}</#macro>"
                        + "<@m 1, 2 />",
                "1 + 3 = 4");

        // Default expression sees previous argument:
        assertOutput("<#macro m a{positional} b=a>${a}${b}</#macro><@m 1/> <@m 2 b=3/>", "11 23");

        addTemplate("lib.ftl", ""
                + "<#assign defaultA=1>"
                + "<#assign b=2>"
                + "<#macro m a=defaultA>${a} ${b}[<#nested>]${b} ${a}</#macro>");
        assertOutput("<#import 'lib.ftl' as lib>"
                + "<#assign a='a'>"
                + "<#assign b='b'>"
                + "<@lib.m>${a}${b}</@> "
                + "<@lib.m a=3>${a}${b}</@>"
                + "", "1 2[ab]2 1 3 2[ab]2 3");
    }

    @Test
    public void testFilterDirective() throws IOException, TemplateException {
        addToDataModel("uc", UpperCaseDirective.INSTANCE);
        assertOutput("<@uc>foo ${1 + 1}</@>", "FOO 2");
    }

}
