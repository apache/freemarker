/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.freemarker.converter;

import static java.nio.charset.StandardCharsets.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.freemarker.converter.ConverterException;
import org.apache.freemarker.converter.FM2ToFM3Converter;
import org.apache.freemarker.converter.UnconvertableLegacyFeatureException;
import org.freemarker.converter.test.ConverterTest;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;

public class FM2ToFM3ConverterTest extends ConverterTest {

    protected void createSourceFiles() throws IOException {
        //
    }

    @Test
    public void testMixed() throws IOException, ConverterException {
        assertConvertedSame("s1\n  <#if t>\n    ${var}\n  </#if>\ns2");
    }

    @Test
    public void testLiterals() throws IOException, ConverterException {
        assertConvertedSame("${''}");
        assertConvertedSame("${'s'}");
        assertConvertedSame("${\"\"}");
        assertConvertedSame("${\"s\"}");
        assertConvertedSame("${\"\\\"'\"}");
        assertConvertedSame("${'\"\\''}");
        assertConvertedSame("${'1${x + 1 + \\'s\\'}2'}");
        assertConvertedSame("${\"s ${'x $\\{\\\"y\\\"}'}\"}");
        assertConvertedSame("${'${1}${2}'}");
        assertConvertedSame("<@m x='${e1 + \"a\\'b$\\{x}\"}' y='$\\{e2}' />");
        assertConvertedSame("${\"&<>\\\"'{}\\\\a/\"}");
        assertConvertedSame("${\"${x}&<>\\\"'{}${x}\\\\a/${x}\"}");

        assertConvertedSame("${r'${1}'}");

        assertConvertedSame("${1}");
        assertConvertedSame("${0.5}");
        assertConvertedSame("${-1.5}");

        assertConvertedSame("${true}");
        assertConvertedSame("${false}");

        assertConvertedSame("${f([])}");
        assertConvertedSame("${f([ <#-- C --> ])}");
        assertConvertedSame("${f([1])}");
        assertConvertedSame("${f([1, [x,y], 3])}");
        assertConvertedSame("${f([<#--1--> 1, <#--2--> 2, <#--3--> 3 <#--4-->])}");

        assertConvertedSame("${f({})}");
        assertConvertedSame("${f({k: v})}");
        assertConvertedSame("${f({k1: v1, k2: v2, 'k3': 33})}");
        assertConvertedSame("${f({ <#--1--> k1 <#--2--> : <#--3--> v1 <#--4-->,k2:v2 <#--5-->})}");

        assertConvertedSame("${f(1 .. 9)}");
        assertConvertedSame("${f(1 ..* 9)}");
        assertConvertedSame("${f(1 ..! 9)}");
        assertConvertedSame("${f(1 ..< 9)}");
        assertConvertedSame("${f(1 ..)}");
        assertConvertedSame("${f(1<#--1-->..\t<#--2-->9)}");
    }

