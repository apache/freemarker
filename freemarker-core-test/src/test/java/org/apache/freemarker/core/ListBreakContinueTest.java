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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ListBreakContinueTest extends TemplateTest {
    
    @Test
    public void testNonHash() throws IOException, TemplateException {
        addToDataModel("listed", ImmutableSet.of(1, 2, 3, 4, 5));
        assertOutput(
                "<#list listed as i>B(${i}) <#if i == 3>Break!<#break></#if>A(${i})<#sep>, </#list>",
                "B(1) A(1), B(2) A(2), B(3) Break!");
        assertOutput(
                "<#list listed as i>B(${i}) <#if i == 3>Continue! <#continue></#if>A(${i})<#sep>, </#list>",
                "B(1) A(1), B(2) A(2), B(3) Continue! B(4) A(4), B(5) A(5)");
    }

    @Test
    public void testHash() throws IOException, TemplateException {
        testHash(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5)); // Listing a TemplateHashModelEx
    }

    private void testHash(Object listed) throws IOException, TemplateException {
        addToDataModel("listed", listed);
        assertOutput(
                "<#list listed as k, v>B(${k}=${v}) <#if k == 'c'>Break!<#break></#if>A(${k}=${v})<#sep>, </#list>",
                "B(a=1) A(a=1), B(b=2) A(b=2), B(c=3) Break!");
        assertOutput(
                "<#list listed as k, v>B(${k}=${v}) <#if k == 'c'>Continue! <#continue></#if>A(${k}=${v})<#sep>, </#list>",
                "B(a=1) A(a=1), B(b=2) A(b=2), B(c=3) Continue! B(d=4) A(d=4), B(e=5) A(e=5)");
    }
    
}
