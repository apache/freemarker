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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.freemarker.core.userpkg.BaseNTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisDivTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.HexTemplateNumberFormatFactory;
import org.apache.freemarker.core.util._CollectionUtil;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.junit.Test;

public class MutableProcessingConfigurationTest {

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        MutableProcessingConfiguration mpc = createMutableProcessingConfiguration();
        for (boolean camelCase : new boolean[] { false, true }) {
            Collection<String> names = mpc.getSettingNames(camelCase);
            String prevName = null;
            for (String name : names) {
                if (prevName != null) {
                    assertThat(name, greaterThan(prevName));
                }
                prevName = name;
            }
        }
    }

    @Test
    public void testStaticFieldKeysCoverAllGetSettingNames() throws Exception {
        MutableProcessingConfiguration mpc = createMutableProcessingConfiguration();
        Collection<String> names = mpc.getSettingNames(false);
        for (String name : names) {
                assertTrue("No field was found for " + name, keyFieldExists(name));
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllStaticKeyFields() throws Exception {
        MutableProcessingConfiguration mpc = createMutableProcessingConfiguration();
        Collection<String> names = mpc.getSettingNames(false);
        
        for (Field f : MutableProcessingConfiguration.class.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final Object name = f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
            }
        }
    }

    @Test
    public void testKeyStaticFieldsHasAllVariationsAndCorrectFormat() throws IllegalArgumentException, IllegalAccessException {
        MutableProcessingConfigurationTest.testKeyStaticFieldsHasAllVariationsAndCorrectFormat(MutableProcessingConfiguration.class);
    }
    
    @Test
    public void testGetSettingNamesNameConventionsContainTheSame() throws Exception {
        MutableProcessingConfiguration mpc = createMutableProcessingConfiguration();
        MutableProcessingConfigurationTest.testGetSettingNamesNameConventionsContainTheSame(
                new ArrayList<>(mpc.getSettingNames(false)),
                new ArrayList<>(mpc.getSettingNames(true)));
    }

    public static void testKeyStaticFieldsHasAllVariationsAndCorrectFormat(
            Class<? extends MutableProcessingConfiguration> confClass) throws IllegalArgumentException, IllegalAccessException {
        // For all _KEY fields there must be a _KEY_CAMEL_CASE and a _KEY_SNAKE_CASE field.
        // Their content must not contradict the expected naming convention.
        // They _KEY filed value must be deducable from the field name
        // The _KEY value must be the same as _KEY_SNAKE_CASE field.
        // The _KEY_CAMEL_CASE converted to snake case must give the value of the _KEY_SNAKE_CASE.
        for (Field field : confClass.getFields()) {
            String fieldName = field.getName();
            if (fieldName.endsWith("_KEY")) {
                String keyFieldValue = (String) field.get(null);
                assertNotEquals(NamingConvention.CAMEL_CASE,
                        _StringUtil.getIdentifierNamingConvention(keyFieldValue));
                assertEquals(fieldName.substring(0, fieldName.length() - 4).toLowerCase(), keyFieldValue);
                
                try {
                    String keySCFieldValue = (String) confClass.getField(fieldName + "_SNAKE_CASE").get(null);
                    assertEquals(keyFieldValue, keySCFieldValue);
                } catch (NoSuchFieldException e) {
                    fail("Missing ..._SNAKE_CASE field for " + fieldName);
                }
                
                try {
                    String keyCCFieldValue = (String) confClass.getField(fieldName + "_CAMEL_CASE").get(null);
                    assertNotEquals(NamingConvention.LEGACY,
                            _StringUtil.getIdentifierNamingConvention(keyCCFieldValue));
                    assertEquals(keyFieldValue, _StringUtil.camelCaseToUnderscored(keyCCFieldValue));
                } catch (NoSuchFieldException e) {
                    fail("Missing ..._CAMEL_CASE field for " + fieldName);
                }
            }
        }
        
        // For each _KEY_SNAKE_CASE field there must be a _KEY field.
        for (Field field : confClass.getFields()) {
            String fieldName = field.getName();
            if (fieldName.endsWith("_KEY_SNAKE_CASE")) {
                try {
                    confClass.getField(fieldName.substring(0, fieldName.length() - 11)).get(null);
                } catch (NoSuchFieldException e) {
                    fail("Missing ..._KEY field for " + fieldName);
                }
            }
        }
        
        // For each _KEY_CAMEL_CASE field there must be a _KEY field.
        for (Field field : confClass.getFields()) {
            String fieldName = field.getName();
            if (fieldName.endsWith("_KEY_CAMEL_CASE")) {
                try {
                    confClass.getField(fieldName.substring(0, fieldName.length() - 11)).get(null);
                } catch (NoSuchFieldException e) {
                    fail("Missing ..._KEY field for " + fieldName);
                }
            }
        }
    }
    
    public static void testGetSettingNamesNameConventionsContainTheSame(List<String> namesSCList, List<String> namesCCList) {
        Set<String> namesSC = new HashSet<>(namesSCList);
        assertEquals(namesSCList.size(), namesSC.size());
        
        Set<String> namesCC = new HashSet<>(namesCCList);
        assertEquals(namesCCList.size(), namesCC.size());

        assertEquals(namesSC.size(), namesCC.size());
        
        for (String nameCC : namesCC) {
            final String nameSC = _StringUtil.camelCaseToUnderscored(nameCC);
            if (!namesSC.contains(nameSC)) {
                fail("\"" + nameCC + "\" misses corresponding snake case name, \"" + nameSC + "\".");
            }
        }
    }

    private MutableProcessingConfiguration createMutableProcessingConfiguration() throws IOException {
        return new TemplateConfiguration.Builder();
    }

    private boolean keyFieldExists(String name) throws Exception {
        try {
            MutableProcessingConfiguration.class.getField(name.toUpperCase() + "_KEY");
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }

    @Test
    public void testCollectionSettingMutability() throws IOException {
        MutableProcessingConfiguration<?> mpc = new Configuration.Builder(Configuration.VERSION_3_0_0);

        {
            assertTrue(_CollectionUtil.isListKnownToBeUnmodifiable(mpc.getAutoIncludes()));
            List<String> mutableValue = new ArrayList<>();
            mutableValue.add("x");
            mpc.setAutoIncludes(mutableValue);
            List<String> immutableValue = mpc.getAutoIncludes();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtil.isListKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.add("y");
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(mpc.getAutoImports()));
            Map<String, String> mutableValue = new HashMap<>();
            mutableValue.put("x", "x.ftl");
            mpc.setAutoImports(mutableValue);
            Map<String, String> immutableValue = mpc.getAutoImports();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", "y.ftl");
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(mpc.getCustomDateFormats()));
            Map<String, TemplateDateFormatFactory> mutableValue = new HashMap<>();
            mutableValue.put("x", EpochMillisTemplateDateFormatFactory.INSTANCE);
            mpc.setCustomDateFormats(mutableValue);
            Map<String, TemplateDateFormatFactory> immutableValue = mpc.getCustomDateFormats();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", EpochMillisDivTemplateDateFormatFactory.INSTANCE);
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }

        {
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(mpc.getCustomNumberFormats()));
            Map<String, TemplateNumberFormatFactory> mutableValue = new HashMap<>();
            mutableValue.put("x", BaseNTemplateNumberFormatFactory.INSTANCE);
            mpc.setCustomNumberFormats(mutableValue);
            Map<String, TemplateNumberFormatFactory> immutableValue = mpc.getCustomNumberFormats();
            assertNotSame(mutableValue, immutableValue); // Must be a copy
            assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(immutableValue));
            assertEquals(mutableValue, immutableValue);
            mutableValue.put("y", HexTemplateNumberFormatFactory.INSTANCE);
            assertNotEquals(mutableValue, immutableValue); // No aliasing
        }
    }

}