    @Test
    public void testOtherExpressions() throws IOException, ConverterException {
        assertConvertedSame("${x + 1\r\n\t- y % 2 / 2 * +z / -1}");
        assertConvertedSame("${x * (y + z) * (\ty+z\n)}");

        assertConvertedSame("${f()}");
        assertConvertedSame("${f(1)}");
        assertConvertedSame("${f(1, 2)}");
        assertConvertedSame("${f<#--1-->(<#--2--> 1, 2 ,<#--3--> 3,<#--4-->4 <#--5-->)}");

        assertConvertedSame("${m[key]}");
        assertConvertedSame("${m['key']}");
        assertConvertedSame("${m <#--1--> [ <#--2--> key <#--3--> ]}");
        assertConvertedSame("${seq\\-foo[1]}");

        assertConvertedSame("${m.key}");
        assertConvertedSame("${m <#--1--> . <#--3--> key}");
        assertConvertedSame("${m.@key}");
        assertConvertedSame("${m.*}");
        assertConvertedSame("${m.**}");

        assertConvertedSame("${.outputFormat}");
        assertConvertedSame("${. <#-- C --> outputFormat}");
        assertConverted("${.outputFormat}","${.output_format}");
        assertConverted("${.node}","${.current_node}");

        assertConvertedSame("${a < b}${a <= b}${(a > b)}${(a >= b)}${a == b}${a != b}");
        assertConvertedSame("${a<#--1--><<#--2-->b}${a<#--3--><=<#--4-->b}"
                + "${(a<#--7-->><#--8-->b)}${(a<#--9-->>=<#--A-->b)}"
                + "${a<#--B-->==<#--C-->b}${a<#--D-->!=<#--E-->b}");
        // "Same" for now, will be different later.
        assertConvertedSame("${a = b}${a == b}");
        assertConvertedSame("${a &lt; b}${a lt b}${a \\lt b}");
        assertConvertedSame("${a &lt;= b}${a lte b}${a \\lte b}");
        assertConvertedSame("${a &gt; b}${a gt b}${a \\gt b}");
        assertConvertedSame("${a &gt;= b}${a gte b}${a \\gte b}");

        // [FM3] Add \and and &amp;&amp; tests when 2.3.27 is released
        assertConvertedSame("${a && b}${a & b}${a || b}${a | b}");
        assertConvertedSame("${a<#--1-->&&<#--2-->b}${a<#--3-->&<#--4-->b}"
                + "${a<#--5-->||<#--6-->b}${a<#--7-->|<#--8-->b}");

        assertConvertedSame("${!a}${! foo}${! <#--1--> bar}${!!c}");

        assertConvertedSame("${a!} ${a!0}");
        assertConvertedSame("${a <#--1--> !} ${a <#--2--> ! <#--3--> 0}");
        assertConvertedSame("${a!b.c(x!0, y!0)}");
        assertConvertedSame("${(a.b)!x}");
        // [FM3] Will be: a!(x+1)
        assertConvertedSame("${a!x+1}");

        assertConvertedSame("${a??} ${a <#--1--> ??}");
    }

    @Test
    public void testInterpolations() throws IOException, ConverterException {
        assertConvertedSame("${n}");
        assertConvertedSame("${  n\n}");

        assertConverted("${n}", "#{n}");
        assertConverted("${n?string('0.00')}", "#{n; m2}");
        assertConverted("${n?string('0.###')}", "#{n; M3}");
        assertConverted("${n?string('0.00###')}", "#{n; m2M5}");
        assertConverted("${n + 1}", "#{n + 1}");
        assertConverted("${(n + 1)?string('0.00')}", "#{n + 1; m2}");
        assertConverted("${(n * m)?string('0.00')}", "#{n * m; m2}");
        assertConverted("${(-n)?string('0.00')}", "#{-n; m2}");
        assertConverted("${a.b?string('0.00')}", "#{a.b; m2}");
        assertConverted("${f()?string('0.00')}", "#{f(); m2}");
        assertConverted("${m[k]?string('0.00')}", "#{m[k]; m2}");
        assertConverted("${n?abs?string('0.00')}", "#{n?abs; m2}");

        assertConverted("${  n  }", "#{  n  }");
        assertConverted("${  n?string('0.00')}", "#{  n ; m2}");
    }

