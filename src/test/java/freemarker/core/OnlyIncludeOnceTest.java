package freemarker.core;

import freemarker.manual.ExamplesTest;
import org.junit.Test;

public class OnlyIncludeOnceTest extends ExamplesTest {

    @Test
    public void testIncludeOnlyOnce() throws Exception {
        assertOutputForNamed("onlyIncludeOnce-main.ftlh");
    }
}
