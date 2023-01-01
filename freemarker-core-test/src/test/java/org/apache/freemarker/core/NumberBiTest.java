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

import java.io.IOException;

public class NumberBiTest extends TemplateTest {

    @Test
    public void testSimple() throws TemplateException, IOException {
        assertNumberBi("1", "1");
        assertNumberBi("-1", "-1");
        assertNumberBi("1.9000", "1.9");
        assertNumberBi("19E-1", "1.9");
        assertNumberBi("INF", "INF");
        assertNumberBi("-Infinity", "-INF");
        assertNumberBi("NaN", "NaN");
    }

    @Test
    public void testPlusPrefix() throws TemplateException, IOException {
        assertNumberBi("+1", "1");
    }

    private final void assertNumberBi(String input, String output) throws TemplateException, IOException {
        assertOutput("${'" + input + "'?number?c}", output);
    }
}
