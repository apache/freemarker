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

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("boxing")
public class CustomAttributeTest {
    
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";
    private static final String KEY_3 = "key3";
    
    private static final Object VALUE_1 = new Object();
    private static final Object VALUE_2 = new Object();
    private static final Object VALUE_3 = new Object();
    private static final Object VALUE_LIST = ImmutableList.<Object>of(
            "s", BigDecimal.valueOf(2), Boolean.TRUE, ImmutableMap.of("a", "A"));
    private static final Object VALUE_BIGDECIMAL = BigDecimal.valueOf(22);

    private static final Object CUST_ATT_KEY = new Object();

    @Test
    public void testStringKey() throws Exception {
        Template t = new Template(null, "", new Configuration(Configuration.VERSION_3_0_0));
        assertEquals(0, t.getCustomAttributeNames().length);        
        assertNull(t.getCustomAttribute(KEY_1));
        
        t.setCustomAttribute(KEY_1, VALUE_1);
        assertArrayEquals(new String[] { KEY_1 }, t.getCustomAttributeNames());        
        assertSame(VALUE_1, t.getCustomAttribute(KEY_1));
        
        t.setCustomAttribute(KEY_2, VALUE_2);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(t.getCustomAttributeNames()));        
        assertSame(VALUE_1, t.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, t.getCustomAttribute(KEY_2));
        
        t.setCustomAttribute(KEY_1, VALUE_2);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(t.getCustomAttributeNames()));        
        assertSame(VALUE_2, t.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, t.getCustomAttribute(KEY_2));
        
        t.setCustomAttribute(KEY_1, null);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(t.getCustomAttributeNames()));        
        assertNull(t.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, t.getCustomAttribute(KEY_2));
        
        t.removeCustomAttribute(KEY_1);
        assertArrayEquals(new String[] { KEY_2 }, t.getCustomAttributeNames());        
        assertNull(t.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, t.getCustomAttribute(KEY_2));
    }

    @Test
    public void testRemoveFromEmptySet() throws Exception {
        Template t = new Template(null, "", new Configuration(Configuration.VERSION_3_0_0));
        t.removeCustomAttribute(KEY_1);
        assertEquals(0, t.getCustomAttributeNames().length);        
        assertNull(t.getCustomAttribute(KEY_1));
        
        t.setCustomAttribute(KEY_1, VALUE_1);
        assertArrayEquals(new String[] { KEY_1 }, t.getCustomAttributeNames());        
        assertSame(VALUE_1, t.getCustomAttribute(KEY_1));
    }
    
    @Test
    public void testFtlHeader() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': [ 's', 2, true, {  'a': 'A' } ], "
                + "'" + KEY_2 + "': " + VALUE_BIGDECIMAL + " "
                + "}>",
                new Configuration(Configuration.VERSION_3_0_0));
        
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(t.getCustomAttributeNames()));
        assertEquals(VALUE_LIST, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_BIGDECIMAL, t.getCustomAttribute(KEY_2));
        
        t.setCustomAttribute(KEY_1, VALUE_1);
        assertEquals(VALUE_1, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_BIGDECIMAL, t.getCustomAttribute(KEY_2));
    }
    
    @Test
    public void testFtl2Header() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': 'a', "
                + "'" + KEY_2 + "': 'b', "
                + "'" + KEY_3 + "': 'c' "
                + "}>",
                new Configuration(Configuration.VERSION_3_0_0));
        
        assertArrayEquals(new String[] { KEY_1, KEY_2, KEY_3 }, sort(t.getCustomAttributeNames()));
        assertEquals("a", t.getCustomAttribute(KEY_1));
        assertEquals("b", t.getCustomAttribute(KEY_2));
        assertEquals("c", t.getCustomAttribute(KEY_3));
        
        t.removeCustomAttribute(KEY_2);
        assertArrayEquals(new String[] { KEY_1, KEY_3 }, sort(t.getCustomAttributeNames()));
        assertEquals("a", t.getCustomAttribute(KEY_1));
        assertNull(t.getCustomAttribute(KEY_2));
        assertEquals("c", t.getCustomAttribute(KEY_3));
    }

    @Test
    public void testFtl3Header() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': 'a', "
                + "'" + KEY_2 + "': 'b', "
                + "'" + KEY_3 + "': 'c' "
                + "}>",
                new Configuration(Configuration.VERSION_3_0_0));
        
        assertArrayEquals(new String[] { KEY_1, KEY_2, KEY_3 }, sort(t.getCustomAttributeNames()));
        assertEquals("a", t.getCustomAttribute(KEY_1));
        assertEquals("b", t.getCustomAttribute(KEY_2));
        assertEquals("c", t.getCustomAttribute(KEY_3));
        
        t.setCustomAttribute(KEY_2, null);
        assertArrayEquals(new String[] { KEY_1, KEY_2, KEY_3 }, sort(t.getCustomAttributeNames()));
        assertEquals("a", t.getCustomAttribute(KEY_1));
        assertNull(t.getCustomAttribute(KEY_2));
        assertEquals("c", t.getCustomAttribute(KEY_3));
    }
    
    private Object[] sort(String[] customAttributeNames) {
        Arrays.sort(customAttributeNames);
        return customAttributeNames;
    }

    @Test
    public void testObjectKey() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        Template t = new Template(null, "", cfg);
        assertNull(t.getCustomAttribute(CUST_ATT_KEY));
        cfg.setCustomAttribute(CUST_ATT_KEY, "cfg");
        assertEquals("cfg", t.getCustomAttribute(CUST_ATT_KEY));
        t.setCustomAttribute(CUST_ATT_KEY, "t");
        assertEquals("t", t.getCustomAttribute(CUST_ATT_KEY));
    }

}
