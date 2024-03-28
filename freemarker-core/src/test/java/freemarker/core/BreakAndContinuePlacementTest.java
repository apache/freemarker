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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class BreakAndContinuePlacementTest extends TemplateTest {
    
    private static final String BREAK_NESTING_ERROR_MESSAGE_PART = "<#break> must be nested";
    private static final String CONTINUE_NESTING_ERROR_MESSAGE_PART = "<#continue> must be nested";

    @Test
    public void testValidPlacements() throws IOException, TemplateException {
        assertOutput("<#assign x = 1><#switch x><#case 1>one<#break><#case 2>two</#switch>", "one");
        assertOutput("<#list 1..2 as x>${x}<#break></#list>", "1");
        assertOutput("<#list 1..2 as x>${x}<#continue></#list>", "12");
        assertOutput("<#list 1..2>[<#items as x>${x}<#break></#items>]</#list>", "[1]");
        assertOutput("<#list 1..2 as x>${x}<#list 1..3>B<#break>E<#items as y></#items></#list>E</#list>.", "1B.");
        assertOutput("<#list 1..2 as x>${x}<#list 3..4 as x>${x}<#break></#list>;</#list>", "13;23;");
        assertOutput("<#list [1..2, 3..4, [], 5..6] as xs>[<#list xs as x>${x}<#else><#break></#list>]</#list>.",
                "[12][34][.");
        assertOutput("<#list [1..2, 3..4, [], 5..6] as xs>"
                + "<#list xs>[<#items as x>${x}</#items>]<#else><#break></#list>"
                + "</#list>.",
                "[12][34].");
        assertOutput("<#list 1..2 as x><#switch x><#on 1>one<#break></#switch>;</#list>", "one");
        assertOutput("<#list 1..2 as x><#switch x><#on 1>one<#continue></#switch>;</#list>", "one;");
        assertOutput("<#forEach x in 1..2>${x}<#break></#forEach>", "1");
        assertOutput("<#forEach x in 1..2>${x}<#continue></#forEach>", "12");
        assertOutput("<#switch 1><#case 1>1<#break></#switch>", "1");
        assertOutput("<#switch 1><#default>1<#break></#switch>", "1");
    }

    @Test
    public void testInvalidPlacements() throws IOException, TemplateException {
        assertErrorContains("<#break>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#continue>", CONTINUE_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#switch 1><#case 1>1<#continue></#switch>", CONTINUE_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#switch 1><#on 1>1<#continue></#switch>", CONTINUE_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#switch 1><#on 1>1<#break></#switch>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#switch 1><#on 1>1<#default><#break></#switch>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#list 1..2 as x>${x}</#list><#break>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#if false><#break></#if>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#list xs><#break></#list>", BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains("<#list 1..2 as x>${x}<#else><#break></#list>", BREAK_NESTING_ERROR_MESSAGE_PART);
    }

    @Test
    public void testInvalidPlacementMacroLoophole() throws IOException, TemplateException {
        final String ftl = "<#list 1..2 as x>${x}<#macro m><#break></#macro></#list>";
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_22);
        assertOutput(ftl, "12");
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        assertErrorContains(ftl, BREAK_NESTING_ERROR_MESSAGE_PART);
        assertErrorContains(ftl.replaceAll("#break", "#continue"), CONTINUE_NESTING_ERROR_MESSAGE_PART);
    }
    
}
