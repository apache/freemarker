package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class SettingDirectiveTest {

    @Test
    public void testGetSettingNamesSorted() throws Exception {
        String prevName = null;
        for (String name : PropertySetting.SETTING_NAMES) {
            if (prevName != null) {
                assertThat(name, greaterThan(prevName));
            }
            prevName = name;
        }
    }
    
}
