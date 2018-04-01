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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.freemarker.core.userpkg.BaseNTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisDivTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.HexTemplateNumberFormatFactory;
import org.apache.freemarker.core.util._CollectionUtils;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MutableProcessingConfigurationTest {

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        Collection<String> names = MutableProcessingConfiguration.getSettingNames();
        String prevName = null;
        for (String name : names) {
            if (prevName != null) {
                assertThat(name, greaterThan(prevName));
            }
            prevName = name;
        }
    }

    @Test
    public void testAllSettingsAreCoveredByMutableSettingsObject() throws Exception {
        ConfigurationTest.testAllSettingsAreCoveredByMutableSettingsObject(
                ProcessingConfiguration.class,
                MutableProcessingConfiguration.class);
    }

    @Test
    public void testGetSettingNamesCorrespondToStaticKeyFields() throws Exception {
        ConfigurationTest.testGetSettingNamesCorrespondToStaticKeyFields(
                MutableProcessingConfiguration.getSettingNames(),
                MutableProcessingConfiguration.class);
    }

    @Test
    public void testCollectionSettingMutability() throws IOException {
        MutableProcessingConfiguration<?> mpc = new Configuration.Builder(Configuration.VERSION_3_0_0);

        {
            assertTrue(_CollectionUtils.isListKnownToBeUnmodifiable(mpc.getAutoIncludes()));
            List<String> mutableValue = new ArrayList<>();
            mutableValue.add("x");
            mpc.setAutoIncludes(mutableValue);
            List<String> immutableValue = mpc.getAutoIncludes();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtils.isListKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.add("y");
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(mpc.getAutoImports()));
            Map<String, String> mutableValue = new HashMap<>();
            mutableValue.put("x", "x.ftl");
            mpc.setAutoImports(mutableValue);
            Map<String, String> immutableValue = mpc.getAutoImports();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", "y.ftl");
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(mpc.getCustomDateFormats()));
            Map<String, TemplateDateFormatFactory> mutableValue = new HashMap<>();
            mutableValue.put("x", EpochMillisTemplateDateFormatFactory.INSTANCE);
            mpc.setCustomDateFormats(mutableValue);
            Map<String, TemplateDateFormatFactory> immutableValue = mpc.getCustomDateFormats();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", EpochMillisDivTemplateDateFormatFactory.INSTANCE);
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(mpc.getCustomNumberFormats()));
            Map<String, TemplateNumberFormatFactory> mutableValue = new HashMap<>();
            mutableValue.put("x", BaseNTemplateNumberFormatFactory.INSTANCE);
            mpc.setCustomNumberFormats(mutableValue);
            Map<String, TemplateNumberFormatFactory> immutableValue = mpc.getCustomNumberFormats();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", HexTemplateNumberFormatFactory.INSTANCE);
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }
    }

    @Test
    public void testSetTimeZone() throws ConfigurationException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);

            Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
            assertEquals(sysDefTZ, cfgB.getTimeZone());
            cfgB.setSetting(MutableProcessingConfiguration.TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfgB.getTimeZone());

            TimeZone newSysDefTZ = TimeZone.getTimeZone("GMT+09");
            TimeZone.setDefault(newSysDefTZ);
            assertEquals(sysDefTZ, cfgB.getTimeZone());
            cfgB.setSetting(MutableProcessingConfiguration.TIME_ZONE_KEY, "JVM default");
            assertEquals(newSysDefTZ, cfgB.getTimeZone());
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }

    @Test
    public void testSetSQLDateAndTimeTimeZone() throws ConfigurationException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);

            Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
            assertEquals(sysDefTZ, cfgB.getSQLDateAndTimeTimeZone());

            cfgB.setSQLDateAndTimeTimeZone(null);
            assertNull(cfgB.getSQLDateAndTimeTimeZone());

            cfgB.setSetting(MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfgB.getSQLDateAndTimeTimeZone());

            cfgB.setSetting(MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "null");
            assertNull(cfgB.getSQLDateAndTimeTimeZone());

            cfgB.unsetSQLDateAndTimeTimeZone();
            assertEquals(sysDefTZ, cfgB.getSQLDateAndTimeTimeZone());
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }

    @Test
    public void testTimeZoneLayers() throws Exception {
        TimeZone localTZ = TimeZone.getTimeZone("Europe/Brussels");

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .sqlDateAndTimeTimeZone(null)
                    .build();
            Template t = new Template(null, "", cfg);
            Environment env1 = t.createProcessingEnvironment(null, new StringWriter());
            Environment env2 = t.createProcessingEnvironment(null, new StringWriter());

            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(TimeZone.getDefault(), env1.getTimeZone());
            assertNull(env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(TimeZone.getDefault(), env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());

            env1.setSQLDateAndTimeTimeZone(_DateUtils.UTC);
            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(TimeZone.getDefault(), env1.getTimeZone());
            assertEquals(_DateUtils.UTC, env1.getSQLDateAndTimeTimeZone());

            env1.setTimeZone(localTZ);
            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(localTZ, env1.getTimeZone());
            assertEquals(_DateUtils.UTC, env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(TimeZone.getDefault(), env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());
        }

        {
            TimeZone otherTZ1 = TimeZone.getTimeZone("GMT+05");
            TimeZone otherTZ2 = TimeZone.getTimeZone("GMT+06");
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .timeZone(otherTZ1)
                    .sqlDateAndTimeTimeZone(otherTZ2)
                    .build();

            Template t = new Template(null, "", cfg);
            Environment env1 = t.createProcessingEnvironment(null, new StringWriter());
            Environment env2 = t.createProcessingEnvironment(null, new StringWriter());

            env1.setTimeZone(localTZ);
            env1.setSQLDateAndTimeTimeZone(_DateUtils.UTC);

            // cfg:
            assertEquals(otherTZ1, cfg.getTimeZone());
            assertEquals(otherTZ2, cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(localTZ, env1.getTimeZone());
            assertEquals(_DateUtils.UTC, env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(otherTZ1, env2.getTimeZone());
            assertEquals(otherTZ2, env2.getSQLDateAndTimeTimeZone());

            try {
                setTimeZoneToNull(env2);
                fail();
            } catch (IllegalArgumentException e) {
                // expected
            }
            env2.setSQLDateAndTimeTimeZone(null);
            assertEquals(otherTZ1, env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());
        }
    }

    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="Expected to fail")
    private void setTimeZoneToNull(Environment env2) {
        env2.setTimeZone(null);
    }

    @Test
    public void testApiBuiltinEnabled() throws Exception {
        try {
            new Template(
                    null, "${1?api}",
                    new Configuration.Builder(Configuration.VERSION_3_0_0).build())
                    .process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString(MutableProcessingConfiguration.API_BUILTIN_ENABLED_KEY));
        }

        new Template(
                null, "${m?api.hashCode()}",
                new Configuration.Builder(Configuration.VERSION_3_0_0).apiBuiltinEnabled(true).build())
                .process(Collections.singletonMap("m", new HashMap<>()), _NullWriter.INSTANCE);
    }

    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS ", justification = "Testing wrong args")
    public void testSetCustomNumberFormat() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        try {
            cfgB.setCustomNumberFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "a_b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "a b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }

        try {
            cfgB.setCustomNumberFormats(ImmutableMap.<String, TemplateNumberFormatFactory>of(
                    "a", HexTemplateNumberFormatFactory.INSTANCE,
                    "@wrong", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY,
                "{ 'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("base", BaseNTemplateNumberFormatFactory.INSTANCE),
                cfgB.getCustomNumberFormats());

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY,
                "{ "
                        + "'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "(), "
                        + "'hex': " + HexTemplateNumberFormatFactory.class.getName() + "()"
                        + " }");
        assertEquals(
                ImmutableMap.of(
                        "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                        "hex", HexTemplateNumberFormatFactory.INSTANCE),
                cfgB.getCustomNumberFormats());

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfgB.getCustomNumberFormats());

        try {
            cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY,
                    "{ 'x': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(EpochMillisTemplateDateFormatFactory.class.getName()),
                    containsString(TemplateNumberFormatFactory.class.getName())));
        }
    }

    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="We test failures")
    @Test
    public void testSetCustomDateFormat() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        try {
            cfgB.setCustomDateFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }

        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "a_b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "a b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }

        try {
            cfgB.setCustomDateFormats(ImmutableMap.<String, TemplateDateFormatFactory>of(
                    "a", EpochMillisTemplateDateFormatFactory.INSTANCE,
                    "@wrong", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY,
                "{ 'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("epoch", EpochMillisTemplateDateFormatFactory.INSTANCE),
                cfgB.getCustomDateFormats());

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY,
                "{ "
                        + "'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "(), "
                        + "'epochDiv': " + EpochMillisDivTemplateDateFormatFactory.class.getName() + "()"
                        + " }");
        assertEquals(
                ImmutableMap.of(
                        "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                        "epochDiv", EpochMillisDivTemplateDateFormatFactory.INSTANCE),
                cfgB.getCustomDateFormats());

        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfgB.getCustomDateFormats());

        try {
            cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY,
                    "{ 'x': " + HexTemplateNumberFormatFactory.class.getName() + "() }");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(HexTemplateNumberFormatFactory.class.getName()),
                    containsString(TemplateDateFormatFactory.class.getName())));
        }
    }

}
