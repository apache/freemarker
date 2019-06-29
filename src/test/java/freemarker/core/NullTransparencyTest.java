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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import freemarker.test.TemplateTest;

public class NullTransparencyTest extends TemplateTest {

    @Test
    public void test() throws Exception {
        List<String> xs = new ArrayList<String>();
        xs.add("a");
        xs.add(null);
        xs.add("b");
        addToDataModel("xs", xs);
        assertOutput("<#list xs?filter(it -> it??) as it>${it!'null'}<#sep>, </#list>", "a, b");
        assertOutput("<#list xs?map(it -> it!'null') as it>${it}<#sep>, </#list>", "a, null, b");

        // Lambdas are always use non-transparent nulls, as there was no backwrad compatibility constraint:
        assertOutput("<#assign it='fallback'><#list xs?map(it -> it!'null') as it>${it}<#sep>, </#list>",
                "a, null, b");
    }
}
