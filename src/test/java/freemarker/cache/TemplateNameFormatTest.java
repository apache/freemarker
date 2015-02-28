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

package freemarker.cache;

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.test.MonitoredTemplateLoader;


public class TemplateNameFormatTest {

    @Test
    public void testToAbsoluteName() throws MalformedTemplateNameException {
        // Path that are treated the same both in 2.3 and 2.4 format:
        for (TemplateNameFormat tnf : new TemplateNameFormat[] {
                TemplateNameFormat.DEFAULT_2_3_0, TemplateNameFormat.DEFAULT_2_4_0 }) {
            // Relative paths:
            // - No scheme:
            assertEquals("a/b", tnf.toAbsoluteName("a/", "b"));
            assertEquals("/a/b", tnf.toAbsoluteName("/a/", "b"));
            assertEquals("a/b", tnf.toAbsoluteName("a/f", "b"));
            assertEquals("/a/b", tnf.toAbsoluteName("/a/f", "b"));
            // - Scheme:
            assertEquals("s://a/b", tnf.toAbsoluteName("s://a/", "b"));
            assertEquals("s:///a/b", tnf.toAbsoluteName("s:///a/", "b"));
            assertEquals("s://a/b", tnf.toAbsoluteName("s://a/f", "b"));
            assertEquals("s:///a/b", tnf.toAbsoluteName("s:///a/f", "b"));
            assertEquals("s://b", tnf.toAbsoluteName("s://f", "b"));
            assertEquals("s:///b", tnf.toAbsoluteName("s:///f", "b"));
            
            // Absolute paths:
            // - No scheme:
            assertEquals("b", tnf.toAbsoluteName("a/", "/b"));
            assertEquals("b", tnf.toAbsoluteName("/a/", "/b"));
            assertEquals("b", tnf.toAbsoluteName("a/s:/f/", "/b"));
            // - Scheme:
            assertEquals("s://b", tnf.toAbsoluteName("s://x/", "/b"));
            assertEquals("s://b", tnf.toAbsoluteName("s:///x/", "/b"));
            
            // Schemed absolute paths:
            assertEquals("s://b", tnf.toAbsoluteName("a/", "s://b"));
            assertEquals("s://b", tnf.toAbsoluteName("i://a/", "s://b"));
        }
        
        // Scheme names in 2.4 format only:
        {
            final TemplateNameFormat tnf = TemplateNameFormat.DEFAULT_2_4_0;
            assertEquals("s:b", tnf.toAbsoluteName("s:f", "b"));
            assertEquals("s:/b", tnf.toAbsoluteName("s:/f", "b"));
            assertEquals("s:b", tnf.toAbsoluteName("s:f", "/b"));
            assertEquals("s:b", tnf.toAbsoluteName("s:/f", "/b"));
            assertEquals("s:f/b", tnf.toAbsoluteName("s:f/", "b"));
            assertEquals("s:/f/b", tnf.toAbsoluteName("s:/f/", "b"));
            assertEquals("s:b", tnf.toAbsoluteName("s:f/", "/b"));
            assertEquals("s:b", tnf.toAbsoluteName("s:/f/", "/b"));
            assertEquals("s:b", tnf.toAbsoluteName("s:/f/", "/b"));
            assertEquals("b", tnf.toAbsoluteName("a/s://f/", "/b"));
        }
        
        // Scheme names in 2.3 format only:
        {
            final TemplateNameFormat tnf = TemplateNameFormat.DEFAULT_2_3_0;
            assertEquals("a/s://b", tnf.toAbsoluteName("a/s://f/", "/b"));
        }
    }

