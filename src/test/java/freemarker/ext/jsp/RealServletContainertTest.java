package freemarker.ext.jsp;

import org.junit.Ignore;
import org.junit.Test;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.test.servlet.WebAppTestCase;

/**
 * Tests {@link FreemarkerServlet} on a real (embedded) Servlet container.
 */
public class RealServletContainertTest extends WebAppTestCase {

    private static final String WEBAPP_BASIC = "basic";
    private static final String WEBAPP_TLD_DISCOVERY = "tldDiscovery";

    @Test
    public void basic1() throws Exception {
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=1");
    }
    
    @Ignore  // c:forEach fails because of EL context issues
    @Test
    public void test2() throws Exception {
        assertOutputsEqual(WEBAPP_BASIC, "tester?view=1.jsp", "tester?view=1-jstl.ftl");        
    }

    @Test
    public void tldDiscovery1() throws Exception {
        assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
    }

}
