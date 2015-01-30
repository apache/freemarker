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
import java.util.Collection;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class ConfigurableTest {

    @Test
    public void testGetSettingNamesSortedAndValid() throws Exception {
        Configurable cfgable = createConfigurable();
        Collection<String> names = cfgable.getSettingNames();
        
        String prevName = null;
        for (String name : names) {
            if (prevName != null) {
                assertThat(name, greaterThan(prevName));
                assertTrue("No field was found for " + name, keyFieldExists(name));
            }
            prevName = name;
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllSettingNames() throws Exception {
        Configurable cfgable = createConfigurable();
        Collection<String> names = cfgable.getSettingNames();
        
        for (Field f : Configurable.class.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final Object name = f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
            }
        }
    }

    private Configurable createConfigurable() throws IOException {
        return new Template(null, "", new Configuration(Configuration.VERSION_2_3_22));
    }

    private boolean keyFieldExists(String name) throws Exception {
        Field field;
        try {
            field = Configurable.class.getField(name.toUpperCase() + "_KEY");
        } catch (NoSuchFieldException e) {
            return false;
        }
        assertEquals(name, field.get(null));
        return true;
    }

}
