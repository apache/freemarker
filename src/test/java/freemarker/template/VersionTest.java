package freemarker.template;

import java.sql.Date;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

    public VersionTest(String name) {
        super(name);
    }
    
    public void testFromNumber() {
        Version v = new Version(1, 2, 3);
        assertEquals("1.2.3", v.toString());
        assertEquals(1002003, v.intValue());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
        assertNull(v.getExtraInfo());
        assertNull(v.isGAECompliant());
        assertNull(v.getBuildDate());
    }

    public void testFromNumber2() {
        Version v = new Version(1, 2, 3, "beta8", Boolean.TRUE, new Date(5000));
        assertEquals("1.2.3-beta8", v.toString());
        assertEquals("beta8", v.getExtraInfo());
        assertTrue(v.isGAECompliant().booleanValue());
        assertEquals(new Date(5000), v.getBuildDate());
    }
    
    public void testFromString() {
        Version v = new Version("1.2.3-beta2");
        assertEquals("1.2.3-beta2", v.toString());
        assertEquals(1002003, v.intValue());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
        assertEquals("beta2", v.getExtraInfo());
        assertNull(v.isGAECompliant());
        assertNull(v.getBuildDate());
    }

    public void testFromString2() {
        Version v = new Version("10.20.30", Boolean.TRUE, new Date(5000));
        assertEquals("10.20.30", v.toString());
        assertEquals(10020030, v.intValue());
        assertEquals(10, v.getMajor());
        assertEquals(20, v.getMinor());
        assertEquals(30, v.getMicro());
        assertNull(v.getExtraInfo());
        assertTrue(v.isGAECompliant().booleanValue());
        assertEquals(new Date(5000), v.getBuildDate());
    }

    public void testFromString3() {
        Version v = new Version("01.002.0003-20130524");
        assertEquals("01.002.0003-20130524", v.toString());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
        assertEquals("20130524", v.getExtraInfo());

        v = new Version("01.002.0003.4");
        assertEquals("01.002.0003.4", v.toString());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
        assertEquals("4", v.getExtraInfo());
        
        v = new Version("1.2.3.FC");
        assertEquals("1.2.3.FC", v.toString());
        assertEquals("FC", v.getExtraInfo());
        
        v = new Version("1.2.3mod");
        assertEquals("1.2.3mod", v.toString());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
        assertEquals("mod", v.getExtraInfo());
        
    }
    
}
