package freemarker.core;

import org.junit.Test;

public class MiscErrorMessagesTest extends TemplateErrorMessageTest {

    @Test
    public void stringIndexOutOfBounds() {
        assertErrorContains("${'foo'[10]}", "length", "3", "10", "String index out of");
    }    
    
}
