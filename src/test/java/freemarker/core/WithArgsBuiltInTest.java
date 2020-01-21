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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.test.TemplateTest;

public class WithArgsBuiltInTest extends TemplateTest {

    private static final String PRINT_O = "o=<#if o?isSequence>[<#list o as v>${v!'null'}<#sep>, </#list>]" +
            "<#else>{<#list o as k,v>${k}=${v!'null'}<#sep>, </#list>}" +
            "</#if>";

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        cfg.setTemplateLoader(templateLoader);
        templateLoader.putTemplate("callables.ftl", "" +
                // Macro with default:
                "<#macro m a b c='d3'>" +
                "a=${a}; b=${b}; c=${c}" +
                "</#macro>" +
                // Macro with Catch-All:
                "<#macro mCA a b o...>" +
                "a=${a}; b=${b}; " + PRINT_O +
                "</#macro>" +
                // Macro with Catch-All Only:
                "<#macro mCAO o...>" + PRINT_O +
                "</#macro>" +
                // Function with default:
                "<#function f(a, b, c='d3')>" +
                "<#return 'a=${a}; b=${b}; c=${c}'>" +
                "</#function>" +
                // Function with Catch-All:
                "<#function fCA(a, b, o...)>" +
                "<#local r>" +
                "a=${a}; b=${b}; " + PRINT_O +
                "</#local>" +
                "<#return r>" +
                "</#function>" +
                // Function with Catch-All Only:
                "<#function fCAO(o...)>" +
                "<#local r>" + PRINT_O +
                "</#local>" +
                "<#return r>" +
                "</#function>"
        );
        cfg.setAutoIncludes(Collections.singletonList("callables.ftl"));
        return cfg;
    }

    @Test
    public void testMacroWithNamedWithArgs() throws Exception {
        assertOutput("<@m b=2 a=1 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgs({'b': 2, 'a': 1}) />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgs({'b': 2, 'a': 1}) a=11 />", "a=11; b=2; c=d3");
        assertOutput("<@m?withArgs({'b': 2, 'a': 1}) a=11 b=22 />", "a=11; b=22; c=d3");
        assertOutput("<@m?withArgs({'b': 2, 'c': 3}) a=1 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgs({}) b=2 c=3 a=1 />", "a=1; b=2; c=3");

        assertOutput("<@mCA a=1 b=2 />", "a=1; b=2; o={}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2}) />", "a=1; b=2; o={}");
        assertOutput("<@mCA?withArgs({'a': 1}) b=2 />", "a=1; b=2; o={}");
        assertOutput("<@mCA?withArgs({}) a=1 b=2 />", "a=1; b=2; o={}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3}) />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2}) c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs({'a': 1}) b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs({}) a=1 b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA a=1 b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA a=1 b=2 c=3 d=4 />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3, 'd': 4}) />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3, 'd': 4}) b=22 />", "a=1; b=22; o={c=3, d=4}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3, 'd': 4}) b=22 e=5 />", "a=1; b=22; o={c=3, d=4, e=5}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3, 'd': 4}) 11 22 />", "a=11; b=22; o={c=3, d=4}");
        assertOutput("<@mCA?withArgs({'a': 1, 'b': 2}) 11 22 33 />", "a=11; b=22; o=[33]");
        assertErrorContains("<@mCA?withArgs({'a': 1, 'b': 2, 'c': 3}) 11 22 33 />",
                "both named and positional", "catch-all");

        assertOutput("<@mCAO?withArgs({'a': 1, 'b': 2}) />", "o={a=1, b=2}");
        assertOutput("<@mCAO?withArgs({'a': 1}) b=2 />", "o={a=1, b=2}");
        assertOutput("<@mCAO?withArgs({}) a=1 b=2 />", "o={a=1, b=2}");
        assertOutput("<@mCAO a=1 b=2 />", "o={a=1, b=2}");

        assertOutput("<@mCAO />", "o=[]");
        assertOutput("<@mCAO?withArgs({}) />", "o={}");

        assertOutput("<@m b=2 a=1 c=null />", "a=1; b=2; c=d3");
        Map<String, Integer> cNull = new HashMap<>();
        cNull.put("c", null);
        addToDataModel("cNull", cNull);
        assertOutput("<@m?withArgs(cNull) b=2 a=1 />", "a=1; b=2; c=d3");
    }

    @Test
    public void testNullsWithMacroWithNamedWithArgs() throws Exception {
        // Null-s in ?withArgs should behave similarly as if they were given directly as argument.
        assertOutput("<@mCAO a=null b=null />", "o={a=null, b=null}");
        Map<String, Integer> aNullBNull = new LinkedHashMap<>();
        aNullBNull.put("a", null);
        aNullBNull.put("b", null);
        addToDataModel("aNullBNull", aNullBNull);
        assertOutput("<@mCAO?withArgs(aNullBNull) />", "o={a=null, b=null}");

        assertOutput("<@m?withArgs({'a': 11, 'b': 22, 'c': 33}) a=111 b=222 c=null />", "a=111; b=222; c=d3");
        assertErrorContains("<@m?withArgs({'a': 11, 'b': 22, 'c': 33}) a=111 b=null c=333 />", "required", "\"b\"");
        assertOutput("<@mCAO?withArgs({'a': 1, 'b': 2}) a=null b=22 c=33 />", "o={a=null, b=22, c=33}");
    }

    @Test
    public void testMacroWithPositionalWithArgs() throws Exception {
        assertOutput("<@m 1 2 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgs([1, 2]) />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgs([1]) 2 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgs([]) 1 2 />", "a=1; b=2; c=d3");
        assertOutput("<@m 1 2 3 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgs([1, 2, 3]) />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgs([1, 2]) c=3 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgs([1, 2, 0]) c=3 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgs([1, 0, 3]) b=2 />", "a=1; b=2; c=3");

        assertOutput("<@mCA 1 2 />", "a=1; b=2; o=[]");
        assertOutput("<@mCA?withArgs([1, 2]) />", "a=1; b=2; o=[]");
        assertOutput("<@mCA?withArgs([1]) 2 />", "a=1; b=2; o=[]");
        assertOutput("<@mCA?withArgs([]) 1 2 />", "a=1; b=2; o=[]");
        assertOutput("<@mCA 1 2 3 />", "a=1; b=2; o=[3]");
        assertOutput("<@mCA?withArgs([1, 2, 3]) />", "a=1; b=2; o=[3]");
        assertOutput("<@mCA?withArgs([1]) 2, 3 />", "a=1; b=2; o=[3]");
        assertOutput("<@mCA?withArgs([1, 2]) 3 />", "a=1; b=2; o=[3]");
        assertOutput("<@mCA?withArgs([1]) b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs([]) a=1 b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs([1, 2]) c=3 />", "a=1; b=2; o={c=3}");
        assertOutput("<@mCA?withArgs([1, 0]) b=2 c=3 />", "a=1; b=2; o={c=3}");
        assertErrorContains("<@mCA?withArgs([1, 2, 3]) d=4 />",
                "both named and positional", "catch-all");

        assertOutput("<@mCAO?withArgs([1, 2]) />", "o=[1, 2]");
        assertOutput("<@mCAO?withArgs([1]) 2 />", "o=[1, 2]");
        assertOutput("<@mCAO 1, 2 />", "o=[1, 2]");

        assertOutput("<@mCAO?withArgs([]) />", "o=[]");
    }

    @Test
    public void testNullsWithMacroWithPositionalWithArgs() throws Exception {
        // Null-s in ?withArgs should behave similarly as if they were given directly as argument.
        assertOutput("<@mCAO 1 null null 4 />", "o=[1, null, null, 4]");
        addToDataModel("args", Arrays.asList(1, null, null, 4));
        assertOutput("<@mCAO?withArgs(args) />", "o=[1, null, null, 4]");
        assertOutput("<@mCAO?withArgs(args) null 5 6 />", "o=[1, null, null, 4, null, 5, 6]");
    }

    @Test
    public void testFunction() throws Exception {
        assertOutput("${f(1, 2)}", "a=1; b=2; c=d3");
        assertOutput("${f?withArgs([1, 2])()}", "a=1; b=2; c=d3");
        assertOutput("${f?withArgs([1])(2)}", "a=1; b=2; c=d3");
        assertOutput("${f?withArgs([])(1, 2)}", "a=1; b=2; c=d3");
        assertOutput("${f(1, 2, 3)}", "a=1; b=2; c=3");
        assertOutput("${f?withArgs([1, 2, 3])()}", "a=1; b=2; c=3");

        assertOutput("${fCA(1, 2)}", "a=1; b=2; o=[]");
        assertOutput("${fCA?withArgs([1, 2])()}", "a=1; b=2; o=[]");
        assertOutput("${fCA?withArgs([1])(2)}", "a=1; b=2; o=[]");
        assertOutput("${fCA?withArgs([])(1, 2)}", "a=1; b=2; o=[]");
        assertOutput("${fCA(1, 2, 3)}", "a=1; b=2; o=[3]");
        assertOutput("${fCA?withArgs([1, 2, 3])()}", "a=1; b=2; o=[3]");
        assertOutput("${fCA?withArgs([1])(2, 3)}", "a=1; b=2; o=[3]");
        assertOutput("${fCA?withArgs([1, 2])(3)}", "a=1; b=2; o=[3]");
        assertOutput("${fCA?withArgs([])(1, 2, 3)}", "a=1; b=2; o=[3]");

        assertOutput("${fCAO(1, 2)}", "o=[1, 2]");
        assertOutput("${fCAO?withArgs([1, 2])()}", "o=[1, 2]");
        assertOutput("${fCAO?withArgs([1])(2)}", "o=[1, 2]");
        assertOutput("${fCAO?withArgs([])(1, 2)}", "o=[1, 2]");

        assertErrorContains("${f?withArgs({'a': 1, 'b': 2})}",
                "function", "hash", "sequence", "?withArgs");
    }

    @Test
    public void testNullsWithFunction() throws Exception {
        // Null-s in ?withArgs should behave similarly as if they were given directly as argument.
        assertOutput("${fCAO(1, null, null, 4)}", "o=[1, null, null, 4]");
        addToDataModel("args", Arrays.asList(1, null, null, 4));
        assertOutput("${fCAO?withArgs(args)()}", "o=[1, null, null, 4]");
        assertOutput("${fCAO?withArgs(args)(null, 5, 6)}", "o=[1, null, null, 4, null, 5, 6]");
    }

    @Test
    public void testCurrentNamespaceWorks() throws Exception {
        addTemplate("ns1.ftl", "" +
                "<#assign v = 'NS1'>" +
                "<#macro m p>" +
                "p=${p} " +
                "v=${v} " +
                "<#local v = 'L'>" +
                "v=${v} " +
                "{<#nested p>} " +
                "v=${v}" +
                "</#macro>");
        assertOutput("" +
                "<#import 'ns1.ftl' as ns1>" +
                "<#assign v = 'NS0'>" +
                "<@ns1.m 1; n>n=${n} v=${v}</@>; " +
                "<#assign m2 = ns1.m?withArgs([2])>" +
                "<@m2; n>n=${n} v=${v}</@>",
        "p=1 v=NS1 v=L {n=1 v=NS0} v=L; " +
                "p=2 v=NS1 v=L {n=2 v=NS0} v=L");
    }

    @Test
    public void testArgCountCheck() throws Exception {
        String macroDef = "<#macro m a b c>${a}, ${b}, ${c}</#macro>";

        // No error:
        assertOutput(macroDef + "<@m 1 2 3 />", "1, 2, 3");
        assertOutput(macroDef + "<@m?with_args([1, 2, 3]) />", "1, 2, 3");
        assertOutput(macroDef + "<@m?with_args([1, 2]) 3 />", "1, 2, 3");

        // Too many args:
        assertErrorContains(macroDef + "<@m 1 2 3 4 />", "accepts 3", "got 4");
        assertErrorContains(macroDef + "<@m?with_args([1, 2, 3, 4]) />", "accepts 3", "got 4");
        assertErrorContains(macroDef + "<@m?with_args([1, 2, 3]) 5 />", "accepts 3", "got 4");
        assertErrorContains(macroDef + "<@m?with_args([1]) 2 3 4 />", "accepts 3", "got 4");

        // Too few args:
        assertErrorContains(macroDef + "<@m 1 2 />", "\"c\"", "was not specified");
        assertErrorContains(macroDef + "<@m?with_args([1, 2]) />", "\"c\"", "was not specified");
        assertErrorContains(macroDef + "<@m?with_args([1]) 2 />", "\"c\"", "was not specified");
        assertErrorContains(macroDef + "<@m?with_args([]) 1 2 />", "\"c\"", "was not specified");
    }

    @Test
    public void testDefaultsThenCatchAll() throws IOException, TemplateException {
        String macroDef = "<#macro m a=1 b=2 c=3 o...>a=${a} b=${b} c=${c} " + PRINT_O + "</#macro>";

        assertOutput(macroDef + "<@m?withArgs([]) />", "a=1 b=2 c=3 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11]) />", "a=11 b=2 c=3 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11, 22]) />", "a=11 b=22 c=3 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11, 22, 33]) />", "a=11 b=22 c=33 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11, 22, 33, 44]) />", "a=11 b=22 c=33 o=[44]");
        assertOutput(macroDef + "<@m?withArgs([11, 22, 33, 44, 55]) />", "a=11 b=22 c=33 o=[44, 55]");

        assertOutput(macroDef + "<@m?withArgs([]) 11 />", "a=11 b=2 c=3 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11]) 22 />", "a=11 b=22 c=3 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11, 22]) 33 />", "a=11 b=22 c=33 o=[]");
        assertOutput(macroDef + "<@m?withArgs([11, 22, 33]) 44 />", "a=11 b=22 c=33 o=[44]");
        assertOutput(macroDef + "<@m?withArgs([11, 22, 33, 44]) 55 />", "a=11 b=22 c=33 o=[44, 55]");

        assertOutput(macroDef + "<@m?withArgs({}) />", "a=1 b=2 c=3 o={}");
        assertOutput(macroDef + "<@m?withArgs({'b':22}) />", "a=1 b=22 c=3 o={}");
        assertOutput(macroDef + "<@m?withArgs({'b':22, 'c':33}) />", "a=1 b=22 c=33 o={}");
        assertOutput(macroDef + "<@m?withArgs({'b':22, 'c':33, 'd':55}) />", "a=1 b=22 c=33 o={d=55}");
        assertOutput(macroDef + "<@m?withArgs({'b':22, 'd':55, 'e':66}) />", "a=1 b=22 c=3 o={d=55, e=66}");

        assertOutput(macroDef + "<@m?withArgs({}) b=22 />", "a=1 b=22 c=3 o={}");
        assertOutput(macroDef + "<@m?withArgs({'b':22}) c=33 />", "a=1 b=22 c=33 o={}");
        assertOutput(macroDef + "<@m?withArgs({'b':22, 'c':33}) d=55 />", "a=1 b=22 c=33 o={d=55}");
        assertOutput(macroDef + "<@m?withArgs({'b':22, 'd':55}) e=66 />", "a=1 b=22 c=3 o={d=55, e=66}");
    }

    @Test
    public void testMethod() throws IOException, TemplateException {
        addToDataModel("obj", new MethodHolder());

        assertOutput("${obj.m3p(1, 2, 3)}", "1, 2, 3");
        assertOutput("${obj.m3p?withArgs([1, 2, 3])()}", "1, 2, 3");
        assertOutput("${obj.m3p?withArgs([1, 2])(3)}", "1, 2, 3");
        assertOutput("${obj.m3p?withArgs([1])(2, 3)}", "1, 2, 3");
        assertOutput("${obj.m3p?withArgs([])(1, 2, 3)}", "1, 2, 3");

        assertOutput("${obj.m0p()}", "OK");
        assertOutput("${obj.m0p?withArgs([])()}", "OK");

        assertOutput("${obj.mVA(1, 2, 3, 4)}", "1, 2, o=[3, 4]");
        assertOutput("${obj.mVA?withArgs([1, 2, 3, 4])()}", "1, 2, o=[3, 4]");
        assertOutput("${obj.mVA?withArgs([1, 2, 3])(4)}", "1, 2, o=[3, 4]");
        assertOutput("${obj.mVA?withArgs([1, 2])(3, 4)}", "1, 2, o=[3, 4]");
        assertOutput("${obj.mVA?withArgs([1])(2, 3, 4)}", "1, 2, o=[3, 4]");
        assertOutput("${obj.mVA?withArgs([])(1, 2, 3, 4)}", "1, 2, o=[3, 4]");

        assertErrorContains("${obj.mVA?withArgs({})}", "hash", "sequence", "argument");

        assertOutput("${obj.mNullable(null, 2, null)}", "null, 2, null");
        addToDataModel("args", Arrays.asList(null, 2, null));
        assertOutput("${obj.mNullable?withArgs(args)()}", "null, 2, null");
    }

    @Test
    public void testMethodWithArgsLast() throws IOException, TemplateException {
        addToDataModel("obj", new MethodHolder());
        assertOutput("${obj.m3p?withArgsLast([1, 2, 3])()}", "1, 2, 3");
        assertOutput("${obj.m3p?withArgsLast([1, 2])(3)}", "3, 1, 2");
        assertOutput("${obj.m3p?withArgsLast([1])(2, 3)}", "2, 3, 1");
        assertOutput("${obj.m3p?withArgsLast([])(1, 2, 3)}", "1, 2, 3");

        addToDataModel("args", Arrays.asList(null, 2));
        assertOutput("${obj.mNullable?withArgsLast(args)(1)}", "1, null, 2");
    }

    public static class MethodHolder {
        public String m3p(int a, int b, int c) {
            return a + ", " + b + ", " + c;
        }

        public String m0p() {
            return "OK";
        }

        public String mVA(int a, int b, int... others) {
            StringBuilder sb = new StringBuilder()
                    .append(a).append(", ").append(b);
            sb.append(", o=[");
            for (int i = 0; i < others.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(others[i]);
            }
            sb.append("]");
            return sb.toString();
        }

        public String mNullable(Integer a, Integer b, Integer c) {
            return a + ", " + b + ", " + c;
        }
    }

    @Test
    public void testLegacyMethod() throws IOException, TemplateException {
        addToDataModel("legacyMethod", new LegacyMethodModel());
        getConfiguration().setNumberFormat("0.00");
        assertOutput("${legacyMethod(1, '2')}", "[1.00, 2]");
        assertOutput("${legacyMethod?withArgs([1, '2'])()}", "[1.00, 2]");
        assertOutput("${legacyMethod?withArgs([1])('2')}", "[1.00, 2]");
        assertOutput("${legacyMethod?withArgs([])(1, '2')}", "[1.00, 2]");
    }

    private static class LegacyMethodModel implements TemplateMethodModel {
        public Object exec(List arguments) throws TemplateModelException {
            for (Object argument : arguments) {
                if (!(argument instanceof String)) {
                    throw new IllegalArgumentException("Arguments should be String-s");
                }
            }
            return arguments.toString();
        }
    }

    @Test
    public void testTemplateDirectiveModel() throws IOException, TemplateException {
        addToDataModel("directive", new TestTemplateDirectiveModel());

        assertOutput("<@directive a=1 b=2 c=3; u, v>${u} ${v}</@>",
                "{a=1, b=2, c=3}{11 22}");
        assertOutput("<@directive?withArgs({'a': 1, 'b': 2, 'c': 3}); u, v>${u} ${v}</@>",
                "{a=1, b=2, c=3}{11 22}");
        assertOutput("<@directive?withArgs({'a': 1, 'b': 2}) c=3; u, v>${u} ${v}</@>",
                "{a=1, b=2, c=3}{11 22}");
        assertOutput("<@directive?withArgs({'a': 1}) b=2 c=3; u, v>${u} ${v}</@>",
                "{a=1, b=2, c=3}{11 22}");
        assertOutput("<@directive?withArgs({}) a=1 b=2 c=3; u, v>${u} ${v}</@>",
                "{a=1, b=2, c=3}{11 22}");

        assertOutput("<@directive?withArgs({}); u, v>${u} ${v}</@>",
                "{}{11 22}");
        assertOutput("<@directive?withArgs({'a': 1, 'b': 2}) b=22 c=3; u>${u}</@>",
                "{a=1, b=22, c=3}{11}");
        Map<String, Integer> args = new LinkedHashMap<>();
        args.put("a", null);
        args.put("b", 2);
        args.put("c", 3);
        args.put("e", 6);
        addToDataModel("args", args);
        assertOutput("<@directive?withArgs(args) b=22 c=null d=55 />",
                "{a=null, b=22, c=null, e=6, d=55}{}");
    }

    @Test
    public void testTemplateDirectiveModelWithArgsLast() throws IOException, TemplateException {
        addToDataModel("directive", new TestTemplateDirectiveModel());

        Map<String, Integer> args = new LinkedHashMap<>();
        args.put("a", null);
        args.put("b", 2);
        args.put("c", 3);
        args.put("e", 6);
        args.put("f", 7);
        args.put("g", null);
        addToDataModel("args", args);

        assertOutput("<@directive?withArgsLast(args) b=22 c=null d=55 />",
                "{b=22, c=null, d=55, a=null, e=6, f=7, g=null}{}");

        assertOutput("<@directive?withArgsLast({}) b=22 c=null d=55 />",
                "{b=22, c=null, d=55}{}");

        assertOutput("<@directive?withArgsLast(args) />",
                "{a=null, b=2, c=3, e=6, f=7, g=null}{}");
    }

    @Test
    public void testMacroWithArgsLastNamed() throws IOException, TemplateException {
        assertOutput("<@m?withArgsLast({'a': 1, 'b': 2}) />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgsLast({'b': 2}) a=1 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgsLast({}) a=1 b=2 />", "a=1; b=2; c=d3");

        assertOutput("<@m?withArgsLast({'a': 1, 'b': 2, 'c': 3}) />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast({'b': 2}) a=1 c=3 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast({'c': 3}) a=1 b=2 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast({}) a=1 b=2 c=3 />", "a=1; b=2; c=3");

        assertOutput("<@m?withArgsLast({'b': 2}) 1 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgsLast({'c': 3}) 1 2 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast({'b': 22, 'c': 3}) 1 2 />", "a=1; b=2; c=3");

        assertOutput("<@mCA?withArgsLast({'a': 1, 'b': 2, 'c': 3, 'd': 4}) />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgsLast({'b': 2, 'c': 3, 'd': 4}) a=1 />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgsLast({'c': 3, 'd': 4}) a=1 b=2 />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgsLast({'d': 4}) a=1 b=2 c=3 />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgsLast({}) a=1 b=2 c=3 d=4 />", "a=1; b=2; o={c=3, d=4}");

        assertOutput("<@mCA?withArgsLast({'a': 11}) 1 2 />", "a=1; b=2; o=[]");
        assertOutput("<@mCA?withArgsLast({'a': 11, 'c': 3}) 1 2 />", "a=1; b=2; o={c=3}");
        assertErrorContains("<@mCA?withArgsLast({'a': 11, 'c': 3}) 1 2 3 />", "both named and positional", "catch-all");
        assertOutput("<@mCA?withArgsLast({'a': 11, 'b': 22}) 1 2 3 />", "a=1; b=2; o=[3]");

        assertOutput("<@mCAO?withArgsLast({'a': 1, 'b': 2}) />", "o={a=1, b=2}");
        assertOutput("<@mCAO?withArgsLast({'b': 2}) a=1 />", "o={a=1, b=2}");
        assertOutput("<@mCAO?withArgsLast({}) a=1 b=2 />", "o={a=1, b=2}");

        assertOutput("<@mCAO?withArgsLast({}) />", "o={}");

        // Ordering of "real" args win:
        assertOutput("<@mCA?withArgsLast({'c': 3, 'd': 4}) a=1 b=2 />", "a=1; b=2; o={c=3, d=4}");
        assertOutput("<@mCA?withArgsLast({'c': 3, 'd': 4}) a=1 d=44 b=2 />", "a=1; b=2; o={d=44, c=3}");
    }

    @Test
    public void testMacroWithArgsLastNamedNullArgs() throws IOException, TemplateException {
        assertOutput("<@mCA?withArgsLast({'c': 3, 'd': 4}) a=1 d=null b=2 />", "a=1; b=2; o={d=null, c=3}");
        Map<String, Integer> cAndDNull = new LinkedHashMap<>();
        cAndDNull.put("c", 3);
        cAndDNull.put("d", null);
        addToDataModel("cAndDNull", cAndDNull);
        assertOutput("<@mCA?withArgsLast(cAndDNull) a=1 b=2 />", "a=1; b=2; o={c=3, d=null}");
        assertOutput("<@mCA?withArgsLast(cAndDNull) a=1 d=null b=2 />", "a=1; b=2; o={d=null, c=3}");
    }

    @Test
    public void testMacroWithArgsLastPositional() throws IOException, TemplateException {
        assertOutput("<@m?withArgsLast([1, 2, 3]) />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast([2, 3]) 1 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast([3]) 1 2 />", "a=1; b=2; c=3");
        assertOutput("<@m?withArgsLast([]) 1 2 3 />", "a=1; b=2; c=3");

        assertOutput("<@m?withArgsLast([]) a=1 b=2 />", "a=1; b=2; c=d3");
        assertErrorContains("<@m?withArgsLast([3]) a=1 b=2 />", "by name", "by position", "last");

        assertOutput("<@m?withArgsLast([1, 2]) />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgsLast([2]) 1 />", "a=1; b=2; c=d3");
        assertOutput("<@m?withArgsLast([]) 1 2 />", "a=1; b=2; c=d3");

        assertOutput("<@mCA?withArgsLast([1, 2, 3, 4]) />", "a=1; b=2; o=[3, 4]");
        assertOutput("<@mCA?withArgsLast([2, 3, 4]) 1 />", "a=1; b=2; o=[3, 4]");
        assertOutput("<@mCA?withArgsLast([3, 4]) 1 2 />", "a=1; b=2; o=[3, 4]");
        assertOutput("<@mCA?withArgsLast([4]) 1 2 3 />", "a=1; b=2; o=[3, 4]");
        assertOutput("<@mCA?withArgsLast([]) 1 2 3 4 />", "a=1; b=2; o=[3, 4]");

        assertOutput("<@mCAO?withArgsLast([1, 2, 3, 4]) />", "o=[1, 2, 3, 4]");
        assertOutput("<@mCAO?withArgsLast([3, 4]) 1 2 />", "o=[1, 2, 3, 4]");
        assertOutput("<@mCAO?withArgsLast([]) 1 2 3 4 />", "o=[1, 2, 3, 4]");

        assertOutput("<@mCAO?withArgsLast([]) a=1 b=2 />", "o={a=1, b=2}");
        assertErrorContains("<@mCAO?withArgsLast([3]) a=1 b=2 />", "by name", "by position", "last");

        assertOutput("<@mCAO?withArgsLast([]) />", "o=[]");

        assertErrorContains("<@m?withArgsLast([0, 0, 0, 0]) />", "3", "4", "parameter");
        assertErrorContains("<@m?withArgsLast([0, 0, 0]) 0 />", "3", "4", "parameter");
        assertErrorContains("<@m?withArgsLast([]) 0 0 0 0 />", "3", "4", "parameter");
    }

    @Test
    public void testMacroWithArgsLastPositionalNullArgs() throws IOException, TemplateException {
        ArrayList<Object> twoAndNull = new ArrayList<>();
        twoAndNull.add(2);
        twoAndNull.add(null);
        addToDataModel("twoAndNull", twoAndNull);

        assertOutput("<@m?withArgsLast(twoAndNull) 1 />", "a=1; b=2; c=d3");
        assertErrorContains("<@m?withArgsLast([3]) null 2 />", "\"a\"", "null");
        assertOutput("<@m?withArgsLast([]) 1 2 null />", "a=1; b=2; c=d3");

        assertOutput("<@mCAO?withArgsLast(twoAndNull) 1 />", "o=[1, 2, null]");
        assertOutput("<@mCAO?withArgsLast([3]) null 2 />", "o=[null, 2, 3]");
    }

    private static class TestTemplateDirectiveModel implements TemplateDirectiveModel {

        public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws
                TemplateException, IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, TemplateModel> param : ((Map<String, TemplateModel>) params).entrySet()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(param.getKey());
                sb.append("=");
                TemplateModel value = param.getValue();
                sb.append(value != null ? EvalUtil.coerceModelToPlainText(value, null, null, env) : "null");
            }
            sb.append("}");
            env.getOut().write(sb.toString());

            if (loopVars.length > 0) {
                loopVars[0] = new SimpleNumber(11);
                if (loopVars.length > 1) {
                    loopVars[1] = new SimpleNumber(22);
                    if (loopVars.length > 2) {
                        throw new TemplateModelException("Too many loop vars");
                    }
                }
            }

            env.getOut().write("{");
            if (body != null) {
                body.render(env.getOut());
            }
            env.getOut().write("}");
        }
    }

}