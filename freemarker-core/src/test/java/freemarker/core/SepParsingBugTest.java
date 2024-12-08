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

public class SepParsingBugTest extends TemplateTest {
    
    @Test
    public void testAutodetectTagSyntax() throws TemplateException, IOException {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_34);
        getConfiguration().setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        assertOutput("<#list [1, 2] as i>${i}<#sep>, </#list>", "1, 2");
        assertOutput("[#list [1, 2] as i]${i}[#sep], [/#list]", "1, 2");
        assertOutput("<#list [1, 2] as i>${i}[#sep], </#list>", "1[#sep], 2[#sep], ");
        assertOutput("[#list [1, 2] as i]${i}<#sep>, [/#list]", "1<#sep>, 2<#sep>, ");
        assertOutput("[#list [1, 2] as i]${i}[sep], [/#list]", "1[sep], 2[sep], ");
        assertOutput("[#list [1, 2] as i]${i}<sep>, [/#list]", "1<sep>, 2<sep>, ");
        assertErrorContains("<#sep>", "#sep must be inside");
        assertErrorContains("[#sep]", "#sep must be inside");
    }

    @Test
    public void testAngleBracketsTagSyntax() throws TemplateException, IOException {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_34);
        getConfiguration().setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertOutput("<#list [1, 2] as i>${i}<#sep>, </#list>", "1, 2");
        assertOutput("[#list [1, 2] as i]${i!'-'}[#sep], [/#list]", "[#list [1, 2] as i]-[#sep], [/#list]");
        assertOutput("<#list [1, 2] as i>${i}[#sep], </#list>", "1[#sep], 2[#sep], ");
        assertOutput("<#list [1, 2] as i>${i}<sep>, </#list>", "1<sep>, 2<sep>, ");
        assertOutput("<#list [1, 2] as i>${i}[sep], </#list>", "1[sep], 2[sep], ");
        assertErrorContains("[#list [1, 2] as i]${i}<#sep>, [/#list]", "#sep must be inside");
        assertErrorContains("<#sep>", "#sep must be inside");
        assertOutput("[#sep]", "[#sep]");
    }

    @Test
    public void testSquareBracketTagSyntax() throws TemplateException, IOException {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_34);
        getConfiguration().setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        assertOutput("<#list [1, 2] as i>${i!'-'}<#sep>, </#list>", "<#list [1, 2] as i>-<#sep>, </#list>");
        assertOutput("[#list [1, 2] as i]${i}[#sep], [/#list]", "1, 2");
        assertErrorContains("<#list [1, 2] as i>${i}[#sep], </#list>", "#sep must be inside");
        assertOutput("[#list [1, 2] as i]${i}<#sep>, [/#list]", "1<#sep>, 2<#sep>, ");
        assertOutput("[#list [1, 2] as i]${i}[sep], [/#list]", "1[sep], 2[sep], ");
        assertOutput("[#list [1, 2] as i]${i}<sep>, [/#list]", "1<sep>, 2<sep>, ");
        assertOutput("<#sep>", "<#sep>");
        assertErrorContains("[#sep]", "#sep must be inside");
    }

    @Test
    public void testPre2Dot3Dot34BugRecreated() throws TemplateException, IOException {
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_33);
        getConfiguration().setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        assertOutput("<#list [1, 2] as i>${i}<sep>, </#list>", "1, 2");
        assertOutput("<#list [1, 2] as i>${i}[#sep], </#list>", "1, 2");
        // square bracket tags were always "strict":
        assertOutput("[#list [1, 2] as i]${i}[sep], [/#list]", "1[sep], 2[sep], ");
        assertOutput("[#list [1, 2] as i]${i}<#sep>, [/#list]", "1, 2");
    }
}