    @Test
    public void testCoreDirectives() throws IOException, ConverterException {
        assertConvertedSame("<#if foo>1</#if>");
        assertConvertedSame("<#if\n  foo\n>\n123\n</#if\n>");

        assertConverted("<#if foo>1<#elseIf bar>2<#else>3</#if>", "<#if foo>1<#elseif bar>2<#else>3</#if>");
        assertConvertedSame("<#if  foo >1<#elseIf  bar >2<#else >3</#if >");
        assertConverted("<#if foo>1<#elseIf bar>2<#else>3</#if>", "<#if foo>1<#elseif bar/>2<#else/>3</#if>");
        assertConverted("<#if foo>1<#elseIf bar>2<#else>3</#if>", "<#if foo>1<#elseif bar />2<#else />3</#if>");

        assertConvertedSame("<#macro m>body</#macro>");
        assertConvertedSame("<#macro <#--1--> m <#--2-->></#macro >");
        assertConvertedSame("<#macro m()></#macro>");
        assertConvertedSame("<#macro m <#--1--> ( <#--2--> ) <#--3--> ></#macro>");
        assertConvertedSame("<#macro m p1></#macro>");
        assertConvertedSame("<#macro m(p1)></#macro>");
        assertConvertedSame("<#macro m p1 p2 p3></#macro>");
        assertConvertedSame("<#macro m p1 <#--1--> p2 <#--2--> p3 <#--3-->></#macro>");
        assertConvertedSame("<#macro m(p1<#--1-->,<#--2--> p2<#--3-->,<#--4-->"
                + " p5<#--5-->)<#--6-->></#macro>");
        assertConvertedSame("<#macro m p1=11 p2=foo p3=a+b></#macro>");
        assertConvertedSame("<#macro m(p1=11, p2=foo, p3=a+b)></#macro>");
        assertConvertedSame("<#macro m p1<#--1-->=<#--2-->11<#--3-->,<#--4-->p2=22></#macro>");
        assertConvertedSame("<#macro m others...></#macro>");
        assertConvertedSame("<#macro m p1 others...></#macro>");
        assertConvertedSame("<#macro m p1 p2=22 others...></#macro>");
        assertConvertedSame("<#macro m(others...)></#macro>");
        assertConvertedSame("<#macro m(others <#--1--> ... <#--2--> )></#macro>");
        assertConvertedSame("<#function f x y><#return x + y></#function>");
        assertConvertedSame("<#function f(x, y=0 <#--0-->)><#return <#--1--> x + y <#--2-->></#function>");
        assertConvertedSame("<#macro m\\-1 p\\-1></#macro>");
        // Only works with " now, as it doesn't keep the literal kind. Later we will escape differently anyway:
        assertConvertedSame("<#macro \"m 1\"></#macro>");
        assertConvertedSame("<#macro m><#nested x + 1, 2, 3></#macro>");
        assertConvertedSame("<#macro m><#nested <#--1--> x + 1 <#--2-->, <#--3--> 2 <#--4-->></#macro>");
        // [FM3] Will be different (comma)
        assertConvertedSame("<#macro m><#nested x + 1 2 3></#macro>");
        assertConvertedSame("<#macro m><#nested <#--1--> x + 1 <#--2--> 2 <#--3-->></#macro>");
        assertConvertedSame("<#macro m><#nested x /></#macro>");
        assertConvertedSame("<#macro m><#return><#return ></#macro>");

        assertConvertedSame("<#assign x = 1>");
        assertConvertedSame("<#global x = 1>");
        assertConvertedSame("<#macro m><#local x = 1></#macro>");
        assertConvertedSame("<#assign x = 1 in someNs>");
        assertConvertedSame("<#assign <#--1--> x <#--2--> = <#--3--> 1 <#--4--> in <#--5--> someNs <#--6-->>");
        assertConvertedSame("<#assign x=1 y=2,z=3>");
        assertConvertedSame("<#assign x += 1>");
        assertConvertedSame("<#assign x *= 2>");
        assertConvertedSame("<#assign x-->");
        assertConvertedSame("<#global x = 1, y++, z /= 2>");
        assertConvertedSame("<#assign x = 1 y++ z /= 2>");
        assertConvertedSame("<#assign <#--0-->x = 1<#--1-->,<#--2-->y++<#--3-->z/=2<#--4-->>");
        // Only works with " now, as it doesn't keep the literal kind. Later we will escape differently anyway:
        assertConvertedSame("<#assign \"x y\" = 1>");
        assertConvertedSame("<#assign x = 1/>");

        assertConvertedSame("<#assign x>t</#assign>");
        assertConvertedSame("<#assign x in ns>t</#assign>");
        assertConvertedSame("<#assign x\\-y>t</#assign>");
        assertConvertedSame("<#assign \"x y\">t</#assign>");
        assertConvertedSame("<#global x>t</#global>");
        assertConvertedSame("<#macro m><#local x>t</#local></#macro>");
        assertConvertedSame("<#assign <#--1--> x <#--2--> in <#--3--> ns <#--4-->>t</#assign >");

        assertConvertedSame("<#attempt>a<#recover>r</#attempt>");
        assertConvertedSame("<#attempt >a<#recover  >r</#attempt   >");
        assertConverted("<#attempt>a<#recover>r</#attempt>", "<#attempt>a<#recover>r</#recover>");
        assertConverted("<#attempt >a<#recover  >r</#attempt   >", "<#attempt >a<#recover  >r</#recover   >");

        assertConvertedSame("<#ftl>");
        assertConvertedSame("[#ftl]"); // To test when the tag syntax is overridden by #ftl
        assertConvertedSame("<#ftl>x");
        assertConvertedSame("<#ftl>x${x}");
        assertConvertedSame("<#ftl>\nx${x}");
        assertConvertedSame("\n\n  <#ftl>\n\nx");
        assertConverted("<#ftl outputFormat='HTML'>x", "<#ftl output_format='HTML'>x");
        assertConverted("<#ftl encoding='utf-8' customSettings={'a': [1, 2, 3]}>",
                "<#ftl encoding='utf-8' attributes={'a': [1, 2, 3]}>");
        assertConverted("<#ftl <#--1-->\n\tencoding='utf-8' <#--2-->\n\tcustomSettings={'a': [1, 2, 3]} <#--3-->\n>",
                "<#ftl <#--1-->\n\tencoding='utf-8' <#--2-->\n\tattributes={'a': [1, 2, 3]} <#--3-->\n>");

        assertConvertedSame("<#ftl outputFormat='XML'><#noAutoEsc><#autoEsc>${x}</#autoEsc></#noAutoEsc>");
        assertConvertedSame("<#ftl outputFormat='XML'><#noAutoEsc ><#autoEsc\t>${x}</#autoEsc\n></#noAutoEsc\r>");
        assertConverted(
                "<#ftl outputFormat='XML'><#noAutoEsc>${x}</#noAutoEsc>",
                "<#ftl output_format='XML'><#noautoesc>${x}</#noautoesc>");

        assertConvertedSame("<#compress>x</#compress>");
        assertConvertedSame("<#compress >x</#compress  >");

        assertConvertedSame("<#escape x as x?html><#noEscape>${v}</#noEscape></#escape>");
        assertConvertedSame("<#escape <#--1--> x <#--2--> as <#--3--> x?html <#--4--> >"
                + "<#noEscape >${v}</#noEscape >"
                + "</#escape >");
        assertConvertedSame("<#flush>");
        assertConvertedSame("<#flush >");

        assertConvertedSame("<#import '/lib/foo.ftl' as foo >");
        assertConvertedSame("<#import <#--1--> '/lib/foo.ftl' <#--2--> as <#--3--> foo <#--4--> >");

        assertConvertedSame("<#include 'foo.ftl'>");
        assertConverted("<#include 'foo.ftl' ignoreMissing=true>", "<#include 'foo.ftl' ignore_missing=true>");
        assertTrue(lastConversionMarkersFileContent.isEmpty());
        assertConverted("<#include 'foo.ftl' ignoreMissing=true>",
                "<#include 'foo.ftl' ignore_missing=true encoding='utf-8' parse=false>");
        assertLastConversionMarkersFileContains("[WARN]", "encoding", "parse");
        assertConverted("<#include 'foo.ftl' ignoreMissing=true>",
                "<#include 'foo.ftl' encoding='utf-8' ignore_missing=true parse=false>");
        assertConverted("<#include 'foo.ftl' ignoreMissing=true>",
                "<#include 'foo.ftl' encoding='utf-8' parse=false ignore_missing=true>");
        assertConvertedSame("<#include <#--1--> 'foo.ftl' <#--2--> >");
        assertConvertedSame("<#include <#--1--> 'foo.ftl' <#--2--> ignoreMissing=true <#--3--> >");
        assertConverted("<#include <#--1--> 'foo.ftl' <#--2-->>",
                "<#include <#--1--> 'foo.ftl' <#--2--> parse=true <#--3--> >");
        assertConverted("<#include <#--1--> 'foo.ftl' <#--2--> ignoreMissing=true <#--3-->>",
                "<#include <#--1--> 'foo.ftl' <#--2--> ignoreMissing=true <#--3--> parse=true <#--4--> >");
        assertConverted("<#include <#--1--> 'foo.ftl' <#--2--> ignoreMissing=true <#--4-->>",
                "<#include <#--1--> 'foo.ftl' <#--2--> encoding='UTF-8' <#--3--> ignoreMissing=true <#--4--> "
                        + "parse=true <#--5--> >");

        assertConvertedSame("<#list xs as x>${x}</#list>");
        assertConvertedSame("<#list <#--1--> xs <#--2--> as <#--3--> x <#--4--> >${x}</#list >");
        assertConvertedSame("<#list xs as k, v>${k}${v}</#list>");
        assertConvertedSame("<#list <#--1--> xs <#--2--> as <#--3--> k <#--4-->, v <#--5--> >${k}${v}</#list >");

        assertConverted("<#list xs as x>${x}</#list>", "<#foreach x in xs>${x}</#foreach>");
        assertConverted(
                "<#list <#--1--> xs <#--XS--> as x <#--X-->>${x}</#list>",
                "<#foreach <#--1--> x <#--X--> in xs <#--XS-->>${x}</#foreach>");

        assertConvertedSame("<#list xs as x>${x}<#sep>, </#list>");
        assertConvertedSame("<#list xs as x>${x}<#sep>, </#sep></#list>");
        assertConvertedSame("<#list xs as x><#sep></#list>");

        assertConvertedSame("<#list xs as x>${x}<#else>-</#list>");
        assertConvertedSame("<#list xs as x>${x}<#else >-</#list >");
        assertConverted("<#list xs as x>${x}<#else>-</#list>", "<#list xs as x>${x}<#else/>-</#list>");

        assertConvertedSame("<#list xs>[<#items as x>${x}<#sep>, </#items>]</#list>");
        assertConvertedSame("<#list xs>[<#items as <#--1--> x <#--2-->>${x}<#sep>, </#items>]</#list>");
        assertConvertedSame("<#list xs>[<#items as k, v>${h}${v}<#sep>, </#items>]</#list>");
        assertConvertedSame(
                "<#list xs>[<#items as <#--1--> k <#--2-->, <#--3--> v <#--4-->>${h}${v}<#sep>, </#items>]</#list>");
        assertConvertedSame("<#list xs as x><#if x == 0><#break></#if>${x}</#list>");
        assertConvertedSame("<#list xs>[<#items  as x>${x}<#sep>, </#sep >|</#items>]<#else>-</#list>");

        assertConvertedSame("<#noParse><#foo>${1}<#----></#noParse>");
        assertConverted("<#noParse >t</#noParse >", "<#noparse >t</#noparse >");

        assertConvertedSame("<#assign x = 1><#t>");
        assertConvertedSame("a<#t>\nb");
        assertConvertedSame("<#t><#nt><#lt><#rt>");
        assertConvertedSame("<#t ><#nt ><#lt ><#rt >");
        assertConvertedSame("<#t><#nt><#lt><#rt>");

        assertConvertedSame("<#ftl stripText='true'>\n\n<#macro m>\nx\n</#macro>\n");

        assertConvertedSame("<#setting <#--1--> numberFormat <#--2--> = <#--3--> '0.0' <#--4-->>");
        assertConverted("<#setting numberFormat='0.0' />", "<#setting number_format='0.0' />");
        try {
            convert("x<#setting classic_compatible=true>");
            fail();
        } catch (UnconvertableLegacyFeatureException e) {
            assertEquals(1, (Object) e.getRow());
            assertEquals(2, (Object) e.getColumn());
        }

        assertConvertedSame("<#stop>");
        assertConvertedSame("<#stop />");
        assertConvertedSame("<#stop 'Reason'>");
        assertConvertedSame("<#stop <#--1--> 'Reason' <#--2-->>");

        assertConvertedSame(""
                + "<#switch x>\n"
                + "  <#--1-->\n"
                + "  <#case 1>one<#break>\n"
                + "  <#--2-->\n"
                + "  <#case 3>one<#break />\n"
                + "  <#case 3>fall through<#case 4>three<#break>\n"
                + "  <#default>def\n"
                + "</#switch>");
        assertConvertedSame(""
                + "<#switch x>\n"
                + "  <#--1-->\n"
                + "</#switch>");
        assertConvertedSame("<#switch x> </#switch>");
        assertConvertedSame("<#switch x><#-- Empty --></#switch>");
        assertConverted("<#switch x> <#case 2> </#switch>", "<#switch x> <#case 2> </#switch>");
        try {
            convert("<#switch x><#default><#case 1></#switch>");
            fail();
        } catch (UnconvertableLegacyFeatureException e) {
            assertEquals(1, (Object) e.getRow());
            assertEquals(22, (Object) e.getColumn());
        }

        assertConvertedSame("<#visit node>");
        assertConvertedSame("<#visit <#--1--> node <#--2-->>");
        assertConvertedSame("<#visit node using ns>");
        assertConvertedSame("<#visit node <#--1--> using <#--2--> ns <#--3-->>");
        assertConvertedSame("<#recurse node>");
        assertConvertedSame("<#recurse <#--1--> node <#--2-->>");
        assertConvertedSame("<#recurse node using ns>");
        assertConvertedSame("<#recurse node <#--1--> using <#--2--> ns <#--3-->>");
        assertConvertedSame("<#recurse>");
        assertConvertedSame("<#recurse <#--1-->>");
        assertConvertedSame("<#recurse <#--1--> using <#--2--> ns>");
        assertConvertedSame("<#macro m><#fallback></#macro>");
        assertConvertedSame("<#macro m><#fallback /></#macro>");

        assertConvertedSame("<#outputFormat 'HTML'>${x}</#outputFormat>");
    }

