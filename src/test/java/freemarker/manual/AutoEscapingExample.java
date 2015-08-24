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

    @Test
    public void testConvert() throws Exception {
        assertOutputForNamed("AutoEscapingExample-convert.ftlh");
    }

    @Test
    public void testConvert2() throws Exception {
        assertOutputForNamed("AutoEscapingExample-convert2.ftl");
    }

    @Test
    public void testStringLiteral() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringLiteral.ftlh");
    }

    @Test
    public void testStringLiteral2() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringLiteral2.ftlh");
    }

    @Test
    public void testStringConcat() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringConcat.ftlh");
    }
    
}
