package freemarker.template;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class NullConfigurationTest {

    @Test
    public void testTemplateNPEBug() throws IOException {
        new Template("legacy", new StringReader("foo"));
    }
    
}
