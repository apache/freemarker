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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class ListWithStreamLikeBuiltinsTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setNumberFormat("0.####");
        cfg.setBooleanFormat("c");
        return cfg;
    }

    @Test
    public void testLambdaScope() throws Exception {
        // Loop variables aren't visible during the lazy result generation:
        assertOutput("<#list (1..3)?map(p -> p * 10 + it!'-') as it>${it}<#sep>, </#list>",
                "10-, 20-, 30-");
        assertOutput("<#list (1..3)?map(p -> p * 10 + it_has_next!'-') as it>${it}<#sep>, </#list>",
                "10-, 20-, 30-");
        assertOutput("<#list (1..3)?map(p -> p * 10 + it!'-')><#items as it>${it}<#sep>, </#items></#list>",
                "10-, 20-, 30-");

        // #else scope wasn't messed up
        assertOutput("<#list []?map(p -> p) as it>${it}<#else>${it_has_next!'-'}</#list>",
                "-");
    }

    @Test
    public void testListEnablesLaziness() throws Exception {
        // #list enables lazy evaluation:
        assertOutput(
                "" +
                        "<#assign s = ''>" +
                        "<#function tenTimes(x)><#assign s += '${x}->'><#return x * 10></#function>" +
                        "<#list (1..3)?map(tenTimes) as x>" +
                        "<#assign s += x>" +
                        "<#sep><#assign s += ', '>" +
                        "</#list>" +
                        "${s}",
                "1->10, 2->20, 3->30");
        // Most other context causes eager behavior:
        assertOutput(
                "" +
                        "<#assign s = ''>" +
                        "<#function tenTimes(x)><#assign s += '${x}->'><#return x * 10></#function>" +
                        "<#assign xs = (1..3)?map(tenTimes)>" +
                        "<#list xs as x>" +
                        "<#assign s += x>" +
                        "<#sep><#assign s += ', '>" +
                        "</#list>" +
                        "${s}",
                "1->2->3->10, 20, 30");

        // ?map-s can be chained and all is "streaming":
        assertOutput(
                "" +
                        "<#assign s = ''>" +
                        "<#function tenTimes(x)><#assign s += '${x}->'><#return x * 10></#function>" +
                        "<#list (1..3)?map(tenTimes)?map(tenTimes)?map(tenTimes) as x>" +
                        "<#assign s += x>" +
                        "<#sep><#assign s += ', '>" +
                        "</#list>" +
                        "${s}",
                "1->10->100->1000, 2->20->200->2000, 3->30->300->3000");

        // Rest of the elements not consumed after #break:
        assertOutput(
                "" +
                        "<#assign s = ''>" +
                        "<#function tenTimes(x)><#assign s += '${x}->'><#return x * 10></#function>" +
                        "<#list (1..3)?map(tenTimes) as x>" +
                        "<#assign s += x>" +
                        "<#sep><#assign s += ', '>" +
                        "<#if x == 20><#break></#if>" +
                        "</#list>" +
                        "${s}",
                "1->10, 2->20, ");
    }

}
