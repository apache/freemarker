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

package org.apache.freemarker.core.templateresolver;

import static org.apache.freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Locale;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;


public class TemplateNameFormatTest {

    @Test
    public void testToRootBasedName() throws MalformedTemplateNameException {
        final TemplateNameFormat tnf = DefaultTemplateNameFormat.INSTANCE;

        // Relative paths:
        // - No scheme:
        assertEquals("a/b", tnf.toRootBasedName("a/", "b"));
        assertEquals("/a/b", tnf.toRootBasedName("/a/", "b"));
        assertEquals("a/b", tnf.toRootBasedName("a/f", "b"));
        assertEquals("/a/b", tnf.toRootBasedName("/a/f", "b"));
        // - Scheme:
        assertEquals("s://a/b", tnf.toRootBasedName("s://a/", "b"));
        assertEquals("s:///a/b", tnf.toRootBasedName("s:///a/", "b"));
        assertEquals("s://a/b", tnf.toRootBasedName("s://a/f", "b"));
        assertEquals("s:///a/b", tnf.toRootBasedName("s:///a/f", "b"));
        assertEquals("s://b", tnf.toRootBasedName("s://f", "b"));
        assertEquals("s:///b", tnf.toRootBasedName("s:///f", "b"));
        assertEquals("s:b", tnf.toRootBasedName("s:f", "b"));
        assertEquals("s:/b", tnf.toRootBasedName("s:/f", "b"));
        assertEquals("s:b", tnf.toRootBasedName("s:f", "/b"));
        assertEquals("s:b", tnf.toRootBasedName("s:/f", "/b"));
        assertEquals("s:f/b", tnf.toRootBasedName("s:f/", "b"));
        assertEquals("s:/f/b", tnf.toRootBasedName("s:/f/", "b"));
        assertEquals("s:b", tnf.toRootBasedName("s:f/", "/b"));
        assertEquals("s:b", tnf.toRootBasedName("s:/f/", "/b"));
        assertEquals("s:b", tnf.toRootBasedName("s:/f/", "/b"));
        assertEquals("b", tnf.toRootBasedName("a/s://f/", "/b"));

        // Absolute paths:
        // - No scheme:
        assertEquals("b", tnf.toRootBasedName("a/", "/b"));
        assertEquals("b", tnf.toRootBasedName("/a/", "/b"));
        assertEquals("b", tnf.toRootBasedName("a/s:/f/", "/b"));
        // - Scheme:
        assertEquals("s://b", tnf.toRootBasedName("s://x/", "/b"));
        assertEquals("s://b", tnf.toRootBasedName("s:///x/", "/b"));

        // Schemed absolute paths:
        assertEquals("s://b", tnf.toRootBasedName("a/", "s://b"));
        assertEquals("s://b", tnf.toRootBasedName("i://a/", "s://b"));
    }

