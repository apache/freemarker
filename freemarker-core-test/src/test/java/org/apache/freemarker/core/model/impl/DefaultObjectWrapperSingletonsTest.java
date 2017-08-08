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

package org.apache.freemarker.core.model.impl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.ref.Reference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.test.TestUtils;

import junit.framework.TestCase;

public class DefaultObjectWrapperSingletonsTest extends TestCase {

    public DefaultObjectWrapperSingletonsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        DefaultObjectWrapper.Builder.clearInstanceCache();
    }

    public void testBuilderEqualsAndHash() throws Exception {
        assertEquals(Configuration.VERSION_3_0_0, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).getIncompatibleImprovements());
        try {
            new DefaultObjectWrapper.Builder(TestUtils.getClosestFutureVersion());
            fail("Maybe you need to update this test for the new FreeMarker version");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }

        DefaultObjectWrapper.Builder builder1;
        DefaultObjectWrapper.Builder builder2;
        
        builder1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        builder2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        assertEquals(builder1, builder2);
        
        builder1.setExposeFields(true);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());
        builder2.setExposeFields(true);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
        
        builder1.setExposureLevel(0);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());
        builder2.setExposureLevel(0);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
        
        builder1.setExposureLevel(1);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());
        builder2.setExposureLevel(1);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
        
        builder1.setDefaultDateType(TemplateDateModel.DATE);
        assertNotEquals(builder1, builder2);
        builder2.setDefaultDateType(TemplateDateModel.DATE);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
        
        builder1.setStrict(true);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());
        builder2.setStrict(true);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());

        builder1.setUseModelCache(true);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());
        builder2.setUseModelCache(true);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
        
        AlphabeticalMethodSorter ms = new AlphabeticalMethodSorter(true);
        builder1.setMethodSorter(ms);
        assertNotEquals(builder1, builder2);
        builder2.setMethodSorter(ms);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());

        MethodAppearanceFineTuner maft = new MethodAppearanceFineTuner() {
            @Override
            public void process(DecisionInput in, Decision out) { }
        };
        builder1.setMethodAppearanceFineTuner(maft);
        assertNotEquals(builder1, builder2);
        builder2.setMethodAppearanceFineTuner(maft);
        assertEquals(builder1, builder2);
        assertEquals(builder1.hashCode(), builder2.hashCode());
    }
    
    public void testDefaultObjectWrapperBuilderProducts() throws Exception {
        List<DefaultObjectWrapper> hardReferences = new LinkedList<>();
        
        assertEquals(0, getDefaultObjectWrapperInstanceCacheSize());
        
        {
            DefaultObjectWrapper ow = getDefaultObjectWrapperWithSetting(Configuration.VERSION_3_0_0, true);
            assertEquals(1, getDefaultObjectWrapperInstanceCacheSize());
            assertSame(ow.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_3_0_0, ow.getIncompatibleImprovements());
            assertFalse(ow.isStrict());
            assertTrue(ow.getUseModelCache());
            assertEquals(TemplateDateModel.UNKNOWN, ow.getDefaultDateType());
            assertSame(ow, ow.getOuterIdentity());
            assertTrue(ow.isClassIntrospectionCacheRestricted());
            assertNull(ow.getMethodAppearanceFineTuner());
            assertNull(ow.getMethodSorter());

            assertSame(ow, getDefaultObjectWrapperWithSetting(Configuration.VERSION_3_0_0, true));
            assertEquals(1, getDefaultObjectWrapperInstanceCacheSize());
            
            hardReferences.add(ow);
        }
        
        {
            DefaultObjectWrapper ow = getDefaultObjectWrapperWithSetting(Configuration.VERSION_3_0_0, false);
            assertEquals(2, getDefaultObjectWrapperInstanceCacheSize());
            assertSame(ow.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_3_0_0, ow.getIncompatibleImprovements());
            assertFalse(ow.getUseModelCache());

            assertSame(ow, getDefaultObjectWrapperWithSetting(Configuration.VERSION_3_0_0, false));
            
            hardReferences.add(ow);
        }
        
        {
            DefaultObjectWrapper ow1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .build();
            DefaultObjectWrapper ow2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .build();
            assertEquals(3, getDefaultObjectWrapperInstanceCacheSize());
            assertSame(ow1, ow2);
            
            assertSame(ow1.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_3_0_0, ow1.getIncompatibleImprovements());
            assertEquals(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY, ow1.getExposureLevel());
            assertFalse(ow1.isStrict());
            assertEquals(TemplateDateModel.UNKNOWN, ow1.getDefaultDateType());
            assertSame(ow1, ow1.getOuterIdentity());
            
            hardReferences.add(ow1);
        }
        
        {
            DefaultObjectWrapper ow1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposeFields(true)
                    .build();
            DefaultObjectWrapper ow2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposeFields(true)
                    .build();
            assertEquals(4, getDefaultObjectWrapperInstanceCacheSize());
            assertSame(ow1, ow2);
            
            assertSame(ow1.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_3_0_0, ow1.getIncompatibleImprovements());
            assertTrue(ow1.isExposeFields());
            
            hardReferences.add(ow1);
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .strict(true)
                    .defaultDateType(TemplateDateModel.DATE_TIME)
                    .outerIdentity(new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build())
                    .build();
            assertEquals(5, getDefaultObjectWrapperInstanceCacheSize());
            assertTrue(ow.isStrict());
            assertEquals(TemplateDateModel.DATE_TIME, ow.getDefaultDateType());
            assertSame(RestrictedObjectWrapper.class, ow.getOuterIdentity().getClass());
            
            hardReferences.add(ow);
        }
        
        // Effect of reference and cache clearings:
        {
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            assertEquals(5, getDefaultObjectWrapperInstanceCacheSize());
            assertEquals(5, getDefaultObjectWrapperNonClearedInstanceCacheSize());
            
            clearDefaultObjectWrapperInstanceCacheReferences(false);
            assertEquals(5, getDefaultObjectWrapperInstanceCacheSize());
            assertEquals(0, getDefaultObjectWrapperNonClearedInstanceCacheSize());
            
            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            assertNotSame(bw1, bw2);
            assertEquals(5, getDefaultObjectWrapperInstanceCacheSize());
            assertEquals(1, getDefaultObjectWrapperNonClearedInstanceCacheSize());
            
            assertSame(bw2, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
            assertEquals(1, getDefaultObjectWrapperNonClearedInstanceCacheSize());
            
            clearDefaultObjectWrapperInstanceCacheReferences(true);
            DefaultObjectWrapper bw3 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            assertNotSame(bw2, bw3);
            assertEquals(1, getDefaultObjectWrapperInstanceCacheSize());
            assertEquals(1, getDefaultObjectWrapperNonClearedInstanceCacheSize());
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .useModelCache(true)
                    .build();
            assertTrue(ow.getUseModelCache());
            assertEquals(2, getDefaultObjectWrapperInstanceCacheSize());
            
            hardReferences.add(ow);
        }
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    private DefaultObjectWrapper getDefaultObjectWrapperWithSetting(Version ici, boolean useModelCache) {
        return new DefaultObjectWrapper.Builder(ici).useModelCache(useModelCache).build();
    }

    public void testMultipleTCCLs() {
        List<DefaultObjectWrapper> hardReferences = new LinkedList<>();
        
        assertEquals(0, getDefaultObjectWrapperInstanceCacheSize());
        
        DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        assertEquals(1, getDefaultObjectWrapperInstanceCacheSize());
        hardReferences.add(bw1);
        
        ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        // Doesn't mater what, just be different from oldTCCL: 
        ClassLoader newTCCL = oldTCCL == null ? getClass().getClassLoader() : null;
        
        DefaultObjectWrapper bw2;
        Thread.currentThread().setContextClassLoader(newTCCL);
        try {
            bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            assertEquals(2, getDefaultObjectWrapperInstanceCacheSize());
            hardReferences.add(bw2);
            
            assertNotSame(bw1, bw2);
            assertSame(bw2, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
        
        assertSame(bw1, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
        assertEquals(2, getDefaultObjectWrapperInstanceCacheSize());

        DefaultObjectWrapper bw3;
        Thread.currentThread().setContextClassLoader(newTCCL);
        try {
            assertSame(bw2, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
            
            DefaultObjectWrapper.Builder bwb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
            bwb.setExposeFields(true);
            bw3 = bwb.build();
            assertEquals(3, getDefaultObjectWrapperInstanceCacheSize());
            hardReferences.add(bw3);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
        
        {
            DefaultObjectWrapper.Builder bwb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
            bwb.setExposeFields(true);
            DefaultObjectWrapper bw4 = bwb.build();
            assertEquals(4, getDefaultObjectWrapperInstanceCacheSize());
            assertNotSame(bw3, bw4);
            hardReferences.add(bw4);
        }
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    public void testClassInrospectorCache() throws TemplateModelException {
        assertFalse(new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .usePrivateCaches(true).build().isClassIntrospectionCacheRestricted());
        assertTrue(new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .build().isClassIntrospectionCacheRestricted());
        
        ClassIntrospector.Builder.clearInstanceCache();
        DefaultObjectWrapper.Builder.clearInstanceCache();
        checkClassIntrospectorCacheSize(0);
        
        List<DefaultObjectWrapper> hardReferences = new LinkedList<>();

        {
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            checkClassIntrospectorCacheSize(1);
            
            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_SAFE)  // this was already set to this
                    .useModelCache(true)  // this shouldn't matter for the introspection cache
                    .build();
            checkClassIntrospectorCacheSize(1);
            
            assertSame(bw2.getClassIntrospector(), bw1.getClassIntrospector());
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertTrue(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));
            assertTrue(bw1.isClassIntrospectionCacheRestricted());
            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposeFields(true)
                    .build();
            checkClassIntrospectorCacheSize(2);
            // Wrapping tests:
            assertTrue(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertTrue(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_ALL)
                    .exposeFields(true)
                    .build();
            checkClassIntrospectorCacheSize(3);
            // Wrapping tests:
            assertTrue(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertTrue(exposesMethods(ow));
            assertTrue(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_ALL)
                    .build();
            checkClassIntrospectorCacheSize(4);
            // Wrapping tests:
            assertFalse(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertTrue(exposesMethods(ow));
            assertTrue(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_NOTHING)
                    .build();
            checkClassIntrospectorCacheSize(5);
            // Wrapping tests:
            assertFalse(exposesFields(ow));
            assertFalse(exposesProperties(ow));
            assertFalse(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_NOTHING)
                    .exposeFields(true)
                    .build();
            checkClassIntrospectorCacheSize(6);
            // Wrapping tests:
            assertTrue(exposesFields(ow));
            assertFalse(exposesProperties(ow));
            assertFalse(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .exposeFields(true)
                    .build();
            checkClassIntrospectorCacheSize(7);
            // Wrapping tests:
            assertTrue(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertFalse(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }
        
        {
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .useModelCache(true)
                    .exposeFields(false)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .build();
            checkClassIntrospectorCacheSize(8);
            ClassIntrospector ci1 = bw1.getClassIntrospector();
            
            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .useModelCache(false)  // Shouldn't mater for the ClassIntrospector
                    .exposeFields(false)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .build();
            ClassIntrospector ci2 = bw2.getClassIntrospector();
            checkClassIntrospectorCacheSize(8);
            
            assertSame(ci1, ci2);
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertFalse(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));

            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }

        // The ClassInrospector cache couldn't become cleared in reality otherwise:
        DefaultObjectWrapper.Builder.clearInstanceCache();

        clearClassIntrospectorInstanceCacheReferences(false);
        checkClassIntrospectorCacheSize(8);
        assertEquals(0, getClassIntrospectorNonClearedInstanceCacheSize());

        {
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                    .build();
            checkClassIntrospectorCacheSize(8);
            assertEquals(1, getClassIntrospectorNonClearedInstanceCacheSize());
            ClassIntrospector ci1 = bw1.getClassIntrospector();
            
            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .exposureLevel(DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY)
                .useModelCache(true)  // Shouldn't mater for the ClassIntrospector
                .build();

            ClassIntrospector ci2 = bw2.getClassIntrospector();
            
            assertSame(ci1, ci2);
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertFalse(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));

            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .build();
            checkClassIntrospectorCacheSize(8);
            assertEquals(2, getClassIntrospectorNonClearedInstanceCacheSize());
            // Wrapping tests:
            assertFalse(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertTrue(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }

        clearClassIntrospectorInstanceCacheReferences(true);
        checkClassIntrospectorCacheSize(8);
        assertEquals(0, getClassIntrospectorNonClearedInstanceCacheSize());
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .exposeFields(true)
                    .build();
            checkClassIntrospectorCacheSize(1);
            // Wrapping tests:
            assertTrue(exposesFields(ow));
            assertTrue(exposesProperties(ow));
            assertTrue(exposesMethods(ow));
            assertFalse(exposesUnsafe(ow));
            // Prevent introspection cache GC:
            hardReferences.add(ow);
        }
        
        {
            MethodAppearanceFineTuner methodAppearanceFineTuner = new MethodAppearanceFineTuner() {
                @Override
                public void process(DecisionInput in, Decision out) {
                }
            };
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .useModelCache(false)
                    .methodAppearanceFineTuner(methodAppearanceFineTuner)  // spoils ClassIntrospector() sharing
                    .build();
            assertSame(
                    bw1,
                    new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                            .useModelCache(false)
                            .methodAppearanceFineTuner(methodAppearanceFineTuner)  // spoils ClassIntrospector() sharing
                            .build());

            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .useModelCache(true)
                    .methodAppearanceFineTuner(methodAppearanceFineTuner)  // spoils ClassIntrospector() sharing
                    .build();
            checkClassIntrospectorCacheSize(1);
            assertNotSame(bw1, bw2);
            assertNotSame(bw1.getClassIntrospector(), bw2.getClassIntrospector());
            assertTrue(bw1.isClassIntrospectionCacheRestricted());
            assertTrue(bw2.isClassIntrospectionCacheRestricted());
        }

        {
            DefaultObjectWrapper bw1 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .methodAppearanceFineTuner(GetlessMethodsAsPropertyGettersRule.INSTANCE)  // doesn't spoils sharing
                    .useModelCache(false)
                    .build();
            assertSame(
                    bw1,
                    new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                            .methodAppearanceFineTuner(GetlessMethodsAsPropertyGettersRule.INSTANCE)  // doesn't spoils sharing
                            .useModelCache(false)
                            .build());
            checkClassIntrospectorCacheSize(2);

            DefaultObjectWrapper bw2 = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .methodAppearanceFineTuner(GetlessMethodsAsPropertyGettersRule.INSTANCE)  // doesn't spoils sharing
                    .useModelCache(true)
                    .build();
            checkClassIntrospectorCacheSize(2);
            
            assertNotSame(bw1, bw2);
            assertSame(bw1.getClassIntrospector(), bw2.getClassIntrospector());  // !
            assertTrue(bw2.isClassIntrospectionCacheRestricted());
        }
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    private void checkClassIntrospectorCacheSize(int expectedSize) {
        assertEquals(expectedSize, getClassIntrospectorInstanceCacheSize());
    }

    public class C {
        
        public String foo = "FOO";
        
        public String getBar() {
            return "BAR";
        }
        
    }

    private boolean exposesFields(DefaultObjectWrapper ow) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) ow.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("foo");
        if (r == null) return false;
        assertEquals("FOO", r.getAsString());
        return true;
    }

    private boolean exposesProperties(DefaultObjectWrapper ow) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) ow.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("bar");
        if (r == null) return false;
        assertEquals("BAR", r.getAsString());
        return true;
    }

    private boolean exposesMethods(DefaultObjectWrapper ow) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) ow.wrap(new C());
        return thm.get("getBar") != null;
    }

    private boolean exposesUnsafe(DefaultObjectWrapper ow) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) ow.wrap(new C());
        return thm.get("wait") != null;
    }
    
    static int getClassIntrospectorInstanceCacheSize() {
        Map instanceCache = ClassIntrospector.Builder.getInstanceCache();
        synchronized (instanceCache) {
            return instanceCache.size();
        }
    }

    static int getClassIntrospectorNonClearedInstanceCacheSize() {
        Map instanceCache = ClassIntrospector.Builder.getInstanceCache();
        synchronized (instanceCache) {
            int cnt = 0;
            for (Iterator it = instanceCache.values().iterator(); it.hasNext(); ) {
                if (((Reference) it.next()).get() != null) cnt++;
            }
            return cnt;
        }
    }
    
    static void clearClassIntrospectorInstanceCacheReferences(boolean enqueue) {
        Map instanceCache = ClassIntrospector.Builder.getInstanceCache();
        synchronized (instanceCache) {
            for (Iterator it = instanceCache.values().iterator(); it.hasNext(); ) {
                Reference ref = ((Reference) it.next());
                ref.clear();
                if (enqueue) {
                    ref.enqueue();
                }
            }
        }
    }

    static int getDefaultObjectWrapperInstanceCacheSize() {
        Map instanceCache = DefaultObjectWrapper.Builder.getInstanceCache();
        synchronized (instanceCache) {
            int size = 0; 
            for (Iterator it1 = instanceCache.values().iterator(); it1.hasNext(); ) {
                size += ((Map) it1.next()).size();
            }
            return size;
        }
    }

    static int getDefaultObjectWrapperNonClearedInstanceCacheSize() {
        Map instanceCache = DefaultObjectWrapper.Builder.getInstanceCache();
        synchronized (instanceCache) {
            int cnt = 0;
            for (Iterator it1 = instanceCache.values().iterator(); it1.hasNext(); ) {
                Map tcclScope = (Map) it1.next();
                for (Iterator it2 = tcclScope.values().iterator(); it2.hasNext(); ) {
                    if (((Reference) it2.next()).get() != null) cnt++;
                }
            }
            return cnt;
        }
    }
    
    static void clearDefaultObjectWrapperInstanceCacheReferences(boolean enqueue) {
        Map instanceCache = DefaultObjectWrapper.Builder.getInstanceCache();
        synchronized (instanceCache) {
            for (Iterator it1 = instanceCache.values().iterator(); it1.hasNext(); ) {
                Map tcclScope = (Map) it1.next();
                for (Iterator it2 = tcclScope.values().iterator(); it2.hasNext(); ) {
                    Reference ref = ((Reference) it2.next());
                    ref.clear();
                    if (enqueue) {
                        ref.enqueue();
                    }
                }
            }
        }
    }
    
}