    @Test
    public void testLegacyDirectives() throws IOException, ConverterException {
        assertConverted("<#--<#bar>-->", "<#comment><#bar></#comment>");
        try {
            convert("x<#comment>--></#comment>");
            fail();
        } catch (UnconvertableLegacyFeatureException e) {
            assertEquals(1, (Object) e.getRow());
            assertEquals(2, (Object) e.getColumn());
        }

        assertConverted("<@m 1, 2, 3/>", "<#call m(1, 2, 3)>");
        assertConverted("<@m/>", "<#call m()>");
        assertConverted("<@m/>", "<#call m>");
        assertConverted("<@m a=1 b=2/>", "<#call m a=1 b=2>");

        assertConverted("<@t a=1 b=2>x</@t>", "<#transform t a=1 b=2>x</#transform>");
        assertConverted("<@n.t>x</@n.t>", "<#transform n.t>x</#transform>");
        assertConverted("<@f()>x</@>", "<#transform f()>x</#transform>");
    }

    @Test
    public void testUserDirectives() throws IOException, ConverterException {
        assertConvertedSame("<@foo/>");
        assertConvertedSame("<@foo />");
        assertConvertedSame("<@foo\\-bar />");
        assertConvertedSame("<@foo></@foo>");
        assertConvertedSame("<@foo\\-bar >t</@foo\\-bar>");
        assertConvertedSame("<@foo\\-bar >t</@>");
        assertConvertedSame("<@foo x=1 y=2 />");
        assertConvertedSame("<@foo x\\-y=1 />");
        assertConvertedSame("<@foo\n\tx = 1\n\ty = 2\n/>");
        assertConvertedSame("<@foo 1 2 />");
        assertConvertedSame("<@foo <#--1--> 1 <#--2--> 2 <#--3--> />");
        assertConvertedSame("<@foo 1, 2 />");
        assertConvertedSame("<@foo <#--1--> 1 <#--2-->, <#--3--> 2 <#--4--> />");
        assertConvertedSame("<@foo x=1; i, j></@>");
        assertConvertedSame("<@foo 1; i, j></@>");
        assertConvertedSame("<@foo 1 2; i\\-2, j></@>");
        assertConvertedSame("<@foo x=1 y=2; i></@>");
        assertConvertedSame("<@foo x=1 ;\n    i <#-- C0 --> , <#--1-->\n\t<!-- C2 --> j <#--3-->\n></@>");
    }

