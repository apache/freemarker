package freemarker.ext.jsp;

import org.junit.Test;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.test.servlet.WebAppTestCase;

/**
 * Tests {@link FreemarkerServlet} on a real (embedded) Servlet container.
 */
public class RealServletContainertTest extends WebAppTestCase {

    @Test
    public void test1() throws Exception {
        assertJSPAndFTLOutputEquals("basic", "tester?view=1");
    }
    
    @Test
    public void test2() throws Exception {
        assertOutputsEqual("basic", "tester?view=1.jsp", "tester?view=1-jstl.ftl");        
    }

}
