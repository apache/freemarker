package freemarker.core;

import org.junit.Test;

import freemarker.test.TemplateTest;

public class MiscErrorMessagesTest extends TemplateTest {

    @Test
    public void stringIndexOutOfBounds() {
        assertErrorContains("${'foo'[10]}", "length", "3", "10", "String index out of");
    }    
    
}