    @Test
    public void testBuiltInExpressions() throws IOException, ConverterException {
        assertConverted("${s?upperCase} ${s?leftPad(123)}", "${s?upper_case} ${s?left_pad(123)}");
        assertConverted("${s?html}", "${s?web_safe}");
        assertConvertedSame("${s  ?   upperCase\t?\t\tleftPad(5)}");
        assertConvertedSame("${s <#--1--> ? <#--2--> upperCase}");
        // Runtime params:
        assertConvertedSame("${s?leftPad(9)}");
        // Parse time params:
        assertConvertedSame("${s?then(1, 2)}");
        assertConvertedSame("${s?switch(1, 'one', 2, 'two', 'more')}");
        assertConvertedSame("${s?then <#--1--> ( <#--2--> 1 <#--3-->, <#--5--> 2 <#--6--> )}");
    }

    @Test
    public void testTagEndCharGlitch() throws IOException, ConverterException {
        assertConverted("<#assign x = 1>x", "<#assign x = 1]x");
        assertConverted("<#if x[0] == 1>x<#else>y</#if>", "<#if x[0] == 1]x<#else]y</#if]");
        assertConverted("<@m x[0]>x</@m>", "<@m x[0]]x</@m]");
        assertConverted("<#ftl customSettings={'a': []}>x", "<#ftl attributes={'a': []}]x");
    }

