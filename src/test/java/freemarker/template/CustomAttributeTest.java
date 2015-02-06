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

package freemarker.template;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.core.CustomAttribute;


public class CustomAttributeTest {
    
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";
    private static final String KEY_3 = "key3";
    
    private static final Object VALUE_1 = new Object();
    private static final Object VALUE_2 = new Object();
    private static final Object VALUE_3 = new Object();
    
    private static final Object VALUE_FTL_1 = ImmutableList.<Object>of(
            "s", BigDecimal.valueOf(2), Boolean.TRUE, ImmutableMap.<String, String>of("a", "A"));
    private static final Object VALUE_FTL_2 = BigDecimal.valueOf(22);

    private static final CustomAttribute CUST_ATT_1 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE);
    private static final CustomAttribute CUST_ATT_2 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE);
    
    @Test
    public void testStringKey() throws Exception {
        Template t = new Template(null, "", new Configuration(Configuration.VERSION_2_3_22));
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
    public void testFtlHeader() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': [ 's', 2, true, {  'a': 'A' } ], "
                + "'" + KEY_2 + "': " + VALUE_FTL_2 + " "
                + "}>",
                new Configuration(Configuration.VERSION_2_3_22));
        
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(t.getCustomAttributeNames()));
        assertEquals(VALUE_FTL_1, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_FTL_2, t.getCustomAttribute(KEY_2));
        
        t.setCustomAttribute(KEY_1, VALUE_1);
        assertEquals(VALUE_1, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_FTL_2, t.getCustomAttribute(KEY_2));
    }
    
    @Test
    public void testFtl2Header() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': 'a', "
                + "'" + KEY_2 + "': 'b', "
                + "'" + KEY_3 + "': 'c' "
                + "}>",
                new Configuration(Configuration.VERSION_2_3_22));
        
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
                new Configuration(Configuration.VERSION_2_3_22));
        
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
        Template t = new Template(null, "", new Configuration(Configuration.VERSION_2_3_22));
        assertNull(CUST_ATT_1.get(t));
        
        CUST_ATT_1.set(VALUE_1, t);
        assertSame(VALUE_1, CUST_ATT_1.get(t));
        assertEquals(0, t.getCustomAttributeNames().length);
        
        t.setCustomAttribute(KEY_2, VALUE_2);
        assertArrayEquals(new String[] { KEY_2 }, t.getCustomAttributeNames());        
        assertSame(VALUE_1, CUST_ATT_1.get(t));
        assertSame(VALUE_2, t.getCustomAttribute(KEY_2));
        
        CUST_ATT_2.set(VALUE_3, t);
        assertSame(VALUE_3, CUST_ATT_2.get(t));
        assertArrayEquals(new String[] { KEY_2 }, t.getCustomAttributeNames());        
    }

}
