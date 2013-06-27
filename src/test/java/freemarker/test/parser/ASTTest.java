package freemarker.test.parser;

import java.io.FileNotFoundException;
import java.io.IOException;

import freemarker.core.ASTPrinter;
import freemarker.test.utility.FileTestCase;

public class ASTTest extends FileTestCase {

    public ASTTest(String name) {
        super(name);
    }
    
    public void test1() throws Exception {
        testAST("ast-1");
    }

    private void testAST(String testName) throws FileNotFoundException, IOException {
        final String templateName = testName + ".ftl";
        assertExpectedFileEqualsString(
                testName + ".ast",
                ASTPrinter.getASTAsString(templateName, loadResource(templateName)));
    }
    
}
