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
package org.apache.freemarker.servlet;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.MockServletContext;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.servlet.InitParamParser;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class InitParamParserTest {

    @Test
    public void testFindTemplatePathSettingAssignmentsStart() {
        assertEquals(0, InitParamParser.findTemplatePathSettingAssignmentsStart("?settings()"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings()"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=1, y=2)"));
        assertEquals(2, InitParamParser.findTemplatePathSettingAssignmentsStart("x ? settings ( x=1, y=2 ) "));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=f(), y=g())"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=\"(\", y='(')"));
        assertEquals(1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings(x=\"(\\\"\", y='(\\'')"));

        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart(""));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("settings"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("settings()"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("foo?/settings(x=1)"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings()x=1)"));
        assertEquals(-1, InitParamParser.findTemplatePathSettingAssignmentsStart("x?settings((x=1)"));

        try {
            assertEquals(0, InitParamParser.findTemplatePathSettingAssignmentsStart("x?setting(x = 1)"));
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("\"setting\""));
        }
    }

    @Test
    public void testCreateTemplateLoader() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates",
                    cfg, getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertEquals(Boolean.FALSE, ctl.getURLConnectionUsesCaches());
        }

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates?settings(URLConnectionUsesCaches=true)",
                    cfg, getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertEquals(Boolean.TRUE, ctl.getURLConnectionUsesCaches());
        }

        {
            MultiTemplateLoader mtl = (MultiTemplateLoader) InitParamParser.createTemplateLoader(
                    "["
                    + "templates?settings(URLConnectionUsesCaches=null, attemptFileAccess=false), "
                    + "foo/templates?settings(URLConnectionUsesCaches=true), "
                    + "classpath:templates, "
                    + "classpath:foo/templates?settings(URLConnectionUsesCaches=true)"
                    + "]",
                    cfg, getClass(), new MockServletContext());

            assertEquals(4, mtl.getTemplateLoaderCount());
            
            final WebAppTemplateLoader tl1 = (WebAppTemplateLoader) mtl.getTemplateLoader(0);
            assertNull(tl1.getURLConnectionUsesCaches());
            assertFalse(tl1.getAttemptFileAccess());
            
            final WebAppTemplateLoader tl2 = (WebAppTemplateLoader) mtl.getTemplateLoader(1);
            assertEquals(Boolean.TRUE, tl2.getURLConnectionUsesCaches());
            assertTrue(tl2.getAttemptFileAccess());
            
            final ClassTemplateLoader tl3 = (ClassTemplateLoader) mtl.getTemplateLoader(2);
            assertEquals(Boolean.FALSE, tl3.getURLConnectionUsesCaches());
            
            final ClassTemplateLoader tl4 = (ClassTemplateLoader) mtl.getTemplateLoader(3);
            assertEquals(Boolean.TRUE, tl4.getURLConnectionUsesCaches());
        }
    }
    
    @Test
    public void testParseCommaSeparatedTemplateLoaderList() {
        assertEquals(Collections.emptyList(),
                InitParamParser.parseCommaSeparatedTemplatePaths(""));
        assertEquals(Collections.emptyList(),
                InitParamParser.parseCommaSeparatedTemplatePaths("  "));
        assertEquals(Collections.emptyList(),
                InitParamParser.parseCommaSeparatedTemplatePaths(","));
        
        assertEquals(ImmutableList.of("a"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a"));
        assertEquals(ImmutableList.of("a"),
                
                InitParamParser.parseCommaSeparatedTemplatePaths("  a  "));
        assertEquals(ImmutableList.of("a", "b", "c"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a,b,c"));
        assertEquals(ImmutableList.of("a", "b", "c"),
                InitParamParser.parseCommaSeparatedTemplatePaths("  a  ,  b  ,  c  "));
        assertEquals(ImmutableList.of("a", "b", "c"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a,b,c,"));
        
        try {
            assertEquals(ImmutableList.of("a", "b", "c"),
                    InitParamParser.parseCommaSeparatedTemplatePaths("a,b,,c"));
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("comma"));
        }
        try {
            assertEquals(ImmutableList.of("a", "b", "c"),
                    InitParamParser.parseCommaSeparatedTemplatePaths(",a,b,c"));
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("comma"));
        }
        try {
            assertEquals(ImmutableList.of("a", "b", "c"),
                    InitParamParser.parseCommaSeparatedTemplatePaths(",a,b,c"));
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("comma"));
        }
        
        assertEquals(ImmutableList.of("a?settings(1)", "b", "c?settings(2)"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a?settings(1),b,c?settings(2)"));
        assertEquals(ImmutableList.of("a ? settings ( 1 )", "b", "c ? settings ( 2 )"),
                InitParamParser.parseCommaSeparatedTemplatePaths(" a ? settings ( 1 ) , b , c ? settings ( 2 ) "));
        assertEquals(ImmutableList.of("a?settings(1,2,3)", "b?settings(1,2)", "c?settings()"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a?settings(1,2,3),b?settings(1,2),c?settings()"));
        assertEquals(ImmutableList.of("a?settings(x=1, y=2)"),
                InitParamParser.parseCommaSeparatedTemplatePaths("a?settings(x=1, y=2)"));
        
        try {
            InitParamParser.parseCommaSeparatedTemplatePaths("a?foo(x=1, y=2)");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("settings"));
        }
    }

}
