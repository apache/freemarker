package freemarker.manual;

import org.junit.Test;

public class AutoEscapingExample extends ExamplesTest {

    @Test
    public void testInfoBox() throws Exception {
        assertOutputForNamed("AutoEscapingExample-infoBox.ftlh");
    }

    @Test
    public void testCapture() throws Exception {
        assertOutputForNamed("AutoEscapingExample-capture.ftlh");
    }

    @Test
    public void testMarkup() throws Exception {
        assertOutputForNamed("AutoEscapingExample-markup.ftlh");
    }
    
}
