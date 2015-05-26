/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class ListValidationsTest extends TemplateTest {
    
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
        assertErrorContains("<#list xs><#forEach x in xs><#items as x></#items></#forEach></#list>",
                "#forEach", "doesn't support", "#items");
        assertErrorContains("<#list xs as x><#items as x>${x}</#items></#list>",
                "#list", "must not have", "#items", "as loopVar");
        assertErrorContains("<#list xs><#list xs as x><#items as x>${x}</#items></#list></#list>",
                "#list", "must not have", "#items", "as loopVar");
        assertErrorContains("<#list xs></#list>",
                "#list", "must have", "#items", "as loopVar");
        assertErrorContains("<#forEach x in xs><#items as x></#items></#forEach>",
                "#forEach", "doesn't support", "#items");
        assertErrorContains("<#list xs><#forEach x in xs><#items as x></#items></#forEach></#list>",
                "#forEach", "doesn't support", "#items");
    }

    @Test
    public void testInvalidSepParseTime() throws IOException, TemplateException {
        assertErrorContains("<#sep>, </#sep>",
                "#sep", "must be inside", "#list", "#foreach");
        assertErrorContains("<#sep>, ",
                "#sep", "must be inside", "#list", "#foreach");
        assertErrorContains("<#list xs as x><#else><#sep>, </#list>",
                "#sep", "must be inside", "#list", "#foreach");
        assertErrorContains("<#list xs as x><#macro m><#sep>, </#macro></#list>",
                "#sep", "must be inside", "#list", "#foreach");
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
        assertErrorContains("<#list foos as foo><#function f>${foo?index}</#function></#list>",
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
    
}