    @Test
    public void testNormalizeRootBasedName() throws MalformedTemplateNameException {
        final TemplateNameFormat tnf = DefaultTemplateNameFormat.INSTANCE;

        assertEquals("", tnf.normalizeRootBasedName(""));
        for (String lead : new String[] { "", "/" }) {
            assertEquals("foo", tnf.normalizeRootBasedName(lead + "foo"));
            assertEquals("foo", tnf.normalizeRootBasedName(lead + "./foo"));
            assertEquals("foo", tnf.normalizeRootBasedName(lead + "./././foo"));
            assertEquals("foo", tnf.normalizeRootBasedName(lead + "bar/../foo"));
            assertEquals("a/b/", tnf.normalizeRootBasedName("a/b/"));
            assertEquals("a/", tnf.normalizeRootBasedName("a/b/../"));
            assertEquals("a/c../..d/e*/*f", tnf.normalizeRootBasedName("a/c../..d/e*/*f"));
            assertEquals("", tnf.normalizeRootBasedName(""));
            assertEquals("foo/bar/*", tnf.normalizeRootBasedName("foo/bar/*"));
            assertEquals("schema://", tnf.normalizeRootBasedName("schema://"));

            assertThrowsWithBackingOutException(lead + "bar/../../x/foo", tnf);
            assertThrowsWithBackingOutException(lead + "../x", tnf);
            assertThrowsWithBackingOutException(lead + "../../../x", tnf);
            assertThrowsWithBackingOutException(lead + "../../../x", tnf);
            assertThrowsWithBackingOutException("x://../../../foo", tnf);

            {
                final String name = lead + "foo\u0000";
                try {
                    tnf.normalizeRootBasedName(name);
                    fail();
                } catch (MalformedTemplateNameException e) {
                    assertEquals(name, e.getTemplateName());

                    assertThat(e.getMalformednessDescription(), containsStringIgnoringCase("null character"));
                }
            }
        }

        // ".." and "."
        assertEquals("foo", tnf.normalizeRootBasedName("bar/./../foo"));
        
        assertThrowsWithBackingOutException("../../foo", tnf);
        assertThrowsWithBackingOutException("../../../../foo", tnf);
        
        // ".." and "*"
        assertEquals("a/*/foo", tnf.normalizeRootBasedName("a/b/*/../foo"));
        //
        assertEquals("foo", tnf.normalizeRootBasedName("a/b/*/../../foo"));
        //
        assertThrowsWithBackingOutException("a/b/*/../../../foo", tnf);
        //
        assertEquals("a/*/foo", tnf.normalizeRootBasedName("a/b/*/*/../foo"));
        //
        assertEquals("a/b/*/foo", tnf.normalizeRootBasedName("a/b/*/c/*/../foo"));
        //
        assertEquals("a/b/*/foo", tnf.normalizeRootBasedName("a/b/*/c/d/*/../../foo"));
        //
        assertEquals("a/*/b/*/foo", tnf.normalizeRootBasedName("a/*//b/*/c/d/*/../../foo"));
        //
        assertEquals("", tnf.normalizeRootBasedName("a/../*"));
        //
        assertEquals("", tnf.normalizeRootBasedName("a/../*/"));
        
        // ".." and "scheme"
        assertThrowsWithBackingOutException("x://../foo", tnf);
        //
        assertThrowsWithBackingOutException("x://../../foo", tnf);
        //
        assertThrowsWithBackingOutException("x:../foo", tnf);
        //
        assertThrowsWithBackingOutException("x:../../foo", tnf);

        // Tricky cases with terminating "/":
        assertEquals("", tnf.normalizeRootBasedName("/"));
        // Terminating "/.." (produces terminating "/"):
        assertEquals("foo/", tnf.normalizeRootBasedName("foo/bar/.."));
        // Terminating "/." (produces terminating "/"):
        assertEquals("foo/bar/", tnf.normalizeRootBasedName("foo/bar/."));
        
        // Lonely "."
        assertEquals("", tnf.normalizeRootBasedName("."));
        // Lonely ".."
        assertThrowsWithBackingOutException("..", tnf);
        // Lonely "*"
        
        // Eliminating redundant "//":
        assertEquals("foo/bar", tnf.normalizeRootBasedName("foo//bar"));
        //
        assertEquals("foo/bar/baaz/wombat", tnf.normalizeRootBasedName("////foo//bar///baaz////wombat"));
        //
        assertEquals("scheme://foo", tnf.normalizeRootBasedName("scheme://foo"));
        //
        assertEquals("scheme://foo/x/y", tnf.normalizeRootBasedName("scheme://foo//x/y"));
        //
        assertEquals("scheme://foo", tnf.normalizeRootBasedName("scheme:///foo"));
        //
        assertEquals("scheme://foo", tnf.normalizeRootBasedName("scheme:////foo"));
        
        // Eliminating redundant "*"-s:
        assertEquals("a/*/b", tnf.normalizeRootBasedName("a/*/*/b"));
        //
        assertEquals("a/*/b", tnf.normalizeRootBasedName("a/*/*/*/b"));
        //
        assertEquals("b", tnf.normalizeRootBasedName("*/*/b"));
        //
        assertEquals("b", tnf.normalizeRootBasedName("/*/*/b"));
        //
        assertEquals("b/*", tnf.normalizeRootBasedName("b/*/*"));
        //
        assertEquals("b/*", tnf.normalizeRootBasedName("b/*/*/*"));
        //
        assertEquals("a/*/b/*/c", tnf.normalizeRootBasedName("*/a/*/b/*/*/c"));
        
        assertEquals("s:a/b", tnf.normalizeRootBasedName("s:a/b"));
        assertEquals("s:a/b", tnf.normalizeRootBasedName("s:/a/b"));
        assertEquals("s://a/b", tnf.normalizeRootBasedName("s://a/b"));
        assertEquals("s://a/b", tnf.normalizeRootBasedName("s:///a/b"));
        assertEquals("s://a/b", tnf.normalizeRootBasedName("s:////a/b"));
        
        // Illegal use a of ":":
        assertNormRBNameThrowsColonException("a/b:c/d", tnf);
        assertNormRBNameThrowsColonException("a/b:/..", tnf);
    }

    @Test
    public void testRootBasedNameToAbsoluteName() throws MalformedTemplateNameException {
        final TemplateNameFormat tnf = DefaultTemplateNameFormat.INSTANCE;
        
        assertEquals("/foo/bar", tnf.rootBasedNameToAbsoluteName("foo/bar"));
        assertEquals("scheme://foo/bar", tnf.rootBasedNameToAbsoluteName("scheme://foo/bar"));
        assertEquals("/foo/bar", tnf.rootBasedNameToAbsoluteName("/foo/bar"));
        // Lenient handling of malformed rootBasedName:
        assertEquals("/a/b://c/d", tnf.rootBasedNameToAbsoluteName("a/b://c/d"));
        assertEquals("b:/c/d", tnf.rootBasedNameToAbsoluteName("b:/c/d"));
        assertEquals("b:c/d", tnf.rootBasedNameToAbsoluteName("b:c/d"));
    }    
    
    @Test
    public void testBackslashNotAllowed() throws IOException {
        Configuration cfg = new TestConfigurationBuilder()
                .templateLoader(new ByteArrayTemplateLoader())
                .templateNameFormat(DefaultTemplateNameFormat.INSTANCE)
                .build();
        try {
            cfg.getTemplate("././foo\\bar.f3ah", Locale.US);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("backslash"));
        }
        
    }

    private void assertThrowsWithBackingOutException(final String name, final TemplateNameFormat tnf) {
        try {
            tnf.normalizeRootBasedName(name);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertEquals(name, e.getTemplateName());
            assertBackingOutFromRootException(e);
        }
    }

    private void assertNormRBNameThrowsColonException(final String name, final TemplateNameFormat tnf)
            throws MalformedTemplateNameException {
        try {
            tnf.normalizeRootBasedName(name);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertEquals(name, e.getTemplateName());
            assertColonException(e);
        }
    }
    
    private void assertBackingOutFromRootException(MalformedTemplateNameException e) {
        assertThat(e.getMessage(), containsStringIgnoringCase("backing out"));
    }

    private void assertColonException(MalformedTemplateNameException e) {
        assertThat(e.getMessage(), containsString("':'"));
    }
    
}
