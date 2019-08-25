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

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class NullLiteralTest extends TemplateTest {

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void basicTest() throws Exception {
        assertOutput("<#list ['a', null, 'b']><#items as it>${it!'-'}<#sep>, </#items></#list>",
                "a, -, b");
        assertOutput("<#assign m = {'a': 'A', 'b': null}>a=${m.a}, b=${m.b!'-'}",
                "a=A, b=-");
        assertOutput("<#assign a = 'A'><#assign b=null>a=${a}, b=${b!'-'}",
                "a=A, b=-");

        addToDataModel("obj", new TestBean());
        assertOutput("${obj.m(1)}, ${obj.m(null)}", "n=1, n=null");

        assertErrorContains("${null + 1}", InvalidReferenceException.class);
        assertErrorContains("${1 + null}", InvalidReferenceException.class);
        assertErrorContains("${null + 's'}", InvalidReferenceException.class);
        assertErrorContains("${'s' + null}", InvalidReferenceException.class);
        assertErrorContains("${null + null}", InvalidReferenceException.class);
    }

    public static class TestBean {
        public String m(Integer n) {
            return "n=" + n;
        }
    }
}