    /** Tests if the names of all current FM2 built-ins can be converted to FM3 names. */
    @Test
    public void testBuiltInNameConversion()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException,
            ConverterException {
        Configuration cfg = new Configuration(Configuration.getVersion());

        StringBuilder sb = new StringBuilder();
        sb.append("<#outputformat 'HTML'><#list xs as x>");
        for (String builtInName : cfg.getSupportedBuiltInNames(Configuration.LEGACY_NAMING_CONVENTION)) {
            if (!LEGACY_ESCAPING_BUTILT_INS.contains(builtInName)) {
                sb.append("${x?").append(builtInName).append("(1, 2)").append("}");
            }
        }
        sb.append("</#list></#outputformat>");
        for (String builtInName : LEGACY_ESCAPING_BUTILT_INS) {
            sb.append("${x?").append(builtInName).append("}");
        }

        convert(sb.toString());
    }

    @Test
    public void testComments() throws IOException, ConverterException {
        assertConvertedSame("\n<#--\n  c\n\t-->\n");
        assertConvertedSame("${1 + <#--1-->\r\n2 +[#-- C2 --]3 +<!--\tC3\t-->4 +[!-- C4 --] 5 + -<!-- -->1}");
    }

    @Test
    public void testSquareBracketTagSyntax() throws IOException, ConverterException {
        assertConverted("[#if true <#-- c -->[#-- c --]]${v}[#else][/#if]",
                "[#if true <#-- c -->[#-- c --]]${v}[#else/][/#if]", true);
        assertConverted("[#ftl][#if true <#-- c -->[#-- c --]]${v}[#else][/#if]",
                "[#ftl][#if true <#-- c -->[#-- c --]]${v}[#else/][/#if]");
    }

