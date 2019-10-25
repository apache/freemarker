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

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class ArgsSpecialVariableTest extends TemplateTest {

    @Test
    public void macroSimpleTest() throws IOException, TemplateException {
        String macroDef = "<#macro m a b><#list .args as k, v>${k}=${v}<#sep>, </#list></#macro>";
        String expectedOutput = "a=11, b=22";
        assertOutput(macroDef +
                        "<@m a=11 b=22 />",
                expectedOutput);
        assertOutput(macroDef +
                        "<@m 11 22 />",
                expectedOutput);
    }

    @Test
    public void macroZeroArgsTest() throws IOException, TemplateException {
        assertOutput("<#macro m>${.args?size}</#macro><@m />", "0");
        assertOutput("<#macro m others...>${.args?size}</#macro><@m />", "0");
    }

    @Test
    public void macroWithDefaultsTest() throws IOException, TemplateException {
        String macroDef = "<#macro m a b c=3><#list .args as k, v>${k}=${v}<#sep>, </#list></#macro>";
        String expectedOutput = "" +
                "a=11, b=22, c=33; " +
                "a=11, b=22, c=3";
        assertOutput(macroDef +
                        "<@m a=11 b=22 c=33 />; " +
                        "<@m a=11 b=22 />",
                expectedOutput);
        assertOutput(macroDef +
                        "<@m 11 22 33 />; " +
                        "<@m 11 22 />",
                expectedOutput);
    }

    @Test
    public void macroWithMultiPassDefaultsTest() throws IOException, TemplateException {
        String macroDef = "<#macro m a=c b=c c=b><#list .args as k, v>${k}=${v}<#sep>, </#list></#macro>";
        String expectedOutput = "" +
                "a=33, b=33, c=33; " +
                "a=22, b=22, c=22; " +
                "a=11, b=33, c=33; " +
                "a=11, b=22, c=22";
        assertOutput(macroDef +
                        "<@m c=33 />; " +
                        "<@m b=22 />; " +
                        "<@m a=11 c=33 />; " +
                        "<@m a=11 b=22 />",
                expectedOutput);
        assertOutput(macroDef +
                        "<@m null, null, 33 />; " +
                        "<@m null, 22, null />; " +
                        "<@m 11, null, 33 />; " +
                        "<@m 11, 22, null />",
                expectedOutput);
    }

    @Test
    public void macroWithCatchAllTest() throws IOException, TemplateException {
        String macroDef = "<#macro m a b=2 others...><#list .args as k, v>${k}=${v}<#sep>, </#list></#macro>";
        assertOutput(macroDef +
                        "<@m a=11 b=22 c=33 d=44 />; " +
                        "<@m a=11 b=22 />; " +
                        "<@m a=11 />; " +
                        "<@m a=11 c=33 />",
                "a=11, b=22, c=33, d=44; " +
                        "a=11, b=22; " +
                        "a=11, b=2; " +
                        "a=11, b=2, c=33");

        assertOutput(macroDef + "<@m 1, 2 />",
                "a=1, b=2");
        assertErrorContains(macroDef + "<@m 1, 2, 3 />",
                ".args", "catch-all");
    }

    @Test
    public void functionSimpleTest() throws IOException, TemplateException {
        String functionDef = "<#function f a b><#return .args?join(', ')></#function>";
        String expectedOutput = "11, 22";
        assertOutput(functionDef +
                        "${f(11, 22)}",
                expectedOutput);
    }


    @Test
    public void functionZeroArgsTest() throws IOException, TemplateException {
        assertOutput("<#function f><#return .args?size></#function>${f()}", "0");
        assertOutput("<#function f others...><#return .args?size></#function>${f()}", "0");
    }
    
    @Test
    public void functionWithDefaultsTest() throws IOException, TemplateException {
        String functionDef = "<#function f a b c=3><#return .args?join(', ')></#function>";
        String expectedOutput = "" +
                "11, 22, 33; " +
                "11, 22, 3";
        assertOutput(functionDef +
                        "${f(11, 22, 33)}; " +
                        "${f(11, 22)}",
                expectedOutput);
    }

    @Test
    public void functionWithMultiPassDefaultsTest() throws IOException, TemplateException {
        String functionDef = "<#function f a=c b=c c=b><#return .args?join(', ')></#function>";
        assertOutput(functionDef +
                        "${f(null, null, 33)}; " +
                        "${f(null, 22)}; " +
                        "${f(11, null, 33)}; " +
                        "${f(11, 22)}",
                "33, 33, 33; " +
                        "22, 22, 22; " +
                        "11, 33, 33; " +
                        "11, 22, 22");
        assertOutput(functionDef +
                        "${f(11, 22)}; " +
                        "${f(11, 22, 33)}",
                "11, 22, 22; " +
                        "11, 22, 33");
    }

    @Test
    public void functionWithCatchAllTest() throws IOException, TemplateException {
        assertOutput("" +
                        "<#function f a b=2 others...><#return .args?join(', ')></#function>" +
                        "${f(11, 22, 33, 44)}; " +
                        "${f(11, 22)}; " +
                        "${f(11)}; " +
                        "${f(11, null, 33)}",
                "11, 22, 33, 44; " +
                        "11, 22; " +
                        "11, 2; " +
                        "11, 2, 33");
    }
    
    @Test
    public void usedInWrongContextTest() throws IOException, TemplateException {
        assertErrorContains("${.args}", "args", "macro", "function");
        assertErrorContains("<#macro m>${'.args'?eval}</#macro><@m />", "args", "macro", "function");
    }

}
