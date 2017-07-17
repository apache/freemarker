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

package org.apache.freemarker.core;

import java.io.IOException;

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

@SuppressWarnings("ThrowableNotThrown")
public class RemovedFM2SyntaxTest extends TemplateTest {

    @Test
    public void testRemovedOperators() throws IOException, TemplateException {
        assertErrorContains("<#if true & true>x</#if>", ParseException.class);
        assertOutput("<#if true && true>x</#if>", "x");

        assertErrorContains("<#if false | true>x</#if>", ParseException.class);
        assertOutput("<#if false || true>x</#if>", "x");

        assertErrorContains("<#if 'a' = 'a'>x</#if>", ParseException.class);
        assertOutput("<#if 'a' == 'a'>x</#if>", "x");
    }

    @Test
    public void testCallSyntax() throws IOException, TemplateException {
        assertErrorContains("<@m 1 2 />", ParseException.class);
        assertErrorContains("<@m 1, 2 />", InvalidReferenceException.class);

        assertErrorContains("\"<#macro m><#nested 1 2></#macro>\"", ParseException.class);
        assertOutput("<#macro m><#nested 1, 2></#macro>", "");
    }

}
