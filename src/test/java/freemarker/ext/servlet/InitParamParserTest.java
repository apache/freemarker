/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package freemarker.ext.servlet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.MockServletContext;

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
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates",
                    cfg, this.getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertNull(ctl.getURLConnectionUsesCaches());
        }

        {
            ClassTemplateLoader ctl = (ClassTemplateLoader) InitParamParser.createTemplateLoader(
                    "classpath:templates?settings(URLConnectionUsesCaches=false)",
                    cfg, this.getClass(), null);
            assertEquals("templates/", ctl.getBasePackagePath());
            assertEquals(Boolean.FALSE, ctl.getURLConnectionUsesCaches());
        }

        {
            MultiTemplateLoader mtl = (MultiTemplateLoader) InitParamParser.createTemplateLoader(
                    "["
                    + "templates?settings(URLConnectionUsesCaches=false, attemptFileAccess=false), "
                    + "foo/templates?settings(URLConnectionUsesCaches=true), "
                    + "classpath:templates, "
                    + "classpath:foo/templates?settings(URLConnectionUsesCaches=true)"
                    + "]",
                    cfg, this.getClass(), new MockServletContext());

            assertEquals(4, mtl.getTemplateLoaderCount());
            
            final WebappTemplateLoader tl1 = (WebappTemplateLoader) mtl.getTemplateLoader(0);
            assertEquals(Boolean.FALSE, tl1.getURLConnectionUsesCaches());
            assertFalse(tl1.getAttemptFileAccess());
            
            final WebappTemplateLoader tl2 = (WebappTemplateLoader) mtl.getTemplateLoader(1);
            assertEquals(Boolean.TRUE, tl2.getURLConnectionUsesCaches());
            assertTrue(tl2.getAttemptFileAccess());
            
            final ClassTemplateLoader tl3 = (ClassTemplateLoader) mtl.getTemplateLoader(2);
            assertNull(tl3.getURLConnectionUsesCaches());
            
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
