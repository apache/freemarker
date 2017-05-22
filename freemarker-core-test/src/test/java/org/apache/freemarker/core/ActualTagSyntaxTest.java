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

import static org.apache.freemarker.core.TagSyntax.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class ActualTagSyntaxTest {

    @Test
    public void testWithFtlHeader() throws IOException {
        testWithFtlHeader(AUTO_DETECT);
        testWithFtlHeader(ANGLE_BRACKET);
        testWithFtlHeader(SQUARE_BRACKET);
    }
    
    private void testWithFtlHeader(TagSyntax cfgTagSyntax) throws IOException {
        assertEquals(getActualTagSyntax("[#ftl]foo", cfgTagSyntax), SQUARE_BRACKET);
        assertEquals(getActualTagSyntax("<#ftl>foo", cfgTagSyntax), ANGLE_BRACKET);
    }
    
    @Test
    public void testUndecidable() throws IOException {
        assertEquals(getActualTagSyntax("foo", AUTO_DETECT), ANGLE_BRACKET);
        assertEquals(getActualTagSyntax("foo", ANGLE_BRACKET), ANGLE_BRACKET);
        assertEquals(getActualTagSyntax("foo", SQUARE_BRACKET), SQUARE_BRACKET);
    }

    @Test
    public void testDecidableWithoutFtlHeader() throws IOException {
        assertEquals(getActualTagSyntax("foo<#if true></#if>", AUTO_DETECT), ANGLE_BRACKET);
        assertEquals(getActualTagSyntax("foo<#if true></#if>", ANGLE_BRACKET), ANGLE_BRACKET);
        assertEquals(getActualTagSyntax("foo<#if true></#if>", SQUARE_BRACKET), SQUARE_BRACKET);
        
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", AUTO_DETECT), SQUARE_BRACKET);
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", ANGLE_BRACKET), ANGLE_BRACKET);
        assertEquals(getActualTagSyntax("foo[#if true][/#if]", SQUARE_BRACKET), SQUARE_BRACKET);
    }
    
    private TagSyntax getActualTagSyntax(String ftl, TagSyntax cfgTagSyntax) throws IOException {
        return new Template(
                null, ftl,
                new TestConfigurationBuilder().tagSyntax(cfgTagSyntax).build()).getActualTagSyntax();
    }
    
}