    @Test
    public void testXmlProcessing() throws IOException, ConverterException {
        assertConverted("${node.@@nestedMarkup}", "${node.@@nested_markup}");
        assertConverted("${node['@@nestedMarkup']}", "${node['@@nested_markup']}");

        assertConvertedSame("${node.@@markup}");
        assertConvertedSame("${node['@@markup']}");
    }

    @Test
    public void testDefaultIncludes() throws IOException, ConverterException {
        FileUtils.write(new File(srcDir, "t.txt"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t.fm"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t.ftl"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t.ftlfoo"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "U.FTLH"), "x", UTF_8);

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        assertFalse(new File(dstDir, "t.txt").exists());
        assertTrue(new File(dstDir, "t.fm3").exists());
        assertTrue(new File(dstDir, "t.fm3").exists());
        assertFalse(new File(dstDir, "t.ftlfoo").exists());
        assertTrue(new File(dstDir, "U.fm3h").exists());
    }

    @Test
    public void testFileExtensionConversion() throws IOException, ConverterException {
        FileUtils.write(new File(srcDir, "t1"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t2.foo"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t3.ftl"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t4.ftlh"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t5.ftlx"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t6.ftl"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t7.ftlh"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t8.ftlx"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t9.Ftl"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t10.Foo3"), "x", UTF_8);

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.setInclude(null);
        converter.execute();

        assertTrue(new File(dstDir, "t1").exists());
        assertTrue(new File(dstDir, "t2.foo").exists());
        assertTrue(new File(dstDir, "t3.fm3").exists());
        assertTrue(new File(dstDir, "t4.fm3h").exists());
        assertTrue(new File(dstDir, "t5.fm3x").exists());
        assertTrue(new File(dstDir, "t6.fm3s").exists());
        assertTrue(new File(dstDir, "t7.fm3sh").exists());
        assertTrue(new File(dstDir, "t8.fm3sx").exists());
        assertTrue(new File(dstDir, "t9.fm3").exists());
        assertTrue(new File(dstDir, "t10.Foo3").exists());
    }

    @Test
    public void testOutputFormatSet() throws IOException, ConverterException {
            File srcFile = new File(srcDir, "t.ftlh");
            FileUtils.write(srcFile, "${x?esc}", UTF_8);

            FM2ToFM3Converter converter = new FM2ToFM3Converter();
            converter.setSource(srcFile);
            converter.setDestinationDirectory(dstDir);
            converter.execute();
    }

    @Test
    public void testCharset() throws IOException, ConverterException {
        FileUtils.write(new File(srcDir, "t1.ftl"),
                "<#ftl encoding='ISO-8859-1'>béka",
                StandardCharsets.ISO_8859_1);
        FileUtils.write(new File(srcDir, "t2.ftl"),
                "béka", Charset.forName("UTF-8"));

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        assertThat(FileUtils.readFileToString(new File(dstDir, "t1.fm3"), StandardCharsets.ISO_8859_1),
                containsString("béka"));
        assertThat(FileUtils.readFileToString(new File(dstDir, "t2.fm3"), UTF_8),
                containsString("béka"));
    }

    private static final Set<String> LEGACY_ESCAPING_BUTILT_INS = ImmutableSet.of(
            "html", "xml", "xhtml", "rtf", "web_safe");

    private void assertConvertedSame(String ftl2) throws IOException, ConverterException {
        assertConverted(ftl2, ftl2);
    }

    private void assertConverted(String ftl3, String ftl2) throws IOException, ConverterException {
        assertEquals(ftl3, convert(ftl2));
    }

    private void assertConvertedSame(String ftl2, boolean squareBracketTagSyntax)
            throws IOException, ConverterException {
        assertConverted(ftl2, ftl2, squareBracketTagSyntax);
    }

    private void assertConverted(String ftl3, String ftl2, boolean squareBracketTagSyntax)
            throws IOException, ConverterException {
        assertEquals(ftl3, convert(ftl2, squareBracketTagSyntax));
    }

    private void assertLastConversionMarkersFileContains(String... parts) {
        for (String part : parts) {
            assertThat(lastConversionMarkersFileContent, containsString(part));
        }
    }

    private String convert(String ftl2) throws IOException, ConverterException {
        return convert(ftl2, false);
    }

    private String lastConversionMarkersFileContent;

    private String convert(String ftl2, boolean squareBracketTagSyntax) throws IOException, ConverterException {
        File srcFile = new File(srcDir, "t");
        FileUtils.write(srcFile, ftl2, UTF_8);

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcFile);
        converter.setDestinationDirectory(dstDir);
        converter.setInclude(null);
        Properties properties = new Properties();
        properties.setProperty(Configuration.DEFAULT_ENCODING_KEY, UTF_8.name());
        if (squareBracketTagSyntax) {
            properties.setProperty(Configuration.TAG_SYNTAX_KEY, "squareBracket");
        }
        converter.setFreeMarker2Settings(properties);

        converter.execute();

        File outputFile = new File(dstDir, "t");
        String output = FileUtils.readFileToString(outputFile, UTF_8);
        if (!outputFile.delete()) {
            throw new IOException("Couldn't delete file: " + outputFile);
        }

        lastConversionMarkersFileContent = readConversionMarkersFile(true);

        return output;
    }

}
