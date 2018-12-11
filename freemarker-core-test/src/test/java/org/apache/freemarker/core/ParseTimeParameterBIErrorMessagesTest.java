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

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class ParseTimeParameterBIErrorMessagesTest extends TemplateTest {

    @Test
    public void testThen() throws Exception {
        assertErrorContains("${true?then}", "expecting", "\"(\"");
        assertErrorContains("${true?then + 1}", "expecting", "\"(\"");
        assertErrorContains("${true?then()}", "?then", "2 parameters");
        assertErrorContains("${true?then(1)}", "?then", "2 parameters");
        assertOutput("${true?then(1, 2)}", "1");
        assertErrorContains("${true?then(1, 2, 3)}", "?then", "2 parameters");
    }

    @Test
    public void testSwitch() throws Exception {
        assertErrorContains("${true?switch}", "expecting", "\"(\"");
        assertErrorContains("${true?switch + 1}", "expecting", "\"(\"");
        assertErrorContains("${true?switch()}", "at least 2 parameters");
        assertErrorContains("${true?switch(true)}", "at least 2 parameters");
        assertOutput("${true?switch(true, 1)}", "1");
    }
    
}
