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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class NullTransparencyTest extends TemplateTest {

    @Override
    protected Object createDataModel() {
        Map<String, Object> dataModel = new HashMap<String, Object>();

        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("b");
        dataModel.put("list", list);

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("ak", "av");
        map.put(null, "bv");
        map.put("ck", null);
        dataModel.put("map", map);

        return dataModel;
    }

    @Test
    public void testLoopVariables() throws IOException, TemplateException {
        addToDataModel("it", "fallback");
        
        assertOutput("<#list list as it>${it!'null'}<#sep>, </#list>",
                "a, null, b");
        assertOutput("<#list list><#items as it>${it!'null'}<#sep>, </#items></#list>",
                "a, null, b");

        assertOutput("<#list map?values as it>${it!'null'}<#sep>, </#list>",
                "av, bv, null");
        assertOutput("<#list map as k, it>${k!'null'}=${it!'null'}<#sep>, </#list>",
                "ak=av, null=bv, ck=null");
        assertOutput("<#list map><#items as k, it>${k!'null'}=${it!'null'}<#sep>, </#items></#list>",
                "ak=av, null=bv, ck=null");

        assertOutput("<#list map?keys as it>${it!'null'}<#sep>, </#list>",
                "ak, null, ck");
        assertOutput("<#list map as it, v>${it!'null'}=${v!'null'}<#sep>, </#list>",
                "ak=av, null=bv, ck=null");
        assertOutput("<#list map><#items as it, v>${it!'null'}=${v!'null'}<#sep>, </#items></#list>",
                "ak=av, null=bv, ck=null");

        assertOutput("" +
                        "<#macro loop><#nested 1>, <#nested totallyMissing></#macro>\n" +
                        "<@loop; it>${it!'null'}</@loop>",
                "1, null");
    }

}
