package freemarker.core;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class LegacyFMParserConstructorsTest {

    @Test
    public void test1() throws ParseException {
        FMParser parser = new FMParser("x");
        parser.Root();
    }
    
    @Test
    public void testCreateExpressionParser() throws ParseException {
         FMParser parser = FMParser.createExpressionParser("x + y");
         parser.Expression();
    }

    @Test
    public void testCreateExpressionParser2() throws IOException {
         FMParser parser = FMParser.createExpressionParser("x + 1");
         parser.setTemplate(new Template(null, "", new Configuration()));
         parser.Expression();
    }
    
}
