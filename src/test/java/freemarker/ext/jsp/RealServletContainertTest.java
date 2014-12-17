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
    private static final String WEBAPP_EL_FUNCTIONS = "elFunctions";
    private static final String WEBAPP_TLD_DISCOVERY = "tldDiscovery";

    @Test
    public void basic1() throws Exception {
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=1");
    }
    
    @Ignore  // c:forEach fails because of EL context issues
    @Test
    public void basic1JSTL() throws Exception {
        assertOutputsEqual(WEBAPP_BASIC, "tester?view=1.jsp", "tester?view=1-jstl.ftl");        
    }
    
    @Test
    public void elFunctions() throws Exception {
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.jsp"));
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.ftl"));
        assertJSPAndFTLOutputEquals(WEBAPP_EL_FUNCTIONS, "tester?view=1");
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
                    "META-INF/tldDiscovery MetaInfTldSources-1.tld");
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

    /**
     * Tests that (1) webInfPerLibJars still loads from WEB-INF/lib/*.jar, and (2) that
     * {@link FreemarkerServlet#SYSTEM_PROPERTY_META_INF_TLD_SOURCES} indeed overrides the init-param, and that the
     * Jetty container's JSTL jar-s will still be discovered.
     */
    @Test
    public void tldDiscoveryNoClasspath() throws Exception {
        try {
            System.setProperty(FreemarkerServlet.SYSTEM_PROPERTY_META_INF_TLD_SOURCES, "clear, webInfPerLibJars");
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY,
                    "test-noClasspath.txt", "tester?view=test-noClasspath.ftl");
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
