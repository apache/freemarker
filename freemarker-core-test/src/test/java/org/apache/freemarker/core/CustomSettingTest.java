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

import static org.apache.freemarker.core.ProcessingConfiguration.MISSING_VALUE_MARKER;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("boxing")
public class CustomSettingTest {
    
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";
    private static final String KEY_3 = "key3";
    private static final Integer KEY_4 = 4;

    private static final Integer VALUE_1 = 1; // Serializable
    private static final Object VALUE_2 = new Object();
    private static final Object VALUE_3 = new Object();
    private static final Object VALUE_4 = new Object();

    @Test
    public void testMutableProcessingConfiguration() throws Exception {
        testMutableProcessingConfiguration(new Configuration.Builder(Configuration.VERSION_3_0_0));

        testMutableProcessingConfiguration(new TemplateConfiguration.Builder());

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
        Environment env = new Template(null, "", cfg).createProcessingEnvironment(null, null);
        testMutableProcessingConfiguration(env);
    }

    private void testMutableProcessingConfiguration(MutableProcessingConfiguration<?> mpc) {
        assertTrue(mpc.getCustomSettingsSnapshot(true).isEmpty());
        assertTrue(mpc.getCustomSettingsSnapshot(false).isEmpty());
        testMissingCustomSettingAccess(mpc, KEY_1);

        mpc.setCustomSetting(KEY_1, VALUE_1);
        mpc.setCustomSetting(KEY_2, VALUE_2);

        assertSame(VALUE_1, mpc.getCustomSetting(KEY_1));
        assertSame(VALUE_2, mpc.getCustomSetting(KEY_2));
        testMissingCustomSettingAccess(mpc, KEY_3);
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, KEY_2, VALUE_2), mpc.getCustomSettingsSnapshot(true));
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, KEY_2, VALUE_2), mpc.getCustomSettingsSnapshot(false));

        assertSame(VALUE_2, mpc.getCustomSetting(KEY_2, "default"));
        mpc.unsetCustomSetting(KEY_2);
        assertEquals("default", mpc.getCustomSetting(KEY_2, "default"));
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1), mpc.getCustomSettingsSnapshot(true));
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1), mpc.getCustomSettingsSnapshot(false));

        mpc.unsetAllCustomSettings();
        assertTrue(mpc.getCustomSettingsSnapshot(true).isEmpty());

        testCustomSettingsSnapshotIsUnmodifiable(mpc);
        mpc.setCustomSetting(KEY_1, VALUE_1);
        testCustomSettingsSnapshotIsUnmodifiable(mpc);

        // Test no aliasing
        Map<Serializable, Object> attrMap1 = mpc.getCustomSettingsSnapshot(false);
        mpc.setCustomSetting(KEY_2, VALUE_2);
        assertNull(attrMap1.get(KEY_2));

        mpc.unsetAllCustomSettings();
        mpc.setCustomSetting(KEY_1, VALUE_1);
        mpc.setCustomSettings(ImmutableMap.of(KEY_2, VALUE_2, KEY_3, VALUE_3));
        assertEquals(
                ImmutableMap.of(KEY_1, VALUE_1, KEY_2, VALUE_2, KEY_3, VALUE_3),
                mpc.getCustomSettingsSnapshot(false));

        try {
            mpc.setCustomSetting(KEY_1, MISSING_VALUE_MARKER);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("MISSING_VALUE_MARKER"));
        }
        try {
            mpc.setCustomSettings(ImmutableMap.of(KEY_1, MISSING_VALUE_MARKER));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("MISSING_VALUE_MARKER"));
        }
    }

    private void testMissingCustomSettingAccess(ProcessingConfiguration pc) {
        testMissingCustomSettingAccess(pc, "noSuchKey");
    }

    private void testCustomSettingsSnapshotIsUnmodifiable(ProcessingConfiguration pc) {
        for (boolean includeInherited : new boolean[] { false,  true }) {
            Map<Serializable, Object> map = pc.getCustomSettingsSnapshot(includeInherited);
            try {
                map.put("aNewKey", 123);
                fail();
            } catch (UnsupportedOperationException e) {
                // Expected
            }
        }
    }

    private void testMissingCustomSettingAccess(ProcessingConfiguration pc, Serializable key) {
        try {
            pc.getCustomSetting(key);
            fail();
        } catch (CustomSettingNotSetException e) {
            assertSame(key, e.getKey());
        }

        assertNull(pc.getCustomSetting(key, null));
        assertEquals("default", pc.getCustomSetting(key, "default"));
        assertSame(MISSING_VALUE_MARKER,
                pc.getCustomSetting(key, MISSING_VALUE_MARKER));
    }

    @Test
    public void testTemplateAttrsFromFtlHeaderOnly() throws Exception {
        Template t = new Template(null, "<#ftl customSettings={"
                + "'" + KEY_1 + "': [ 's', 2, true, {  'a': 'A' } ], "
                + "'" + KEY_2 + "': 22 "
                + "}>",
                new Configuration.Builder(Configuration.VERSION_3_0_0).build());

        assertEquals(ImmutableSet.of(KEY_1, KEY_2), t.getCustomSettingsSnapshot(true).keySet());
        assertEquals(
                ImmutableList.<Object>of("s", BigDecimal.valueOf(2), Boolean.TRUE, ImmutableMap.of("a", "A")),
                t.getCustomSetting(KEY_1));
        assertEquals(BigDecimal.valueOf(22), t.getCustomSetting(KEY_2));

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);
    }

    @Test
    public void testTemplateAttrsFromFtlHeaderAndFromTemplateConfiguration() throws Exception {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomSetting(KEY_3, VALUE_3);
        tcb.setCustomSetting(KEY_4, VALUE_4);
        Template t = new Template(null, "<#ftl customSettings={"
                + "'" + KEY_1 + "': 'a', "
                + "'" + KEY_2 + "': 'b', "
                + "'" + KEY_3 + "': 'c' "
                + "}>",
                new Configuration.Builder(Configuration.VERSION_3_0_0).build(),
                tcb.build());

        assertEquals(ImmutableMap.of(KEY_1, "a", KEY_2, "b", KEY_3, "c", KEY_4, VALUE_4),
                t.getCustomSettingsSnapshot(true));
        assertEquals("a", t.getCustomSetting(KEY_1));
        assertEquals("b", t.getCustomSetting(KEY_2));
        assertEquals("c", t.getCustomSetting(KEY_3)); // Has overridden TC attribute
        assertEquals(VALUE_4, t.getCustomSetting(KEY_4)); // Inherited TC attribute

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);
    }

    @Test
    public void testTemplateAttrsFromTemplateConfigurationOnly() throws Exception {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomSetting(KEY_3, VALUE_3);
        tcb.setCustomSetting(KEY_4, VALUE_4);
        Template t = new Template(null, "",
                new Configuration.Builder(Configuration.VERSION_3_0_0).build(),
                tcb.build());

        assertEquals(ImmutableSet.of(KEY_3, KEY_4), t.getCustomSettingsSnapshot(true).keySet());
        assertEquals(VALUE_3, t.getCustomSetting(KEY_3));
        assertEquals(VALUE_4, t.getCustomSetting(KEY_4));

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);
    }

    @Test
    public void testTemplateAttrsFromConfigurationOnly() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .customSetting(KEY_1, VALUE_1)
                .build();
        Template t = new Template(null, "", cfg);

        assertEquals(VALUE_1, t.getCustomSetting(KEY_1));
        assertEquals("default", t.getCustomSetting(KEY_2, "default"));

        assertEquals(ImmutableMap.of(KEY_1, VALUE_1), t.getCustomSettingsSnapshot(true));
        assertTrue(t.getCustomSettingsSnapshot(false).isEmpty());

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);
    }

    @Test
    public void testTemplateAttrsFromTemplateAndConfiguration() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .customSetting(KEY_1, VALUE_1)
                .build();
        Template t = new Template(null, "<#ftl customSettings={'k2':'v2'}>", cfg);

        assertEquals(VALUE_1, t.getCustomSetting(KEY_1));
        assertEquals("v2", t.getCustomSetting("k2"));

        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, "k2", "v2"), t.getCustomSettingsSnapshot(true));
        assertEquals(ImmutableMap.of("k2", "v2"), t.getCustomSettingsSnapshot(false));

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);
    }

    @Test
    public void testAllLayers() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .customSetting(KEY_1, VALUE_1)
                .build();
        Template t = new Template(null, "<#ftl customSettings={'k2':'v2'}>", cfg,
                new TemplateConfiguration.Builder().customSetting(KEY_3, VALUE_3).build());

        assertEquals(VALUE_1, t.getCustomSetting(KEY_1));
        assertEquals("v2", t.getCustomSetting("k2"));
        assertEquals(VALUE_3, t.getCustomSetting(KEY_3));

        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, "k2", "v2", KEY_3, VALUE_3),
                t.getCustomSettingsSnapshot(true));
        assertEquals(ImmutableMap.of("k2", "v2", KEY_3, VALUE_3),
                t.getCustomSettingsSnapshot(false));

        testMissingCustomSettingAccess(t);
        testCustomSettingsSnapshotIsUnmodifiable(t);

        Environment env = t.createProcessingEnvironment(null, null);

        assertEquals(VALUE_1, env.getCustomSetting(KEY_1));
        assertEquals("v2", env.getCustomSetting("k2"));
        assertEquals(VALUE_3, env.getCustomSetting(KEY_3));
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, "k2", "v2", KEY_3, VALUE_3),
                env.getCustomSettingsSnapshot(true));
        assertEquals(Collections.emptyMap(),
                env.getCustomSettingsSnapshot(false));

        env.setCustomSetting(KEY_4, VALUE_4);
        assertEquals(VALUE_1, env.getCustomSetting(KEY_1));
        assertEquals("v2", env.getCustomSetting("k2"));
        assertEquals(VALUE_3, env.getCustomSetting(KEY_3));
        assertEquals(VALUE_4, env.getCustomSetting(KEY_4));
        assertEquals(ImmutableMap.of(KEY_1, VALUE_1, "k2", "v2", KEY_3, VALUE_3, KEY_4, VALUE_4),
                env.getCustomSettingsSnapshot(true));
        assertEquals(ImmutableMap.of(KEY_4, VALUE_4),
                env.getCustomSettingsSnapshot(false));

        testMissingCustomSettingAccess(env);
        testCustomSettingsSnapshotIsUnmodifiable(env);
    }

}
