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

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class FM3EscapedOperatorsTest extends TemplateTest {
    
    @Test
    public void testComparisonResults() throws Exception {
        assertOutput("${(1 < 2)?c} ${(1 < 1)?c} ${(1 lt 2)?c} ${(1 lt 1)?c}", "true false true false");
        assertOutput("${(1 <= 2)?c} ${(1 <= 1)?c} ${(1 le 2)?c} ${(1 le 1)?c}", "true true true true");
        assertOutput("${(2 > 1)?c} ${(1 > 1)?c} ${(2 gt 1)?c} ${(1 gt 1)?c}", "true false true false");
        assertOutput("${(2 >= 1)?c} ${(1 >= 1)?c} ${(2 ge 1)?c} ${(1 ge 1)?c}", "true true true true");
    }

    @Test
    public void testLogicalAndResults() throws Exception {
        assertOutput("${(true && true)?c} ${(true && false)?c} ${(true and true)?c} ${(true and false)?c}",
                "true false true false");
    }

    @Test
    public void testLogicalOrResults() throws Exception {
        assertOutput("${(true || false)?c} ${(false || false)?c} ${(true or false)?c} ${(false or false)?c}",
                "true false true false");
    }
    
    @Test
    public void testCanonicalForm() throws Exception {
        assertEquals(
                "${a && b and c || d or f} ${a < b}  ${a lt b}",
                new Template(null, "${a && b and c || d or f} ${a < b}  ${a lt b}",
                getConfiguration()).toString());
    }
    
}
