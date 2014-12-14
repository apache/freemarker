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
    public void tldDiscoveryBasic() throws Exception {
        try {
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
        }
    }

    @Test
    public void tldDiscoveryEmulatedProblems1() throws Exception {
        try {
            JspTestFreemarkerServlet.emulateNoJarURLConnections = true;
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
        }       
    }

    @Test
    public void tldDiscoveryEmulatedProblems2() throws Exception {
        try {
            JspTestFreemarkerServlet.emulateNoJarURLConnections = true;
            JspTestFreemarkerServlet.emulateNoUrlToFileConversions = true;
            // Because of emulateNoUrlToFileConversions = true it won't be able to list the directories, so:
            System.setProperty(
                    FreemarkerServlet.SYSTEM_PROPERTY_CLASSPATH_TLDS,
                    "META-INF/tldDiscovery-ClassPathTlds-3.tld");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
            System.clearProperty(FreemarkerServlet.SYSTEM_PROPERTY_CLASSPATH_TLDS);
        }
    }

    @Test
    public void tldDiscoveryClasspathOnly() throws Exception {
        try {
            System.setProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES, "clear, classpath");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt", "tester?view=test1.ftl");
        } finally {
            JspTestFreemarkerServlet.resetToDefaults();
            System.clearProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES);
        }
    }

    @Test
    public void tldDiscoveryRelative() throws Exception {
        assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "subdir/test-rel.txt", "tester?view=subdir/test-rel.ftl");
    }
    
}
