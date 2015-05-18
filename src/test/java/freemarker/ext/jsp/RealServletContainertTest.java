/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.jsp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.test.servlet.DefaultModel2TesterAction;
import freemarker.test.servlet.WebAppTestCase;

/**
 * Tests {@link FreemarkerServlet} on a real (embedded) Servlet container.
 */
@SuppressWarnings("boxing")
public class RealServletContainertTest extends WebAppTestCase {

    private static final String WEBAPP_BASIC = "basic";
    private static final String WEBAPP_TLD_DISCOVERY = "tldDiscovery";
    private static final String WEBAPP_ERRORS = "errors";
    private static final String WEBAPP_CONFIG = "config";
    private static final String WEBAPP_MULTIPLE_LOADERS = "multipleLoaders";

    @Test
    public void basicTrivial() throws Exception {
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=trivial");
    }

    @Test
    @Ignore  // c:forEach fails because of EL context issues
    public void basicTrivialJSTL() throws Exception {
        assertOutputsEqual(WEBAPP_BASIC, "tester?view=trivial.jsp", "tester?view=trivial-jstl-@Ignore.ftl");        
    }

    @Test
    public void basicCustomTags1() throws Exception {
        assertExpectedEqualsOutput(WEBAPP_BASIC, "customTags1.txt", "tester?view=customTags1.ftl", false);
    }

