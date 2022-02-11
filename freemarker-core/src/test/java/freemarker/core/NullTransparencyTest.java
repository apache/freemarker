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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class NullTransparencyTest extends TemplateTest {

    @Override
    protected Object createDataModel() {
        Map<String, Object> dataModel = new HashMap<>();

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add(null);
        list.add("b");
        dataModel.put("list", list);

        Map<String, String> map = new LinkedHashMap<>();
        map.put("ak", "av");
        map.put(null, "bv");
        map.put("ck", null);
        dataModel.put("map", map);

        return dataModel;
    }

    @Test
    public void testWithoutClashingHigherScopeVar() throws Exception {
        assertTrue(getConfiguration().getFallbackOnNullLoopVariable());
        testLambdaArguments();
        testLoopVariables("null");

        getConfiguration().setFallbackOnNullLoopVariable(false);
        testLambdaArguments();
        testLoopVariables("null");
    }

    @Test
    public void testWithClashingHigherScopeVar() throws Exception {
        addToDataModel("it", "fallback");

        assertTrue(getConfiguration().getFallbackOnNullLoopVariable());
        testLambdaArguments();
        testLoopVariables("fallback");

        getConfiguration().setFallbackOnNullLoopVariable(false);
        testLambdaArguments();
        testLoopVariables("null");
    }

    // Lambdas arguments never fall back on null, as there was no backward compatibility constraint:
    protected void testLambdaArguments() throws IOException, TemplateException {
        assertOutput("<#list list?filter(it -> it??) as it>${it!'null'}<#sep>, </#list>",
                "a, b");
        assertOutput("<#list list?takeWhile(it -> it??) as it>${it!'null'}<#sep>, </#list>",
                "a");
        assertOutput("<#list list?map(it -> it!'null') as it>${it}<#sep>, </#list>",
                "a, null, b");
    }

    // Loop variables by default fallback on null, for backward compatibility
    protected void testLoopVariables(String expectedFallback) throws IOException, TemplateException {
        assertOutput("<#list list as it>${it!'null'}<#sep>, </#list>",
                "a, " + expectedFallback + ", b");
        assertOutput("<#list list><#items as it>${it!'null'}<#sep>, </#items></#list>",
                "a, " + expectedFallback + ", b");

        assertOutput("<#list map?values as it>${it!'null'}<#sep>, </#list>",
                "av, bv, " + expectedFallback);
        assertOutput("<#list map as k, it>${k!'null'}=${it!'null'}<#sep>, </#list>",
                "ak=av, null=bv, ck=" + expectedFallback);
        assertOutput("<#list map><#items as k, it>${k!'null'}=${it!'null'}<#sep>, </#items></#list>",
                "ak=av, null=bv, ck=" + expectedFallback);

        assertOutput("<#list map?keys as it>${it!'null'}<#sep>, </#list>",
                "ak, " + expectedFallback + ", ck");
        assertOutput("<#list map as it, v>${it!'null'}=${v!'null'}<#sep>, </#list>",
                "ak=av, " + expectedFallback + "=bv, ck=null");
        assertOutput("<#list map><#items as it, v>${it!'null'}=${v!'null'}<#sep>, </#items></#list>",
                "ak=av, " + expectedFallback + "=bv, ck=null");

        assertOutput("" +
                "<#macro loop><#nested 1>, <#nested totallyMissing></#macro>\n" +
                "<@loop; it>${it!'null'}</@loop>",
                "1, " + expectedFallback);
    }

}