    @Test
    public void testNormalizeAbsoluteName() throws MalformedTemplateNameException {
        // Normalizations that are the same in legacy and modern format:
        for (TemplateNameFormat tnf : new TemplateNameFormat[] {
                TemplateNameFormat.DEFAULT_2_3_0, TemplateNameFormat.DEFAULT_2_4_0 }) {
            assertEquals("", tnf.normalizeAbsoluteName(""));
            for (String lead : new String[] { "", "/" }) {
                assertEquals("foo", tnf.normalizeAbsoluteName(lead + "foo"));
                assertEquals("foo", tnf.normalizeAbsoluteName(lead + "./foo"));
                assertEquals("foo", tnf.normalizeAbsoluteName(lead + "./././foo"));
                assertEquals("foo", tnf.normalizeAbsoluteName(lead + "bar/../foo"));
                assertEquals("a/b/", tnf.normalizeAbsoluteName("a/b/"));
                assertEquals("a/", tnf.normalizeAbsoluteName("a/b/../"));
                assertEquals("a/c../..d/e*/*f", tnf.normalizeAbsoluteName("a/c../..d/e*/*f"));
                assertEquals("", tnf.normalizeAbsoluteName(""));
                assertEquals("foo/bar/*", tnf.normalizeAbsoluteName("foo/bar/*"));
                assertEquals("schema://", tnf.normalizeAbsoluteName("schema://"));
                
                assertThrowsWithBackingOutException(lead + "bar/../../x/foo", tnf);
                assertThrowsWithBackingOutException(lead + "../x", tnf);
                assertThrowsWithBackingOutException(lead + "../../../x", tnf);
                assertThrowsWithBackingOutException(lead + "../../../x", tnf);
                assertThrowsWithBackingOutException("x://../../../foo", tnf);
                
                {
                    final String name = lead + "foo\u0000";
                    try {
                        tnf.normalizeAbsoluteName(name);
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
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("foo", "../../foo");
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("foo", "../../../../foo");
        
        // ".." and "*"
        assertEqualsOn23AndOn24("a/b/foo", "a/*/foo", "a/b/*/../foo");
        //
        assertEqualsOn23AndOn24("a/foo", "foo", "a/b/*/../../foo");
        //
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("foo", "a/b/*/../../../foo");
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
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("x:/foo", "x://../foo");
        //
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("foo", "x://../../foo");
        //
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("x:../foo", "x:../foo");
        //
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("foo", "x:../../foo");

        // Tricky cases with terminating "/":
        assertEqualsOn23AndOn24("/", "", "/");
        // Terminating "/.." (produces terminating "/"):
        assertEqualsOn23AndOn24("foo/bar/..", "foo/", "foo/bar/..");
        // Terminating "/." (produces terminating "/"):
        assertEqualsOn23AndOn24("foo/bar/.", "foo/bar/", "foo/bar/.");
        
        // Lonely "."
        assertEqualsOn23AndOn24(".", "", ".");
        // Lonely ".."
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24("..", "..");
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

        assertEquals("s:a/b", TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName("s:a/b"));
        assertEquals("s:a/b", TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName("s:/a/b"));
        assertEquals("s://a/b", TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName("s://a/b"));
        assertEquals("s://a/b", TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName("s:///a/b"));
        assertEquals("s://a/b", TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName("s:////a/b"));
        
        // Illegal use a of ":":
        assertNormAbsNameThrowsColonExceptionOn24("a/b:c/d");
        assertNormAbsNameThrowsColonExceptionOn24("a/b:/..");
    }
    
    @Test
    public void assertBackslashNotSpecialWith23() throws MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);

        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTemplate("foo\\bar.ftl", "");
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
                    tl.getTemplatesTried());
            tl.clear();
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
                    tl.getTemplatesTried());
            tl.clear();
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
    public void assertBackslashNotAllowedWith24() throws MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        try {
            cfg.getTemplate("././foo\\bar.ftl", Locale.US);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("backslash"));
        }
        
    }
    
    private void assertEqualsOn23AndOn24(String expected23, String expected24, String name)
            throws MalformedTemplateNameException {
        assertEquals(expected23, TemplateNameFormat.DEFAULT_2_3_0.normalizeAbsoluteName(name));
        assertEquals(expected24, TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName(name));
    }

    private void assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24(final String expected23, final String name)
            throws MalformedTemplateNameException {
        assertEquals(expected23, TemplateNameFormat.DEFAULT_2_3_0.normalizeAbsoluteName(name));
        assertThrowsWithBackingOutException(name, TemplateNameFormat.DEFAULT_2_4_0);
    }

    private void assertThrowsWithBackingOutException(final String name, final TemplateNameFormat tnf) {
        try {
            tnf.normalizeAbsoluteName(name);
            fail();
        } catch (MalformedTemplateNameException e) {
            assertEquals(name, e.getTemplateName());
            assertBackingOutFromRootException(e);
        }
    }

    private void assertNormAbsNameThrowsColonExceptionOn24(final String name) throws MalformedTemplateNameException {
        try {
            TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName(name);
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
