package freemarker.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import freemarker.template.MalformedTemplateNameException;

public class TemplateNameFormatTest {

    @Test
    public void testToAbsoluteName() throws MalformedTemplateNameException {
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
            }
        }
        
        // Normalizations that differ in legacy and modern format:
        
        for (String lead : new String[] { "", "/" }) {
            assertNormAbsNameIsNullOn23ButThrowsBackOutExcOn24(lead + "bar/../../x/foo");
            assertNormAbsNameIsNullOn23ButThrowsBackOutExcOn24(lead + "../x");
            assertNormAbsNameIsNullOn23ButThrowsBackOutExcOn24(lead + "../../../x");

            {
                final String name = lead + "foo\u0000";
                assertNull(TemplateNameFormat.DEFAULT_2_3_0.normalizeAbsoluteName(name));
                try {
                    TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName(name);
                    fail();
                } catch (MalformedTemplateNameException e) {
                    assertEquals(name, e.getTemplateName());
                    assertTrue(e.getMalformednessDescription().toLowerCase().contains("null character"));
                }
            }
        } // for lead
        
        assertNormAbsNameIsNullOn23ButThrowsBackOutExcOn24("x://../../../foo");
        
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
    
    private void assertEqualsOn23AndOn24(String expected23, String expected24, String name)
            throws MalformedTemplateNameException {
        assertEquals(expected23, TemplateNameFormat.DEFAULT_2_3_0.normalizeAbsoluteName(name));
        assertEquals(expected24, TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName(name));
    }

    private void assertNormAbsNameIsNullOn23ButThrowsBackOutExcOn24(final String name) throws MalformedTemplateNameException {
        assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24(null, name);
    }

    private void assertNormAbsNameEqualsOn23ButThrowsBackOutExcOn24(final String expected23, final String name) throws MalformedTemplateNameException {
        assertEquals(expected23, TemplateNameFormat.DEFAULT_2_3_0.normalizeAbsoluteName(name));
        try {
            TemplateNameFormat.DEFAULT_2_4_0.normalizeAbsoluteName(name);
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
        assertTrue(e.getMessage().toLowerCase().contains("backing out"));
    }

    private void assertColonException(MalformedTemplateNameException e) {
        assertTrue(e.getMessage().toLowerCase().contains("':'"));
    }
    
}
