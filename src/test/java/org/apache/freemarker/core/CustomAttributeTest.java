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
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("boxing")
public class CustomAttributeTest {
    
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";
    private static final String KEY_3 = "key3";
    private static final Integer KEY_4 = 4;

    private static final Integer VALUE_1 = 1; // Serializable
    private static final Object VALUE_2 = new Object();
    private static final Object VALUE_3 = new Object();
    private static final Object VALUE_4 = new Object();
    private static final Object VALUE_LIST = ImmutableList.<Object>of(
            "s", BigDecimal.valueOf(2), Boolean.TRUE, ImmutableMap.of("a", "A"));
    private static final Object VALUE_BIGDECIMAL = BigDecimal.valueOf(22);

    private static final Object CUST_ATT_KEY = new Object();

    @Test
    public void testStringKey() throws Exception {
        // Need some MutableProcessingConfiguration:
        TemplateConfiguration.Builder mpc = new TemplateConfiguration.Builder();

        assertEquals(0, mpc.getCustomAttributeNames().length);
        assertNull(mpc.getCustomAttribute(KEY_1));
        
        mpc.setCustomAttribute(KEY_1, VALUE_1);
        assertArrayEquals(new String[] { KEY_1 }, mpc.getCustomAttributeNames());
        assertSame(VALUE_1, mpc.getCustomAttribute(KEY_1));
        
        mpc.setCustomAttribute(KEY_2, VALUE_2);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(mpc.getCustomAttributeNames()));
        assertSame(VALUE_1, mpc.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, mpc.getCustomAttribute(KEY_2));

        mpc.setCustomAttribute(KEY_1, VALUE_2);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(mpc.getCustomAttributeNames()));
        assertSame(VALUE_2, mpc.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, mpc.getCustomAttribute(KEY_2));

        mpc.setCustomAttribute(KEY_1, null);
        assertArrayEquals(new String[] { KEY_1, KEY_2 }, sort(mpc.getCustomAttributeNames()));
        assertNull(mpc.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, mpc.getCustomAttribute(KEY_2));

        mpc.removeCustomAttribute(KEY_1);
        assertArrayEquals(new String[] { KEY_2 }, mpc.getCustomAttributeNames());
        assertNull(mpc.getCustomAttribute(KEY_1));
        assertSame(VALUE_2, mpc.getCustomAttribute(KEY_2));
    }

    @Test
    public void testRemoveFromEmptySet() throws Exception {
        // Need some MutableProcessingConfiguration:
        TemplateConfiguration.Builder mpc = new TemplateConfiguration.Builder();

        mpc.removeCustomAttribute(KEY_1);
        assertEquals(0, mpc.getCustomAttributeNames().length);
        assertNull(mpc.getCustomAttribute(KEY_1));

        mpc.setCustomAttribute(KEY_1, VALUE_1);
        assertArrayEquals(new String[] { KEY_1 }, mpc.getCustomAttributeNames());
        assertSame(VALUE_1, mpc.getCustomAttribute(KEY_1));
    }

    @Test
    public void testAttrsFromFtlHeaderOnly() throws Exception {
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': [ 's', 2, true, {  'a': 'A' } ], "
                + "'" + KEY_2 + "': " + VALUE_BIGDECIMAL + " "
                + "}>",
                new Configuration(Configuration.VERSION_3_0_0));

        assertEquals(ImmutableSet.of(KEY_1, KEY_2), t.getCustomAttributes().keySet());
        assertEquals(VALUE_LIST, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_BIGDECIMAL, t.getCustomAttribute(KEY_2));

        t.setCustomAttribute(KEY_1, VALUE_1);
        assertEquals(VALUE_1, t.getCustomAttribute(KEY_1));
        assertEquals(VALUE_BIGDECIMAL, t.getCustomAttribute(KEY_2));

        t.setCustomAttribute(KEY_1, null);
        assertEquals(ImmutableSet.of(KEY_1, KEY_2), t.getCustomAttributes().keySet());
        assertNull(t.getCustomAttribute(KEY_1));
    }

    @Test
    public void testAttrsFromFtlHeaderAndFromTemplateConfiguration() throws Exception {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomAttribute(KEY_3, VALUE_3);
        tcb.setCustomAttribute(KEY_4, VALUE_4);
        Template t = new Template(null, "<#ftl attributes={"
                + "'" + KEY_1 + "': 'a', "
                + "'" + KEY_2 + "': 'b', "
                + "'" + KEY_3 + "': 'c' "
                + "}>",
                new Configuration(Configuration.VERSION_3_0_0),
                tcb.build());

        assertEquals(ImmutableSet.of(KEY_1, KEY_2, KEY_3, KEY_4), t.getCustomAttributes().keySet());
        assertEquals("a", t.getCustomAttribute(KEY_1));
        assertEquals("b", t.getCustomAttribute(KEY_2));
        assertEquals("c", t.getCustomAttribute(KEY_3)); // Has overridden TC attribute
        assertEquals(VALUE_4, t.getCustomAttribute(KEY_4)); // Inherited TC attribute

        t.setCustomAttribute(KEY_3, null);
        assertEquals(ImmutableSet.of(KEY_1, KEY_2, KEY_3, KEY_4), t.getCustomAttributes().keySet());
        assertNull("null value shouldn't cause fallback to TC attribute", t.getCustomAttribute(KEY_3));
    }


    @Test
    public void testAttrsFromTemplateConfigurationOnly() throws Exception {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomAttribute(KEY_3, VALUE_3);
        tcb.setCustomAttribute(KEY_4, VALUE_4);
        Template t = new Template(null, "",
                new Configuration(Configuration.VERSION_3_0_0),
                tcb.build());

        assertEquals(ImmutableSet.of(KEY_3, KEY_4), t.getCustomAttributes().keySet());
        assertEquals(VALUE_3, t.getCustomAttribute(KEY_3));
        assertEquals(VALUE_4, t.getCustomAttribute(KEY_4));
    }

    private Object[] sort(String[] customAttributeNames) {
        Arrays.sort(customAttributeNames);
        return customAttributeNames;
    }

}
