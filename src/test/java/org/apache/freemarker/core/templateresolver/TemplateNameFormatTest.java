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

import static org.apache.freemarker.test.hamcerst.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Locale;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ParseException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.test.MonitoredTemplateLoader;
import org.junit.Test;

import com.google.common.collect.ImmutableList;


public class TemplateNameFormatTest {

    @Test
    public void testToRootBasedName() throws MalformedTemplateNameException {
        // Path that are treated the same both in 2.3 and 2.4 format:
        for (TemplateNameFormat tnf : new TemplateNameFormat[] {
                DefaultTemplateNameFormatFM2.INSTANCE, DefaultTemplateNameFormat.INSTANCE }) {
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
        
        // Scheme names in 2.4 format only:
        {
            final TemplateNameFormat tnf = DefaultTemplateNameFormat.INSTANCE;
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
        }
        
        // Scheme names in 2.3 format only:
        {
            final TemplateNameFormat tnf = DefaultTemplateNameFormatFM2.INSTANCE;
            assertEquals("a/s://b", tnf.toRootBasedName("a/s://f/", "/b"));
        }
    }

    @Test
    public void testNormalizeRootBasedName() throws MalformedTemplateNameException {
        // Normalizations that are the same in legacy and modern format:
        for (TemplateNameFormat tnf : new TemplateNameFormat[] {
                DefaultTemplateNameFormatFM2.INSTANCE, DefaultTemplateNameFormat.INSTANCE }) {
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
        }
        
        // ".." and "."
        assertEqualsOn23AndOn24("bar/foo", "foo", "bar/./../foo");
        
        // Even number of leading ".."-s bug:
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("foo", "../../foo");
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("foo", "../../../../foo");
        
        // ".." and "*"
        assertEqualsOn23AndOn24("a/b/foo", "a/*/foo", "a/b/*/../foo");
        //
        assertEqualsOn23AndOn24("a/foo", "foo", "a/b/*/../../foo");
        //
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("foo", "a/b/*/../../../foo");
        //
        assertEqualsOn23AndOn24("a/b/*/foo", "a/*/foo", "a/b/*/*/../foo");
        //
        assertEqualsOn23AndOn24("a/b/*/c/foo", "a/b/*/foo", "a/b/*/c/*/../foo");
        //
        assertEqualsOn23AndOn24("a/b/*/c/foo", "a/b/*/foo", "a/b/*/c/d/*/../../foo");
        //
        assertEqualsOn23AndOn24("a/*//b/*/c/foo", "a/*/b/*/foo", "a/*//b/*/c/d/*/../../foo");
        //
        assertEqualsOn23AndOn24("*", "", "a/../*");
        //
        assertEqualsOn23AndOn24("*/", "", "a/../*/");
        
        // ".." and "scheme"
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("x:/foo", "x://../foo");
        //
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("foo", "x://../../foo");
        //
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("x:../foo", "x:../foo");
        //
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("foo", "x:../../foo");

        // Tricky cases with terminating "/":
        assertEqualsOn23AndOn24("/", "", "/");
        // Terminating "/.." (produces terminating "/"):
        assertEqualsOn23AndOn24("foo/bar/..", "foo/", "foo/bar/..");
        // Terminating "/." (produces terminating "/"):
        assertEqualsOn23AndOn24("foo/bar/.", "foo/bar/", "foo/bar/.");
        
        // Lonely "."
        assertEqualsOn23AndOn24(".", "", ".");
        // Lonely ".."
        assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24("..", "..");
        // Lonely "*"
        
        // Eliminating redundant "//":
        assertEqualsOn23AndOn24("foo//bar", "foo/bar", "foo//bar");
        //
        assertEqualsOn23AndOn24("///foo//bar///baaz////wombat", "foo/bar/baaz/wombat", "////foo//bar///baaz////wombat");
        //
        assertEqualsOn23AndOn24("scheme://foo", "scheme://foo", "scheme://foo");
        //
        assertEqualsOn23AndOn24("scheme://foo//x/y", "scheme://foo/x/y", "scheme://foo//x/y");
        //
        assertEqualsOn23AndOn24("scheme:///foo", "scheme://foo", "scheme:///foo");
        //
        assertEqualsOn23AndOn24("scheme:////foo", "scheme://foo", "scheme:////foo");
        
        // Eliminating redundant "*"-s:
        assertEqualsOn23AndOn24("a/*/*/b", "a/*/b", "a/*/*/b");
        //
        assertEqualsOn23AndOn24("a/*/*/*/b", "a/*/b", "a/*/*/*/b");
        //
        assertEqualsOn23AndOn24("*/*/b", "b", "*/*/b");
        //
        assertEqualsOn23AndOn24("*/*/b", "b", "/*/*/b");
        //
        assertEqualsOn23AndOn24("b/*/*", "b/*", "b/*/*");
        //
        assertEqualsOn23AndOn24("b/*/*/*", "b/*", "b/*/*/*");
        //
        assertEqualsOn23AndOn24("*/a/*/b/*/*/c", "a/*/b/*/c", "*/a/*/b/*/*/c");
        
        // New kind of scheme handling:

        assertEquals("s:a/b", DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName("s:a/b"));
        assertEquals("s:a/b", DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName("s:/a/b"));
        assertEquals("s://a/b", DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName("s://a/b"));
        assertEquals("s://a/b", DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName("s:///a/b"));
        assertEquals("s://a/b", DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName("s:////a/b"));
        
        // Illegal use a of ":":
        assertNormRBNameThrowsColonExceptionOn24("a/b:c/d");
        assertNormRBNameThrowsColonExceptionOn24("a/b:/..");
    }
    
    @Test
    public void assertBackslashNotSpecialWith23() throws MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);

        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("foo\\bar.ftl", "");
        cfg.setTemplateLoader(tl);

