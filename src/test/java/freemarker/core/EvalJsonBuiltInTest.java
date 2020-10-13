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

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class EvalJsonBuiltInTest extends TemplateTest {

    @Test
    public void test() throws Exception {
        assertOutput("${'1'?eval_json}", "1");
        assertOutput("${'1'?evalJson}", "1");

        assertOutput("${'null'?evalJson!'-'}", "-");

        assertOutput("<#list '{\"a\": 1e2, \"b\": null}'?evalJson as k, v>${k}=${v!'NULL'}<#sep>, </#list>", "a=100, b=NULL");
    }

}
