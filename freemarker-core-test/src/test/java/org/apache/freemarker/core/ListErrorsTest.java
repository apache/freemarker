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

import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.core.templatesuite.models.Listables;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ListErrorsTest extends TemplateTest {
    
    @Test
    public void testValid() throws IOException, TemplateException {
        assertOutput("<#list 1..2 as x><#list 3..4>${x}:<#items as x>${x}</#items></#list>;</#list>", "1:34;2:34;");
        assertOutput("<#list [] as x>${x}<#else><#list 1..2 as x>${x}<#sep>, </#list></#list>", "1, 2");
        assertOutput("<#macro m>[<#nested 3>]</#macro>"
                + "<#list 1..2 as x>"
                + "${x}@${x?index}"
                + "<@m ; x>"
                + "${x},"
                + "<#list 4..4 as x>${x}@${x?index}</#list>"
                + "</@>"
                + "${x}@${x?index}; "
                + "</#list>",
                "1@0[3,4@0]1@0; 2@1[3,4@0]2@1; ");
    }

    @Test
    public void testInvalidItemsParseTime() throws IOException, TemplateException {
        assertErrorContains("<#items as x>${x}</#items>",
                "#items", "must be inside", "#list");
        assertErrorContains("<#list xs><#macro m><#items as x></#items></#macro></#list>",
                "#items", "must be inside", "#list");
        assertErrorContains("<#list xs as x><#items as x>${x}</#items></#list>",
                "#list", "must not have", "#items", "as loopVar");
        assertErrorContains("<#list xs><#list xs as x><#items as x>${x}</#items></#list></#list>",
                "#list", "must not have", "#items", "as loopVar");
        assertErrorContains("<#list xs></#list>",
                "#list", "must have", "#items", "as loopVar");
    }

    @Test
    public void testInvalidSepParseTime() throws IOException, TemplateException {
        assertErrorContains("<#sep>, </#sep>",
                "#sep", "must be inside", "#list");
        assertErrorContains("<#sep>, ",
                "#sep", "must be inside", "#list");
        assertErrorContains("<#list xs as x><#else><#sep>, </#list>",
                "#sep", "must be inside", "#list");
        assertErrorContains("<#list xs as x><#macro m><#sep>, </#macro></#list>",
                "#sep", "must be inside", "#list");
    }

    @Test
    public void testInvalidItemsRuntime() throws IOException, TemplateException {
        assertErrorContains("<#list 1..1><#items as x></#items><#items as x></#items></#list>",
                "#items", "already entered earlier");
        assertErrorContains("<#list 1..1><#items as x><#items as y>${x}/${y}</#items></#items></#list>",
                "#items", "Can't nest #items into each other");
    }
    
    @Test
    public void testInvalidLoopVarBuiltinLHO() {
        assertErrorContains("<#list foos>${foo?index}</#list>",
                "?index", "foo", "no loop variable");
        assertErrorContains("<#list foos as foo></#list>${foo?index}",
                "?index", "foo" , "no loop variable");
        assertErrorContains("<#list foos as foo><#macro m>${foo?index}</#macro></#list>",
                "?index", "foo" , "no loop variable");
        assertErrorContains("<#list foos as foo><#function f()>${foo?index}</#function></#list>",
                "?index", "foo" , "no loop variable");
        assertErrorContains("<#list xs as x>${foo?index}</#list>",
                "?index", "foo" , "no loop variable");
        assertErrorContains("<#list foos as foo><@m; foo>${foo?index}</@></#list>",
                "?index", "foo" , "user defined directive");
        assertErrorContains(
                "<#list foos as foo><@m; foo><@m; foo>${foo?index}</@></@></#list>",
                "?index", "foo" , "user defined directive");
        assertErrorContains(
                "<#list foos as foo><@m; foo>"
                + "<#list foos as foo><@m; foo>${foo?index}</@></#list>"
                + "</@></#list>",
                "?index", "foo" , "user defined directive");
    }

    @Test
    public void testKeyValueSameName() {
        assertErrorContains("<#list {} as foo, foo></#list>",
                "key", "value", "both" , "foo");
    }

    @Test
    public void testCollectionVersusHash() {
        assertErrorContains("<#list {} as i></#list>",
                "as k, v");
        assertErrorContains("<#list [] as k, v></#list>",
                "only one loop variable");
    }

    @Test
    public void testNonEx2NonStringKey() throws IOException, TemplateException {
        addToDataModel("m", new Listables.NonEx2MapAdapter(ImmutableMap.of("k1", "v1", 2, "v2"),
                new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build()));
        assertOutput("<#list m?keys as k>${k};</#list>", "k1;2;");
        assertErrorContains("<#list m as k, v></#list>",
                "string", "number", ".TemplateHashModelEx2");
    }
    
}
