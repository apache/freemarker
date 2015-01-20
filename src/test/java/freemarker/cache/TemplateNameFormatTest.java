package freemarker.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TemplateNameFormatTest {

    @Test
    public void testToAbsoluteName() {
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
            // - Scheme:
            assertEquals("s://b", tnf.toAbsoluteName("s://x/", "/b"));
            assertEquals("s://b", tnf.toAbsoluteName("s:///x/", "/b"));
            
            // Schemed absolute paths:
            assertEquals("s://b", tnf.toAbsoluteName("a/", "s://b"));
            assertEquals("s://b", tnf.toAbsoluteName("i://a/", "s://b"));
        }
        
        // Scheme names in 2.4 format:
        final TemplateNameFormat tnf = TemplateNameFormat.DEFAULT_2_4_0;
        assertEquals("s:b", tnf.toAbsoluteName("s:f", "b"));
        assertEquals("s:/b", tnf.toAbsoluteName("s:/f", "b"));
        assertEquals("s:b", tnf.toAbsoluteName("s:f", "/b"));
        assertEquals("s:b", tnf.toAbsoluteName("s:/f", "/b"));
        assertEquals("s:f/b", tnf.toAbsoluteName("s:f/", "b"));
        assertEquals("s:/f/b", tnf.toAbsoluteName("s:/f/", "b"));
        assertEquals("s:b", tnf.toAbsoluteName("s:f/", "/b"));
        assertEquals("s:b", tnf.toAbsoluteName("s:/f/", "/b"));
    }

    @Test
    public void testNormalizeAbsoluteName() {
        // Normalizations that are the same in legacy and modern format:
        for (TemplateNameFormat tnf : new TemplateNameFormat[] {
                TemplateNameFormat.DEFAULT_2_3_0, TemplateNameFormat.DEFAULT_2_4_0 }) {
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
                assertNull(tnf.normalizeAbsoluteName(lead + "bar/../../x/foo"));
                assertNull(tnf.normalizeAbsoluteName(lead + "../x"));
                assertNull(tnf.normalizeAbsoluteName(lead + "../../../x"));
                assertNull(tnf.normalizeAbsoluteName(lead + "foo\u0000"));
            }
            assertEquals("", tnf.normalizeAbsoluteName(""));
            assertNull(tnf.normalizeAbsoluteName("x://../../../foo"));
        }
        
        // Normalizations that differ in legacy and modern format:
        
        final TemplateNameFormat tnf23 = TemplateNameFormat.DEFAULT_2_3_0;
        final TemplateNameFormat tnf24 = TemplateNameFormat.DEFAULT_2_4_0;
        
        // ".." and "."
        assertEquals("bar/foo", tnf23.normalizeAbsoluteName("bar/./../foo"));
        assertEquals("foo", tnf24.normalizeAbsoluteName("bar/./../foo"));
        
        // Even number of leading ".."-s bug: 
        assertEquals("foo", tnf23.normalizeAbsoluteName("../../foo"));
        assertNull(tnf24.normalizeAbsoluteName("../../foo"));
        //
        assertEquals("foo", tnf23.normalizeAbsoluteName("../../../../foo"));
        assertNull(tnf24.normalizeAbsoluteName("../../../../foo"));
        
        // ".." and "*"
        assertEquals("a/b/foo", tnf23.normalizeAbsoluteName("a/b/*/../foo"));
        assertEquals("a/*/foo", tnf24.normalizeAbsoluteName("a/b/*/../foo"));
        //
        assertEquals("a/foo", tnf23.normalizeAbsoluteName("a/b/*/../../foo"));
        assertEquals("foo", tnf24.normalizeAbsoluteName("a/b/*/../../foo"));
        //
        assertEquals("foo", tnf23.normalizeAbsoluteName("a/b/*/../../../foo"));
        assertNull(tnf24.normalizeAbsoluteName("a/b/*/../../../foo"));
        //
        assertEquals("a/b/*/foo", tnf23.normalizeAbsoluteName("a/b/*/*/../foo"));
        assertEquals("a/*/foo", tnf24.normalizeAbsoluteName("a/b/*/*/../foo"));
        //
        assertEquals("a/b/*/c/foo", tnf23.normalizeAbsoluteName("a/b/*/c/*/../foo"));
        assertEquals("a/b/*/foo", tnf24.normalizeAbsoluteName("a/b/*/c/*/../foo"));
        //
        assertEquals("a/b/*/c/foo", tnf23.normalizeAbsoluteName("a/b/*/c/d/*/../../foo"));
        assertEquals("a/b/*/foo", tnf24.normalizeAbsoluteName("a/b/*/c/d/*/../../foo"));
        //
        assertEquals("a/*//b/*/c/foo", tnf23.normalizeAbsoluteName("a/*//b/*/c/d/*/../../foo"));
        assertEquals("a/*/b/*/foo", tnf24.normalizeAbsoluteName("a/*//b/*/c/d/*/../../foo"));
        //
        assertEquals("*", tnf23.normalizeAbsoluteName("a/../*"));
        assertEquals("", tnf24.normalizeAbsoluteName("a/../*"));
        //
        assertEquals("*/", tnf23.normalizeAbsoluteName("a/../*/"));
        assertEquals("", tnf24.normalizeAbsoluteName("a/../*/"));
        
        // ".." and "scheme"
        assertEquals("x:/foo", tnf23.normalizeAbsoluteName("x://../foo"));
        assertNull(tnf24.normalizeAbsoluteName("x://../foo"));
        //
        assertEquals("foo", tnf23.normalizeAbsoluteName("x://../../foo"));
        assertNull(tnf24.normalizeAbsoluteName("x://../../foo"));
        //
        assertEquals("x:../foo", tnf23.normalizeAbsoluteName("x:../foo"));
        assertNull(tnf24.normalizeAbsoluteName("x:../foo"));
        //
        assertEquals("foo", tnf23.normalizeAbsoluteName("x:../../foo"));
        assertNull(tnf24.normalizeAbsoluteName("x:../../foo"));

        // Tricky cases with terminating "/":
        assertEquals("/", tnf23.normalizeAbsoluteName("/"));
        assertEquals("", tnf24.normalizeAbsoluteName("/"));
        // Terminating "/.." (produces terminating "/"):
        assertEquals("foo/bar/..", tnf23.normalizeAbsoluteName("foo/bar/.."));
        assertEquals("foo/", tnf24.normalizeAbsoluteName("foo/bar/.."));
        // Terminating "/." (produces terminating "/"):
        assertEquals("foo/bar/.", tnf23.normalizeAbsoluteName("foo/bar/."));
        assertEquals("foo/bar/", tnf24.normalizeAbsoluteName("foo/bar/."));
        
        // Lonely "."
        assertEquals(".", tnf23.normalizeAbsoluteName("."));
        assertEquals("", tnf24.normalizeAbsoluteName("."));
        // Lonely ".."
        assertEquals("..", tnf23.normalizeAbsoluteName(".."));
        assertNull(tnf24.normalizeAbsoluteName(".."));
        // Lonely "*"
        
        // Eliminating redundant "//":
        assertEquals("foo//bar", tnf23.normalizeAbsoluteName("foo//bar"));
        assertEquals("foo/bar", tnf24.normalizeAbsoluteName("foo//bar"));
        //
        assertEquals("///foo//bar///baaz////wombat", tnf23.normalizeAbsoluteName("////foo//bar///baaz////wombat"));
        assertEquals("foo/bar/baaz/wombat", tnf24.normalizeAbsoluteName("////foo//bar///baaz////wombat"));
        //
        assertEquals("scheme://foo", tnf23.normalizeAbsoluteName("scheme://foo"));
        assertEquals("scheme://foo", tnf24.normalizeAbsoluteName("scheme://foo"));
        //
        assertEquals("scheme://foo//x/y", tnf23.normalizeAbsoluteName("scheme://foo//x/y"));
        assertEquals("scheme://foo/x/y", tnf24.normalizeAbsoluteName("scheme://foo//x/y"));
        //
        assertEquals("scheme:///foo", tnf23.normalizeAbsoluteName("scheme:///foo"));
        assertEquals("scheme://foo", tnf24.normalizeAbsoluteName("scheme:///foo"));
        //
        assertEquals("scheme:////foo", tnf23.normalizeAbsoluteName("scheme:////foo"));
        assertEquals("scheme://foo", tnf24.normalizeAbsoluteName("scheme:////foo"));
        
        // Eliminating redundant "*"-s:
        assertEquals("a/*/*/b", tnf23.normalizeAbsoluteName("a/*/*/b"));
        assertEquals("a/*/b", tnf24.normalizeAbsoluteName("a/*/*/b"));
        //
        assertEquals("a/*/*/*/b", tnf23.normalizeAbsoluteName("a/*/*/*/b"));
        assertEquals("a/*/b", tnf24.normalizeAbsoluteName("a/*/*/*/b"));
        //
        assertEquals("*/*/b", tnf23.normalizeAbsoluteName("*/*/b"));
        assertEquals("b", tnf24.normalizeAbsoluteName("*/*/b"));
        //
        assertEquals("*/*/b", tnf23.normalizeAbsoluteName("/*/*/b"));
        assertEquals("b", tnf24.normalizeAbsoluteName("/*/*/b"));
        //
        assertEquals("b/*/*", tnf23.normalizeAbsoluteName("b/*/*"));
        assertEquals("b/*", tnf24.normalizeAbsoluteName("b/*/*"));
        //
        assertEquals("b/*/*/*", tnf23.normalizeAbsoluteName("b/*/*/*"));
        assertEquals("b/*", tnf24.normalizeAbsoluteName("b/*/*"));
        //
        assertEquals("*/a/*/b/*/*/c", tnf23.normalizeAbsoluteName("*/a/*/b/*/*/c"));
        assertEquals("a/*/b/*/c", tnf24.normalizeAbsoluteName("*/a/*/b/*/*/c"));
        
        // New kind of scheme handling:
        
        assertEquals("s:a/b", tnf24.normalizeAbsoluteName("s:a/b"));
        assertEquals("s:a/b", tnf24.normalizeAbsoluteName("s:/a/b"));
        assertEquals("s://a/b", tnf24.normalizeAbsoluteName("s://a/b"));
        assertEquals("s://a/b", tnf24.normalizeAbsoluteName("s:///a/b"));
        assertEquals("s://a/b", tnf24.normalizeAbsoluteName("s:////a/b"));
        // Illegal use a of ":":
        assertNull(tnf24.normalizeAbsoluteName("a/b:c/d"));
        assertNull(tnf24.normalizeAbsoluteName("a/b:/.."));
    }

}