    @Test
    public void basicCustomAttributes() throws Exception {
        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl");

        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes-2.3.22-future.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl&viewServlet=freemarker-2.3.22-future");
        
        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes-2.3.0.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl&viewServlet=freemarker-2.3.0",
                true,
                ImmutableList.<Pattern>of(
                        Pattern.compile("(?<=^Date-time: ).*", Pattern.MULTILINE), // Uses Date.toString, so plat. dep.
                        Pattern.compile("(?<=^MyMap: ).*", Pattern.MULTILINE)  // Uses HashMap, so order unknown
                        ));
    }

    @Test
    public void basicELFunctions() throws Exception {
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.jsp"));
        //System.out.println(getResponseContent(WEBAPP_EL_FUNCTIONS, "tester?view=1.ftl"));
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=customELFunctions1");
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
    public void tldDiscoveryBasicDefultOverride() throws Exception {
        try {
            restartWebAppIfStarted(WEBAPP_TLD_DISCOVERY);
            assertExpectedEqualsOutput(WEBAPP_TLD_DISCOVERY, "test1.txt",
                    "tester?view=test1.ftl&viewServlet=freemarker-defaultOverride");
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
    
    @Test
    public void errorStatusCodes() throws Exception {
        assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS, "missing.jsp"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS, "failing-runtime.jsp"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS, "failing-parsetime.jsp"));
        
        assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-default-dev"));
        assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-default-dev"));
        assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-default-dev"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-default-dev"));
        
        assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-default-prod"));
        assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-default-prod"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-default-prod"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-default-prod"));
        
        assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-future-prod"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-future-prod"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-future-prod"));
        assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-future-prod"));
    }
    
    @Test
    public void testTemplateLoaderConfig() throws Exception {
        assertEquals("from /WEB-INF/classes", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-class-root"));
        assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=sub/test2.ftl&viewServlet=freemarker-class-root"));
        assertEquals("from /WEB-INF/classes/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-class-sub"));
        assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test2.ftl&viewServlet=freemarker-class-sub"));
        
        assertEquals("from /WEB-INF/classes", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-classpath-root"));
        assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=sub/test2.ftl&viewServlet=freemarker-classpath-root"));
        assertEquals("from /WEB-INF/classes/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-classpath-sub"));
        assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test2.ftl&viewServlet=freemarker-classpath-sub"));
        
        assertEquals("from /WEB-INF/templates", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-webinfPerTemplates"));
        assertEquals("from /", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-contentRoot"));
    }
    
    @Test
    public void testConfigurationDefaults() throws Exception {
        assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertDefaultsFreemarkerServlet"));
        assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertDefaultsIcI2322FreemarkerServlet"));
        assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertCustomizedDefaultsFreemarkerServlet"));
        assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertObjectWrapperDefaults1FreemarkerServlet"));
        assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertObjectWrapperDefaults2FreemarkerServlet"));
    }
    
    @Test
    public void testMultipleLoaders() throws Exception {
       assertEquals("In test.ftl",
               getResponseContent(WEBAPP_MULTIPLE_LOADERS, "tester?view=test.ftl")); 
       assertEquals("In classpath-test.ftl",
               getResponseContent(WEBAPP_MULTIPLE_LOADERS, "tester?view=classpath-test.ftl")); 
    }

    public static class AllKindOfContainersModel2Action extends DefaultModel2TesterAction {

        public String execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
            req.setAttribute("linkedList", initTestCollection(new LinkedList<Integer>()));
            req.setAttribute("arrayList", initTestCollection(new ArrayList<Integer>()));
            req.setAttribute("myList", new MyList());
            
            req.setAttribute("linkedHashMap", initTestMap(new LinkedHashMap()));
            req.setAttribute("treeMap", initTestMap(new TreeMap()));
            req.setAttribute("myMap", new MyMap());
            
            req.setAttribute("treeSet", initTestCollection(new TreeSet()));
            
            return super.execute(req, resp);
        }
        
        private Collection<Integer> initTestCollection(Collection<Integer> list) {
            for (int i = 0; i < 3; i++) list.add(i + 1);
            return list;
        }

        private Map<String, Integer> initTestMap(Map<String, Integer> map) {
            for (int i = 0; i < 3; i++) map.put(String.valueOf((char) ('a' + i)), i + 1);
            return map;
        }
        
    }
    
    public static class MyMap extends AbstractMap<String, Integer> {

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            return ImmutableSet.<Map.Entry<String, Integer>>of(
                    new MyEntry("a", 1), new MyEntry("b", 2), new MyEntry("c", 3));
        }
        
        private static class MyEntry implements Map.Entry<String, Integer> {
            
            private final String key;
            private final Integer value;

            public MyEntry(String key, Integer value) {
                this.key = key;
                this.value = value;
            }

            public String getKey() {
                return key;
            }

            public Integer getValue() {
                return value;
            }

            public Integer setValue(Integer value) {
                throw new UnsupportedOperationException();
            }
            
        }
        
    }
    
    public static class MyList extends AbstractList<Integer> {

        @SuppressWarnings("boxing")
        @Override
        public Integer get(int index) {
            return index + 1;
        }

        @Override
        public int size() {
            return 3;
        }
        
    }
    
    public static abstract class AssertingFreemarkerServlet extends FreemarkerServlet {

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            Configuration cfg = getConfiguration();
            try {
                doAssertions(cfg);
            } catch (Exception e) {
                throw new ServletException("Test failed", e);
            }
            
            response.setStatus(200);
            response.getWriter().write("OK");
        }

        protected abstract void doAssertions(Configuration cfg) throws Exception;
        
    }
    
    public static class AssertDefaultsFreemarkerServlet extends AssertingFreemarkerServlet {

        protected void doAssertions(Configuration cfg) {
            assertEquals(Configuration.VERSION_2_3_22, cfg.getIncompatibleImprovements());
            
            assertSame(cfg.getTemplateExceptionHandler(), TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            
            assertFalse(cfg.getLogTemplateExceptions());
            
            {
                ObjectWrapper ow = cfg.getObjectWrapper();
                assertTrue(ow instanceof DefaultObjectWrapper);
                assertEquals(Configuration.VERSION_2_3_22, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
            }
            
            {
                TemplateLoader tl = cfg.getTemplateLoader();
                assertTrue(tl instanceof ClassTemplateLoader);
                assertEquals("/", ((ClassTemplateLoader) tl).getBasePackagePath());
            }
            
        }
        
    }

    public static class AssertDefaultsIcI2322FreemarkerServlet extends AssertDefaultsFreemarkerServlet {

        @Override
        protected Configuration createConfiguration() {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
            return cfg;
        }
        
    }
    
    public static class AssertCustomizedDefaultsFreemarkerServlet extends AssertingFreemarkerServlet {

        @Override
        protected Configuration createConfiguration() {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_20);
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(true);
            cfg.setObjectWrapper(new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build());
            cfg.setTemplateLoader(new WebappTemplateLoader(getServletContext()));
            return cfg;
        }

        protected void doAssertions(Configuration cfg) {
            assertEquals(Configuration.VERSION_2_3_20, cfg.getIncompatibleImprovements());
            
            assertSame(cfg.getTemplateExceptionHandler(), TemplateExceptionHandler.RETHROW_HANDLER);
            
            assertTrue(cfg.getLogTemplateExceptions());
            
            {
                ObjectWrapper ow = cfg.getObjectWrapper();
                assertSame(BeansWrapper.class, ow.getClass());
                assertEquals(Configuration.VERSION_2_3_21, ((BeansWrapper) ow).getIncompatibleImprovements());
            }
            
            {
                TemplateLoader tl = cfg.getTemplateLoader();
                assertTrue(tl instanceof WebappTemplateLoader);
            }
            
        }
        
    }
    
    public static class AssertObjectWrapperDefaults1FreemarkerServlet extends AssertingFreemarkerServlet {

        @Override
        protected void doAssertions(Configuration cfg) throws Exception {
            ObjectWrapper ow = cfg.getObjectWrapper();
            assertSame(BeansWrapper.class, ow.getClass());
            assertEquals(Configuration.VERSION_2_3_21, ((BeansWrapper) ow).getIncompatibleImprovements());
        }

        @Override
        protected ObjectWrapper createDefaultObjectWrapper() {
            return new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
        }
        
    }

    public static class AssertObjectWrapperDefaults2FreemarkerServlet extends
            AssertObjectWrapperDefaults1FreemarkerServlet {

        @Override
        protected Configuration createConfiguration() {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_20);
            cfg.setObjectWrapper(new SimpleObjectWrapper(Configuration.VERSION_2_3_22));
            return cfg;
        }
        
        @Override
        protected void doAssertions(Configuration cfg) throws Exception {
            ObjectWrapper ow = cfg.getObjectWrapper();
            assertSame(SimpleObjectWrapper.class, ow.getClass());
            assertEquals(Configuration.VERSION_2_3_22, ((BeansWrapper) ow).getIncompatibleImprovements());
        }
        
    }
    
}
