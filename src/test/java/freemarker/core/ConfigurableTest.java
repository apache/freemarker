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

package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class ConfigurableTest {

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        Configurable cfgable = createConfigurable();
        for (boolean camelCase : new boolean[] { false, true }) {
            Collection<String> names = cfgable.getSettingNames(camelCase);
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
        Configurable cfgable = createConfigurable();
        Collection<String> names = cfgable.getSettingNames(false);
        for (String name : names) {
                assertTrue("No field was found for " + name, keyFieldExists(name));
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllStaticKeyFields() throws Exception {
        Configurable cfgable = createConfigurable();
        Collection<String> names = cfgable.getSettingNames(false);
        
        for (Field f : Configurable.class.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final Object name = f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
            }
        }
    }

    @Test
    public void testKeyStaticFieldsHasAllVariationsAndCorrectFormat() throws IllegalArgumentException, IllegalAccessException {
        ConfigurableTest.testKeyStaticFieldsHasAllVariationsAndCorrectFormat(Configurable.class);
    }
    
    @Test
    public void testGetSettingNamesNameConventionsContainTheSame() throws Exception {
        Configurable cfgable = createConfigurable();
        ConfigurableTest.testGetSettingNamesNameConventionsContainTheSame(
                new ArrayList<String>(cfgable.getSettingNames(false)),
                new ArrayList<String>(cfgable.getSettingNames(true)));
    }

    public static void testKeyStaticFieldsHasAllVariationsAndCorrectFormat(
            Class<? extends Configurable> confClass) throws IllegalArgumentException, IllegalAccessException {
        // For all _KEY fields there must be a _KEY_CAMEL_CASE and a _KEY_SNAKE_CASE field.
        // Their content must not contradict the expected naming convention.
        // They _KEY filed value must be deducable from the field name
        // The _KEY value must be the same as _KEY_SNAKE_CASE field.
        // The _KEY_CAMEL_CASE converted to snake case must give the value of the _KEY_SNAKE_CASE.
        for (Field field : confClass.getFields()) {
            String fieldName = field.getName();
            if (fieldName.endsWith("_KEY")) {
                String keyFieldValue = (String) field.get(null);
                assertNotEquals(Configuration.CAMEL_CASE_NAMING_CONVENTION,
                        _CoreStringUtils.getIdentifierNamingConvention(keyFieldValue));
                assertEquals(fieldName.substring(0, fieldName.length() - 4).toLowerCase(), keyFieldValue);
                
                try {
                    String keySCFieldValue = (String) confClass.getField(fieldName + "_SNAKE_CASE").get(null);
                    assertEquals(keyFieldValue, keySCFieldValue);
                } catch (NoSuchFieldException e) {
                    fail("Missing ..._SNAKE_CASE field for " + fieldName);
                }
                
                try {
                    String keyCCFieldValue = (String) confClass.getField(fieldName + "_CAMEL_CASE").get(null);
                    assertNotEquals(Configuration.LEGACY_NAMING_CONVENTION,
                            _CoreStringUtils.getIdentifierNamingConvention(keyCCFieldValue));
                    assertEquals(keyFieldValue, _CoreStringUtils.camelCaseToUnderscored(keyCCFieldValue));
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
        Set<String> namesSC = new HashSet<String>(namesSCList);
        assertEquals(namesSCList.size(), namesSC.size());
        
        Set<String> namesCC = new HashSet<String>(namesCCList);
        assertEquals(namesCCList.size(), namesCC.size());

        assertEquals(namesSC.size(), namesCC.size());
        
        for (String nameCC : namesCC) {
            final String nameSC = _CoreStringUtils.camelCaseToUnderscored(nameCC);
            if (!namesSC.contains(nameSC)) {
                fail("\"" + nameCC + "\" misses corresponding snake case name, \"" + nameSC + "\".");
            }
        }
    }
    
    private Configurable createConfigurable() throws IOException {
        return new Template(null, "", new Configuration(Configuration.VERSION_2_3_22));
    }

    private boolean keyFieldExists(String name) throws Exception {
        try {
            Configurable.class.getField(name.toUpperCase() + "_KEY");
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }

}
