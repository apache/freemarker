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

package freemarker.template;

import static org.junit.Assert.*;

import java.sql.Date;

import org.junit.Test;

@SuppressWarnings("boxing")
public class VersionTest {

    @Test
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

    @Test
    public void testFromNumber2() {
        Version v = new Version(1, 2, 3, "beta8", Boolean.TRUE, new Date(5000));
        assertEquals("1.2.3-beta8", v.toString());
        assertEquals("beta8", v.getExtraInfo());
        assertTrue(v.isGAECompliant().booleanValue());
        assertEquals(new Date(5000), v.getBuildDate());
    }

    @Test
    public void testFromNumber3() {
        Version v = new Version(new Version(1, 2, 3).intValue());
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getMicro());
    }
    
    public void testFromNumberIncubating() {
        Version v = new Version(2, 3, 24, "rc01-incubating", Boolean.FALSE, new Date(5000));
        assertEquals("2.3.24-rc01-incubating", v.toString());
        assertEquals("rc01-incubating", v.getExtraInfo());
        assertFalse(v.isGAECompliant().booleanValue());
        assertEquals(new Date(5000), v.getBuildDate());
    }
    
    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testFromStringIncubating() {
        Version v = new Version("2.3.24-rc01-incubating");
        assertEquals("2.3.24-rc01-incubating", v.toString());
        assertEquals(2, v.getMajor());
        assertEquals(3, v.getMinor());
        assertEquals(24, v.getMicro());
        assertEquals("rc01-incubating", v.getExtraInfo());
    }
    
    @Test
    public void testHashAndEquals() {
        Version v1 = new Version("1.2.3-beta2");
        Version v2 = new Version(1, 2, 3, "beta2", null, null);
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        
        v2 = new Version("1.2.3-beta3");
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version(1, 2, 3, "beta2", true, null);
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version(1, 2, 3, "beta2", null, new Date(5000));
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version("1.2.9-beta2");
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version("1.9.3-beta2");
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version("9.2.3-beta2");
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
        
        v2 = new Version("1.2.3");
        assertTrue(!v1.equals(v2));
        assertTrue(v1.hashCode() != v2.hashCode());
    }

    @Test
    public void testShortForms() {
        Version v = new Version("1.0.0-beta2");
        assertEquals(v, new Version("1.0-beta2"));
        assertEquals(v, new Version("1-beta2"));

        v = new Version("1.0.0");
        assertEquals(v, new Version("1.0"));
        assertEquals(v, new Version("1"));
    }
    
    @Test
    public void testMalformed() {
        try {
            new Version("1.2.");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            new Version("1.2.3.");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            new Version("1..3");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new Version(".2");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            new Version("a");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            new Version("-a");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    
}