        {
            final String name = "foo\\bar.ftl";
            
            Template t = cfg.getTemplate(name, Locale.US);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(
                    ImmutableList.of(
                            "foo\\bar_en_US.ftl",
                            "foo\\bar_en.ftl",
                            name),
                    tl.getLoadNames());
            tl.clearEvents();
        }

        try {
            cfg.getTemplate("foo\\missing.ftl", Locale.US);
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("foo\\missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of(
                            "foo\\missing_en_US.ftl",
                            "foo\\missing_en.ftl",
                            "foo\\missing.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final String name = "foo/bar\\..\\bar.ftl";
            try {
                cfg.getTemplate(name, Locale.US);
                fail();
            } catch (TemplateNotFoundException e) {
                assertEquals(name, e.getTemplateName());
            }
        }
        
    }

    @Test
    public void assertBackslashNotAllowed() throws MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        cfg.setTemplateLoader(new ByteArrayTemplateLoader());
        cfg.setTemplateNameFormat(DefaultTemplateNameFormat.INSTANCE);
        try {
            cfg.getTemplate("././foo\\bar.ftl", Locale.US);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("backslash"));
        }
        
    }
    
    private void assertEqualsOn23AndOn24(String expected23, String expected24, String name)
            throws MalformedTemplateNameException {
        assertEquals(expected23, DefaultTemplateNameFormatFM2.INSTANCE.normalizeRootBasedName(name));
        assertEquals(expected24, DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName(name));
    }

    private void assertNormRBNameEqualsOn23ButThrowsBackOutExcOn24(final String expected23, final String name)
            throws MalformedTemplateNameException {
        assertEquals(expected23, DefaultTemplateNameFormatFM2.INSTANCE.normalizeRootBasedName(name));
        assertThrowsWithBackingOutException(name, DefaultTemplateNameFormat.INSTANCE);
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

    private void assertNormRBNameThrowsColonExceptionOn24(final String name) throws MalformedTemplateNameException {
        try {
            DefaultTemplateNameFormat.INSTANCE.normalizeRootBasedName(name);
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
