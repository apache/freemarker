/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.util._DateUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.junit.Test;

public class MutableParsingAndProcessingConfigurationTest {

    @Test
    public void testSetAutoEscaping() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

        cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_SUPPORTED);
        assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, cfgB.getAutoEscapingPolicy());

        cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_DEFAULT);
        assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

        cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.DISABLE);
        assertEquals(AutoEscapingPolicy.DISABLE, cfgB.getAutoEscapingPolicy());

        cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY, "enableIfSupported");
        assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, cfgB.getAutoEscapingPolicy());

        cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY, "enableIfDefault");
        assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

        cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY, "disable");
        assertEquals(AutoEscapingPolicy.DISABLE, cfgB.getAutoEscapingPolicy());
    }

    @Test
    public void testSetOutputFormat() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(UndefinedOutputFormat.INSTANCE, cfgB.getOutputFormat());
        assertFalse(cfgB.isOutputFormatSet());

        try {
            cfgB.setOutputFormat(null);
            fail();
        } catch (_NullArgumentException e) {
            // Expected
        }

        assertFalse(cfgB.isOutputFormatSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY, XMLOutputFormat.class.getSimpleName());
        assertEquals(XMLOutputFormat.INSTANCE, cfgB.getOutputFormat());

        cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY, HTMLOutputFormat.class.getSimpleName());
        assertEquals(HTMLOutputFormat.INSTANCE, cfgB.getOutputFormat());

        cfgB.unsetOutputFormat();
        assertEquals(UndefinedOutputFormat.INSTANCE, cfgB.getOutputFormat());
        assertFalse(cfgB.isOutputFormatSet());

        cfgB.setOutputFormat(UndefinedOutputFormat.INSTANCE);
        assertTrue(cfgB.isOutputFormatSet());
        cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY, "default");
        assertFalse(cfgB.isOutputFormatSet());

        try {
            cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY, "null");
        } catch (InvalidSettingValueException e) {
            assertThat(e.getCause().getMessage(), containsString(UndefinedOutputFormat.class.getSimpleName()));
        }
    }

    @Test
    public void testSetRecognizeStandardFileExtensions() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.setRecognizeStandardFileExtensions(false);
        assertFalse(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.unsetRecognizeStandardFileExtensions();
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.setRecognizeStandardFileExtensions(true);
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY, "false");
        assertFalse(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY, "default");
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());
    }

    @Test
    public void testSetTabSize() throws Exception {
        String ftl = "${\t}";

        try {
            new Template(null, ftl,
                    new Configuration.Builder(Configuration.VERSION_3_0_0).build());
            fail();
        } catch (ParseException e) {
            assertEquals(9, e.getColumnNumber());
        }

        try {
            new Template(null, ftl,
                    new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(1).build());
            fail();
        } catch (ParseException e) {
            assertEquals(4, e.getColumnNumber());
        }

        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(257);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testTabSizeSetting() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertEquals(8, cfgB.getTabSize());
        cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY, "4");
        assertEquals(4, cfgB.getTabSize());
        cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY, "1");
        assertEquals(1, cfgB.getTabSize());

        try {
            cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY, "x");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause(), instanceOf(NumberFormatException.class));
        }
    }

    @Test
    public void testLazyImportsSetSetting() throws ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertFalse(cfgB.getLazyImports());
        assertFalse(cfgB.isLazyImportsSet());
        cfgB.setSetting("lazyImports", "true");
        assertTrue(cfgB.getLazyImports());
        cfgB.setSetting("lazyImports", "false");
        assertFalse(cfgB.getLazyImports());
        assertTrue(cfgB.isLazyImportsSet());
    }

    @Test
    public void testLazyAutoImportsSetSetting() throws ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertNull(cfgB.getLazyAutoImports());
        assertFalse(cfgB.isLazyAutoImportsSet());
        cfgB.setSetting("lazyAutoImports", "true");
        assertEquals(Boolean.TRUE, cfgB.getLazyAutoImports());
        assertTrue(cfgB.isLazyAutoImportsSet());
        cfgB.setSetting("lazyAutoImports", "false");
        assertEquals(Boolean.FALSE, cfgB.getLazyAutoImports());
        cfgB.setSetting("lazyAutoImports", "null");
        assertNull(cfgB.getLazyAutoImports());
        assertTrue(cfgB.isLazyAutoImportsSet());
        cfgB.unsetLazyAutoImports();
        assertNull(cfgB.getLazyAutoImports());
        assertFalse(cfgB.isLazyAutoImportsSet());
    }

    @Test
    public void testLocaleSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertFalse(cfgB.isLocaleSet());

        Locale nonDefault = Locale.getDefault().equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
        cfgB.setLocale(nonDefault);
        assertTrue(cfgB.isLocaleSet());
        assertEquals(nonDefault, cfgB.getLocale());

        cfgB.unsetLocale();
        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertFalse(cfgB.isLocaleSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.LOCALE_KEY, "JVM default");
        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertTrue(cfgB.isLocaleSet());
    }

    @Test
    public void testSourceEncodingSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(StandardCharsets.UTF_8, cfgB.getSourceEncoding());
        assertFalse(cfgB.isSourceEncodingSet());

        cfgB.setSourceEncoding(StandardCharsets.ISO_8859_1);
        assertTrue(cfgB.isSourceEncodingSet());
        assertEquals(StandardCharsets.ISO_8859_1, cfgB.getSourceEncoding());

        cfgB.unsetSourceEncoding();
        assertEquals(StandardCharsets.UTF_8, cfgB.getSourceEncoding());
        assertFalse(cfgB.isSourceEncodingSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY, "JVM default");
        assertEquals(Charset.defaultCharset(), cfgB.getSourceEncoding());
        assertTrue(cfgB.isSourceEncodingSet());
    }

    @Test
    public void testTimeZoneSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertFalse(cfgB.isTimeZoneSet());

        TimeZone nonDefault = TimeZone.getDefault().equals(_DateUtil.UTC) ? TimeZone.getTimeZone("PST") : _DateUtil.UTC;
        cfgB.setTimeZone(nonDefault);
        assertTrue(cfgB.isTimeZoneSet());
        assertEquals(nonDefault, cfgB.getTimeZone());

        cfgB.unsetTimeZone();
        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertFalse(cfgB.isTimeZoneSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.TIME_ZONE_KEY, "JVM default");
        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertTrue(cfgB.isTimeZoneSet());
    }

    // ------------

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        for (boolean camelCase : new boolean[] { false, true }) {
            List<String> names = new ArrayList<>(MutableParsingAndProcessingConfiguration.getSettingNames());
            List<String> inheritedNames = new ArrayList<>(MutableProcessingConfiguration.getSettingNames());
            assertStartsWith(names, inheritedNames);

            String prevName = null;
            for (int i = inheritedNames.size(); i < names.size(); i++) {
                String name = names.get(i);
                if (prevName != null) {
                    assertThat(name, greaterThan(prevName));
                }
                prevName = name;
            }
        }
    }

    @Test
    public void testAllSettingsAreCoveredByMutableSettingsObject() throws Exception {
        ConfigurationTest.testAllSettingsAreCoveredByMutableSettingsObject(
                ParsingAndProcessingConfiguration.class,
                MutableParsingAndProcessingConfiguration.class);
    }

    @Test
    public void testGetSettingNamesCorrespondToStaticKeyFields() throws Exception {
        ConfigurationTest.testGetSettingNamesCorrespondToStaticKeyFields(
                MutableParsingAndProcessingConfiguration.getSettingNames(),
                MutableParsingAndProcessingConfiguration.class);
    }

    @SuppressWarnings("boxing")
    private void assertStartsWith(List<String> list, List<String> headList) {
        int index = 0;
        for (String name : headList) {
            assertThat(index, lessThan(list.size()));
            assertEquals(name, list.get(index));
            index++;
        }
    }

}
