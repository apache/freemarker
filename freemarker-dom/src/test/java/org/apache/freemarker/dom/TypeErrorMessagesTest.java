/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.dom;

import org.apache.freemarker.dom.test.DOMLoader;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class TypeErrorMessagesTest extends TemplateTest {

    @Test
    public void test() throws Exception {
        addToDataModel("doc", DOMLoader.toModel("<a><b>123</b><c a='true'>1</c><c a='false'>2</c></a>"));

        assertErrorContains("${doc.a.c}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.c?boolean}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.d}",
                "used as string", "query result", "0", "no matches");
        assertErrorContains("${doc.a.d?boolean}",
                "used as string", "query result", "0", "no matches");

        assertErrorContains("${doc.a.c.@a}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.d.@b}",
                "used as string", "query result", "x", "no matches");

        assertErrorContains("${doc.a.b * 2}",
                "used as number", "text", "explicit conversion");
        assertErrorContains("<#if doc.a.b></#if>",
                "used as number", "text", "explicit conversion");

        assertErrorContains("${doc.a.d?nodeName}",
                "used as node", "query result", "0", "no matches");
        assertErrorContains("${doc.a.c?nodeName}",
                "used as node", "query result", "2", "multiple matches");
    }

}
