/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.servlet.jsp;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateExceptionHandler;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.servlet.FreemarkerServlet;
import org.apache.freemarker.servlet.FreemarkerServletConfigurationBuilder;
import org.apache.freemarker.servlet.WebAppTemplateLoader;
import org.apache.freemarker.servlet.test.DefaultModel2TesterAction;
import org.apache.freemarker.servlet.test.WebAppTestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests {@link FreemarkerServlet} on a real (embedded) Servlet container.
 */
@SuppressWarnings("boxing")
@SuppressFBWarnings(value="ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification="Hack needed for testing only")
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
    public void basicServletScopeAttributes() throws Exception {
        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl");

        restartWebAppIfStarted(WEBAPP_BASIC);  // To clear the application scope attributes
        assertExpectedEqualsOutput(WEBAPP_BASIC, "attributes-modernModels.txt", "tester"
                + "?action=" + AllKindOfContainersModel2Action.class.getName()
                + "&view=attributes.ftl&viewServlet=freemarker-modernModels");
        // [FM3] Won't need the "modern" servlet as soon as DOW defaults change to recommended values
    }

    @Test
    public void basicELFunctions() throws Exception {
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=customELFunctions1");
    }

    // https://issues.apache.org/jira/browse/FREEMARKER-18
    @Test
    public void basicELFunctionsTagNameClash() throws Exception {
        // System.out.println(getResponseContent(WEBAPP_BASIC, "tester?view=elFunctionsTagNameClash.jsp"));
        // System.out.println(getResponseContent(WEBAPP_BASIC, "tester?view=elFunctionsTagNameClash.ftl"));
        assertJSPAndFTLOutputEquals(WEBAPP_BASIC, "tester?view=elFunctionsTagNameClash");
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
        Assert.assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS, "missing.jsp"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS, "failing-runtime.jsp"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS, "failing-parsetime.jsp"));
        
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-default-dev"));
        Assert.assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-default-dev"));
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-default-dev"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-default-dev"));
        
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-default-prod"));
        Assert.assertEquals(404, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-default-prod"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-default-prod"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-default-prod"));
        
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=not-failing.ftl&viewServlet=freemarker-future-prod"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=missing.ftl&viewServlet=freemarker-future-prod"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-runtime.ftl&viewServlet=freemarker-future-prod"));
        Assert.assertEquals(500, getResponseStatusCode(WEBAPP_ERRORS,
                "tester?view=failing-parsetime.ftlnv&viewServlet=freemarker-future-prod"));
    }
    
    @Test
    public void testTemplateLoaderConfig() throws Exception {
        Assert.assertEquals("from /WEB-INF/classes", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-class-root"));
        Assert.assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=sub/test2.ftl&viewServlet=freemarker-class-root"));
        Assert.assertEquals("from /WEB-INF/classes/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-class-sub"));
        Assert.assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test2.ftl&viewServlet=freemarker-class-sub"));
        
        Assert.assertEquals("from /WEB-INF/classes", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-classpath-root"));
        Assert.assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=sub/test2.ftl&viewServlet=freemarker-classpath-root"));
        Assert.assertEquals("from /WEB-INF/classes/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-classpath-sub"));
        Assert.assertEquals("from WEB-INF/lib/templates.jar/sub", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test2.ftl&viewServlet=freemarker-classpath-sub"));
        
        Assert.assertEquals("from /WEB-INF/templates", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-webinfPerTemplates"));
        Assert.assertEquals("from /", getResponseContent(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-contentRoot"));
    }
    
    @Test
    public void testConfigurationDefaults() throws Exception {
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertDefaultsFreemarkerServlet"));
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertCustomizedDefaultsFreemarkerServlet"));
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertObjectWrapperDefaults1FreemarkerServlet"));
        Assert.assertEquals(200, getResponseStatusCode(WEBAPP_CONFIG,
                "tester?view=test.ftl&viewServlet=freemarker-assertObjectWrapperDefaults2FreemarkerServlet"));
    }
    
    @Test
    public void testMultipleLoaders() throws Exception {
       Assert.assertEquals("In test.ftl",
               getResponseContent(WEBAPP_MULTIPLE_LOADERS, "tester?view=test.ftl")); 
       Assert.assertEquals("In classpath-test.ftl",
               getResponseContent(WEBAPP_MULTIPLE_LOADERS, "tester?view=classpath-test.ftl")); 
    }

    public static class AllKindOfContainersModel2Action extends DefaultModel2TesterAction {

        @Override
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

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public Integer getValue() {
                return value;
            }

            @Override
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

        @Override
        protected void doAssertions(Configuration cfg) {
            assertEquals(Configuration.VERSION_3_0_0, cfg.getIncompatibleImprovements());
            
            assertSame(cfg.getTemplateExceptionHandler(), TemplateExceptionHandler.HTML_DEBUG_HANDLER);

            {
                ObjectWrapper ow = cfg.getObjectWrapper();
                assertTrue(ow instanceof DefaultObjectWrapper);
                assertEquals(Configuration.VERSION_3_0_0, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
            }
            
            {
                TemplateLoader tl = cfg.getTemplateLoader();
                assertTrue(tl instanceof ClassTemplateLoader);
                assertEquals("/", ((ClassTemplateLoader) tl).getBasePackagePath());
            }
            
        }
        
    }
    
    public static class AssertCustomizedDefaultsFreemarkerServlet extends AssertingFreemarkerServlet {

        @Override
        protected Configuration.ExtendableBuilder createConfigurationBuilder() {
            return new FreemarkerServletConfigurationBuilder(
                    AssertCustomizedDefaultsFreemarkerServlet.this, Configuration.VERSION_3_0_0) {

                @Override
                protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
                    return TemplateExceptionHandler.RETHROW_HANDLER;
                }

                @Override
                protected String getDefaultBooleanFormat() {
                    return "Y,N";
                }

                @Override
                protected ObjectWrapper getDefaultObjectWrapper() {
                    DefaultObjectWrapper.Builder bwb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
                    bwb.setUseModelCache(true);
                    return bwb.build();
                }

                @Override
                protected TemplateLoader getDefaultTemplateLoader() {
                    return new WebAppTemplateLoader(getServletContext());
                }
            };
        }

        @Override
        protected void doAssertions(Configuration cfg) {
            assertEquals(Configuration.VERSION_3_0_0, cfg.getIncompatibleImprovements());
            
            assertSame(cfg.getTemplateExceptionHandler(), TemplateExceptionHandler.RETHROW_HANDLER);
            
            assertEquals("Y,N", cfg.getBooleanFormat());
            
            {
                ObjectWrapper ow = cfg.getObjectWrapper();
                assertSame(DefaultObjectWrapper.class, ow.getClass());
                assertTrue(((DefaultObjectWrapper) ow).getUseModelCache());
                assertEquals(Configuration.VERSION_3_0_0, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
            }
            
            {
                TemplateLoader tl = cfg.getTemplateLoader();
                assertTrue(tl instanceof WebAppTemplateLoader);
            }
            
        }
        
    }
    
    public static class AssertObjectWrapperDefaults1FreemarkerServlet extends AssertingFreemarkerServlet {

        @Override
        protected void doAssertions(Configuration cfg) throws Exception {
            ObjectWrapper ow = cfg.getObjectWrapper();
            assertSame(DefaultObjectWrapper.class, ow.getClass());
            assertTrue(((DefaultObjectWrapper) ow).getUseModelCache());
        }

        @Override
        protected Configuration.ExtendableBuilder createConfigurationBuilder() {
            return new FreemarkerServletConfigurationBuilder(
                    AssertObjectWrapperDefaults1FreemarkerServlet.this, Configuration.VERSION_3_0_0) {
                @Override
                protected ObjectWrapper getDefaultObjectWrapper() {
                    DefaultObjectWrapper.Builder bwb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
                    bwb.setUseModelCache(true);
                    assertEquals(Configuration.VERSION_3_0_0, bwb.getIncompatibleImprovements());
                    return bwb.build();
                }
            };
        }

    }

    public static class AssertObjectWrapperDefaults2FreemarkerServlet extends
            AssertObjectWrapperDefaults1FreemarkerServlet {

        @Override
        protected Configuration.ExtendableBuilder createConfigurationBuilder() {
            Configuration.ExtendableBuilder cfgB = super.createConfigurationBuilder();
            // This is not a proper way of doing this, but consistent behavior still needs to be tested.
            cfgB.setObjectWrapper(new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
            return cfgB;
        }
        
        @Override
        protected void doAssertions(Configuration cfg) throws Exception {
            ObjectWrapper ow = cfg.getObjectWrapper();
            assertSame(RestrictedObjectWrapper.class, ow.getClass());
            assertEquals(Configuration.VERSION_3_0_0, ((DefaultObjectWrapper) ow).getIncompatibleImprovements());
        }
        
    }
    
}
