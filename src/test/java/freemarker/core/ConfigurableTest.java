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
