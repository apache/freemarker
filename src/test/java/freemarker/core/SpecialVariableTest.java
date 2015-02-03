package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class SpecialVariableTest {

    @Test
    public void testGetSettingNamesSorted() throws Exception {
        String prevName = null;
        for (String name : BuiltinVariable.SPEC_VAR_NAMES) {
            if (prevName != null) {
                assertThat(name, greaterThan(prevName));
            }
            prevName = name;
        }
    }

}
