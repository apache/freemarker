package freemarker.core;

import org.junit.Test;

public class LegacyFMParserConstructorsTest {

    @Test
    public void test1() throws ParseException {
        FMParser parser = new FMParser("x");
        parser.Root();
    }
    
}
