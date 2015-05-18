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

package freemarker.ext.beans;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecisionInput;
import freemarker.template.Configuration;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;

public class BeansWrapperSingletonsTest extends TestCase {

    public BeansWrapperSingletonsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        BeansWrapperBuilder.clearInstanceCache();  // otherwise ClassInrospector cache couldn't become cleared in reality
        _TemplateAPI.DefaultObjectWrapperFactory_clearInstanceCache();
        BeansWrapperBuilder.clearInstanceCache();
    }

    public void testBeansWrapperFactoryEquals() throws Exception {
        assertEquals(Configuration.VERSION_2_3_21, new BeansWrapperBuilder(Configuration.VERSION_2_3_21).getIncompatibleImprovements());
        assertEquals(Configuration.VERSION_2_3_0, new BeansWrapperBuilder(Configuration.VERSION_2_3_20).getIncompatibleImprovements());
        try {
            new BeansWrapperBuilder(new Version(2, 3, 24));
            fail("Maybe you need to update this test for the new FreeMarker version");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }

        BeansWrapperBuilder pa1;
        BeansWrapperBuilder pa2;
        
        pa1 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
        pa2 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
        assertEquals(pa1, pa2);
        
        pa1.setSimpleMapWrapper(true);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setSimpleMapWrapper(true);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        pa1.setExposeFields(true);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setExposeFields(true);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        pa1.setExposureLevel(0);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setExposureLevel(0);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        pa1.setExposureLevel(1);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setExposureLevel(1);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        pa1.setDefaultDateType(TemplateDateModel.DATE);
        assertNotEquals(pa1, pa2);
        pa2.setDefaultDateType(TemplateDateModel.DATE);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        pa1.setStrict(true);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setStrict(true);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());

        pa1.setUseModelCache(true);
        assertNotEquals(pa1, pa2);
        assertFalse(pa1.hashCode() == pa2.hashCode());
        pa2.setUseModelCache(true);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        AlphabeticalMethodSorter ms = new AlphabeticalMethodSorter(true);
        pa1.setMethodSorter(ms);
        assertNotEquals(pa1, pa2);
        pa2.setMethodSorter(ms);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
        
        MethodAppearanceFineTuner maft = new MethodAppearanceFineTuner() {
            public void process(MethodAppearanceDecisionInput in, MethodAppearanceDecision out) { }
        };
        pa1.setMethodAppearanceFineTuner(maft);
        assertNotEquals(pa1, pa2);
        pa2.setMethodAppearanceFineTuner(maft);
        assertEquals(pa1, pa2);
        assertTrue(pa1.hashCode() == pa2.hashCode());
    }
    
    public void testBeansWrapperFactoryProducts() throws Exception {
        List<BeansWrapper> hardReferences = new LinkedList<BeansWrapper>();
        
        assertEquals(0, getBeansWrapperInstanceCacheSize());
        
        {
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_19, true);
            assertEquals(1, getBeansWrapperInstanceCacheSize());
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            assertFalse(bw.isStrict());
            assertFalse(bw.getUseCache());
            assertEquals(TemplateDateModel.UNKNOWN, bw.getDefaultDateType());
            assertSame(bw, bw.getOuterIdentity());
            assertTrue(bw.isClassIntrospectionCacheRestricted());
            assertNull(bw.getMethodAppearanceFineTuner());
            assertNull(bw.getMethodSorter());
            
            try {
                bw.setExposeFields(true);  // can't modify the settings of a (potential) singleton
                fail();
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), containsString("modify"));
            }
            
            assertSame(bw, getBeansWrapperWithSetting(Configuration.VERSION_2_3_20, true));
            assertSame(bw, getBeansWrapperWithSetting(Configuration.VERSION_2_3_0, true));
            assertEquals(1, getBeansWrapperInstanceCacheSize());
            
            BeansWrapperBuilder factory = new BeansWrapperBuilder(new Version(2, 3, 5));
            factory.setSimpleMapWrapper(true);
            assertSame(bw, factory.build());
            assertEquals(1, getBeansWrapperInstanceCacheSize());
            
            hardReferences.add(bw);            
        }
        
        {
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_21, true);
            assertEquals(2, getBeansWrapperInstanceCacheSize());
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            assertTrue(bw.isClassIntrospectionCacheRestricted());
            assertNull(bw.getMethodAppearanceFineTuner());
            assertNull(bw.getMethodSorter());
            
            BeansWrapperBuilder factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
            factory.setSimpleMapWrapper(true);
            assertSame(bw, factory.build());
            assertEquals(2, getBeansWrapperInstanceCacheSize());
            
            hardReferences.add(bw);            
        }
        
        {
            // Again... same as the very first
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_19, true);
            assertEquals(2, getBeansWrapperInstanceCacheSize());
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
        }
        
        {
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_19, false);
            assertEquals(3, getBeansWrapperInstanceCacheSize());
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            assertSame(bw, getBeansWrapperWithSetting(Configuration.VERSION_2_3_20, false));
            assertSame(bw, getBeansWrapperWithSetting(Configuration.VERSION_2_3_0, false));
            assertSame(bw, new BeansWrapperBuilder(new Version(2, 3, 5)).build());
            assertEquals(3, getBeansWrapperInstanceCacheSize());
            
            hardReferences.add(bw);            
        }
        
        {
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_21, false);
            assertEquals(4, getBeansWrapperInstanceCacheSize());
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            assertSame(bw, new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build());
            assertEquals(4, getBeansWrapperInstanceCacheSize());
            
            hardReferences.add(bw);            
        }

        {
            // Again... same as earlier
            BeansWrapper bw = getBeansWrapperWithSetting(Configuration.VERSION_2_3_21, true);
            assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isSimpleMapWrapper());
            assertEquals(4, getBeansWrapperInstanceCacheSize());
        }
        
        {
            BeansWrapperBuilder factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = factory.build();
            BeansWrapper bw2 = factory.build();
            assertEquals(5, getBeansWrapperInstanceCacheSize());
            assertSame(bw, bw2);
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
            assertFalse(bw.isStrict());
            assertEquals(TemplateDateModel.UNKNOWN, bw.getDefaultDateType());
            assertSame(bw, bw.getOuterIdentity());
            
            hardReferences.add(bw);            
        }
        
        {
            BeansWrapperBuilder factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setExposeFields(true);
            BeansWrapper bw = factory.build();
            BeansWrapper bw2 = factory.build();
            assertEquals(6, getBeansWrapperInstanceCacheSize());
            assertSame(bw, bw2);
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertTrue(bw.isExposeFields());
            
            hardReferences.add(bw);            
        }
        
        {
            BeansWrapperBuilder factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setStrict(true);
            factory.setDefaultDateType(TemplateDateModel.DATETIME);
            factory.setOuterIdentity(new SimpleObjectWrapper());
            BeansWrapper bw = factory.build();
            assertEquals(7, getBeansWrapperInstanceCacheSize());
            assertTrue(bw.isStrict());
            assertEquals(TemplateDateModel.DATETIME, bw.getDefaultDateType());
            assertSame(SimpleObjectWrapper.class, bw.getOuterIdentity().getClass());
            
            hardReferences.add(bw);            
        }
        
        // Effect of reference and cache clearings:
        {
            BeansWrapper bw1 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
            assertEquals(7, getBeansWrapperInstanceCacheSize());
            assertEquals(7, getBeansWrapperNonClearedInstanceCacheSize());
            
            clearBeansWrapperInstanceCacheReferences(false);
            assertEquals(7, getBeansWrapperInstanceCacheSize());
            assertEquals(0, getBeansWrapperNonClearedInstanceCacheSize());
            
            BeansWrapper bw2 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
            assertNotSame(bw1, bw2);
            assertEquals(7, getBeansWrapperInstanceCacheSize());
            assertEquals(1, getBeansWrapperNonClearedInstanceCacheSize());
            
            assertSame(bw2, new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build());
            assertEquals(1, getBeansWrapperNonClearedInstanceCacheSize());
            
            clearBeansWrapperInstanceCacheReferences(true);
            BeansWrapper bw3 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
            assertNotSame(bw2, bw3);
            assertEquals(1, getBeansWrapperInstanceCacheSize());
            assertEquals(1, getBeansWrapperNonClearedInstanceCacheSize());
        }

        {
            BeansWrapperBuilder factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setUseModelCache(true);
            BeansWrapper bw = factory.build();
            assertTrue(bw.getUseCache());
            assertEquals(2, getBeansWrapperInstanceCacheSize());
            
            hardReferences.add(bw);            
        }
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    private BeansWrapper getBeansWrapperWithSetting(Version ici, boolean simpleMapWrapper) {
        BeansWrapperBuilder f = new BeansWrapperBuilder(ici);
        f.setSimpleMapWrapper(simpleMapWrapper);
        return f.build();
    }

    public void testMultipleTCCLs() {
        List<BeansWrapper> hardReferences = new LinkedList<BeansWrapper>();
        
        assertEquals(0, getBeansWrapperInstanceCacheSize());
        
        BeansWrapper bw1 = new BeansWrapperBuilder(Configuration.VERSION_2_3_19).build();
        assertEquals(1, getBeansWrapperInstanceCacheSize());
        hardReferences.add(bw1);
        
        ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        // Doesn't mater what, just be different from oldTCCL: 
        ClassLoader newTCCL = oldTCCL == null ? this.getClass().getClassLoader() : null;
        
        BeansWrapper bw2;
        Thread.currentThread().setContextClassLoader(newTCCL);
        try {
            bw2 = new BeansWrapperBuilder(Configuration.VERSION_2_3_19).build();
            assertEquals(2, getBeansWrapperInstanceCacheSize());
            hardReferences.add(bw2);
            
            assertNotSame(bw1, bw2);
            assertSame(bw2, new BeansWrapperBuilder(Configuration.VERSION_2_3_19).build());
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
        
        assertSame(bw1, new BeansWrapperBuilder(Configuration.VERSION_2_3_19).build());
        assertEquals(2, getBeansWrapperInstanceCacheSize());

        BeansWrapper bw3;
        Thread.currentThread().setContextClassLoader(newTCCL);
        try {
            assertSame(bw2, new BeansWrapperBuilder(Configuration.VERSION_2_3_19).build());
            
            bw3 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
            assertEquals(3, getBeansWrapperInstanceCacheSize());
            hardReferences.add(bw3);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
        
        BeansWrapper bw4 = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
        assertEquals(4, getBeansWrapperInstanceCacheSize());
        hardReferences.add(bw4);
        
        assertNotSame(bw3, bw4);
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    public void testClassInrospectorCache() throws TemplateModelException {
        assertFalse(new BeansWrapper().isClassIntrospectionCacheRestricted());
        assertFalse(new BeansWrapper(Configuration.VERSION_2_3_21).isClassIntrospectionCacheRestricted());
        assertTrue(new BeansWrapperBuilder(Configuration.VERSION_2_3_20).build().isClassIntrospectionCacheRestricted());
        
        ClassIntrospectorBuilder.clearInstanceCache();
        BeansWrapperBuilder.clearInstanceCache();
        checkClassIntrospectorCacheSize(0);
        
        List<BeansWrapper> hardReferences = new LinkedList<BeansWrapper>();
        BeansWrapperBuilder factory;
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            
            BeansWrapper bw1 = factory.build();
            checkClassIntrospectorCacheSize(1);
            
            factory.setExposureLevel(BeansWrapper.EXPOSE_SAFE);  // this was already set to this
            factory.setSimpleMapWrapper(true);  // this shouldn't matter for the introspection cache
            BeansWrapper bw2 = factory.build();
            checkClassIntrospectorCacheSize(1);
            
            assertSame(bw2.getClassIntrospector(), bw1.getClassIntrospector());
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertTrue(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));
            assertFalse(isSimpleMapWrapper(bw1));
            assertTrue(bw1.isClassIntrospectionCacheRestricted());
            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setExposeFields(true);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(2);
            // Wrapping tests:
            assertTrue(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertTrue(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }

        {
            factory.setExposureLevel(BeansWrapper.EXPOSE_ALL);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(3);
            // Wrapping tests:
            assertTrue(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertTrue(exposesMethods(bw));
            assertTrue(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }
        
        {
            factory.setExposeFields(false);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(4);
            // Wrapping tests:
            assertFalse(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertTrue(exposesMethods(bw));
            assertTrue(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }
        
        {
            factory.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(5);
            // Wrapping tests:
            assertFalse(exposesFields(bw));
            assertFalse(exposesProperties(bw));
            assertFalse(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }

        {
            factory.setExposeFields(true);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(6);
            // Wrapping tests:
            assertTrue(exposesFields(bw));
            assertFalse(exposesProperties(bw));
            assertFalse(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }

        {
            factory.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(7);
            // Wrapping tests:
            assertTrue(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertFalse(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            assertFalse(isSimpleMapWrapper(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
            factory.setExposeFields(false);
            factory.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            
            BeansWrapper bw1 = factory.build();
            checkClassIntrospectorCacheSize(8);
            ClassIntrospector ci1 = bw1.getClassIntrospector();
            
            factory.setSimpleMapWrapper(true);  // Shouldn't mater
            BeansWrapper bw2 = factory.build();
            ClassIntrospector ci2 = bw2.getClassIntrospector();
            checkClassIntrospectorCacheSize(8);
            
            assertSame(ci1, ci2);
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertFalse(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));
            assertFalse(isSimpleMapWrapper(bw1));
            
            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }
        
        BeansWrapperBuilder.clearInstanceCache();  // otherwise ClassInrospector cache couldn't become cleared in reality
        _TemplateAPI.DefaultObjectWrapperFactory_clearInstanceCache();
        clearClassIntrospectorInstanceCacheReferences(false);
        checkClassIntrospectorCacheSize(8);
        assertEquals(0, getClassIntrospectorNonClearedInstanceCacheSize());

        {
            factory.setSimpleMapWrapper(false);
            factory.setExposeFields(false);
            
            BeansWrapper bw1 = factory.build();
            checkClassIntrospectorCacheSize(8);
            assertEquals(1, getClassIntrospectorNonClearedInstanceCacheSize());
            ClassIntrospector ci1 = bw1.getClassIntrospector();
            
            factory.setSimpleMapWrapper(true);  // Shouldn't mater
            BeansWrapper bw2 = factory.build();
            ClassIntrospector ci2 = bw2.getClassIntrospector();
            
            assertSame(ci1, ci2);
            assertNotSame(bw1, bw2);
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertTrue(exposesProperties(bw1));
            assertFalse(exposesMethods(bw1));
            assertFalse(exposesUnsafe(bw1));
            assertFalse(isSimpleMapWrapper(bw1));
            
            // Prevent introspection cache GC:
            hardReferences.add(bw1);
            hardReferences.add(bw2);
        }
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(8);
            assertEquals(2, getClassIntrospectorNonClearedInstanceCacheSize());
            // Wrapping tests:
            assertFalse(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertTrue(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }

        clearClassIntrospectorInstanceCacheReferences(true);
        checkClassIntrospectorCacheSize(8);
        assertEquals(0, getClassIntrospectorNonClearedInstanceCacheSize());
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_21);
            factory.setExposeFields(true);
            BeansWrapper bw = factory.build();
            checkClassIntrospectorCacheSize(1);
            // Wrapping tests:
            assertTrue(exposesFields(bw));
            assertTrue(exposesProperties(bw));
            assertTrue(exposesMethods(bw));
            assertFalse(exposesUnsafe(bw));
            // Prevent introspection cache GC:
            hardReferences.add(bw);
        }
        
        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setMethodAppearanceFineTuner(new MethodAppearanceFineTuner() {
                public void process(MethodAppearanceDecisionInput in, MethodAppearanceDecision out) {
                }
            });  // spoils ClassIntrospector() sharing
            
            BeansWrapper bw1 = factory.build();
            assertSame(bw1, factory.build());
            
            factory.setSimpleMapWrapper(true);
            BeansWrapper bw2 = factory.build();
            checkClassIntrospectorCacheSize(1);
            assertNotSame(bw1, bw2);
            assertNotSame(bw1.getClassIntrospector(), bw2.getClassIntrospector());
            assertTrue(bw1.isClassIntrospectionCacheRestricted());
            assertTrue(bw2.isClassIntrospectionCacheRestricted());
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertFalse(exposesFields(bw2));
            assertTrue(exposesProperties(bw1));
            assertTrue(exposesProperties(bw2));
            assertTrue(exposesMethods(bw1));
            assertTrue(exposesMethods(bw2));
            assertFalse(exposesUnsafe(bw1));
            assertFalse(exposesUnsafe(bw2));
            assertFalse(isSimpleMapWrapper(bw1));
            assertTrue(isSimpleMapWrapper(bw2));
        }

        {
            factory = new BeansWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setMethodAppearanceFineTuner(GetlessMethodsAsPropertyGettersRule.INSTANCE);  // doesn't spoils sharing
            
            BeansWrapper bw1 = factory.build();
            assertSame(bw1, factory.build());
            checkClassIntrospectorCacheSize(2);
            
            factory.setSimpleMapWrapper(true);
            BeansWrapper bw2 = factory.build();
            checkClassIntrospectorCacheSize(2);
            
            assertNotSame(bw1, bw2);
            assertSame(bw1.getClassIntrospector(), bw2.getClassIntrospector());  // !
            assertTrue(bw1.isClassIntrospectionCacheRestricted());
            assertTrue(bw2.isClassIntrospectionCacheRestricted());
            
            // Wrapping tests:
            assertFalse(exposesFields(bw1));
            assertFalse(exposesFields(bw2));
            assertTrue(exposesProperties(bw1));
            assertTrue(exposesProperties(bw2));
            assertTrue(exposesMethods(bw1));
            assertTrue(exposesMethods(bw2));
            assertFalse(exposesUnsafe(bw1));
            assertFalse(exposesUnsafe(bw2));
            assertFalse(isSimpleMapWrapper(bw1));
            assertTrue(isSimpleMapWrapper(bw2));
        }
        
        assertTrue(hardReferences.size() != 0);  // just to save it from GC until this line        
    }
    
    private void checkClassIntrospectorCacheSize(int expectedSize) {
        assertEquals(expectedSize, getClassIntrospectorInstanceCacheSize());
    }

    private void assertNotEquals(Object o1, Object o2) {
        assertFalse(o1.equals(o2));
    }
    
    public class C {
        
        public String foo = "FOO";
        
        public String getBar() {
            return "BAR";
        }
        
    }

    private boolean isSimpleMapWrapper(BeansWrapper bw) throws TemplateModelException {
        return bw.wrap(new HashMap()) instanceof SimpleMapModel;
    }
    
    private boolean exposesFields(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("foo");
        if (r == null) return false;
        assertEquals("FOO", r.getAsString());
        return true;
    }

    private boolean exposesProperties(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("bar");
        if (r == null) return false;
        assertEquals("BAR", r.getAsString());
        return true;
    }

    private boolean exposesMethods(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        return thm.get("getBar") != null;
    }

    private boolean exposesUnsafe(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        return thm.get("wait") != null;
    }
    
    static int getClassIntrospectorInstanceCacheSize() {
        Map instanceCache = ClassIntrospectorBuilder.getInstanceCache();
        synchronized (instanceCache) {
            return instanceCache.size();
        }
    }

    static int getClassIntrospectorNonClearedInstanceCacheSize() {
        Map instanceCache = ClassIntrospectorBuilder.getInstanceCache();
        synchronized (instanceCache) {
            int cnt = 0;
            for (Iterator it = instanceCache.values().iterator(); it.hasNext(); ) {
                if (((Reference) it.next()).get() != null) cnt++;
            }
            return cnt;
        }
    }
    
    static void clearClassIntrospectorInstanceCacheReferences(boolean enqueue) {
        Map instanceCache = ClassIntrospectorBuilder.getInstanceCache();
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

    static int getBeansWrapperInstanceCacheSize() {
        Map instanceCache = BeansWrapperBuilder.getInstanceCache();
        synchronized (instanceCache) {
            int size = 0; 
            for (Iterator it1 = instanceCache.values().iterator(); it1.hasNext(); ) {
                size += ((Map) it1.next()).size();
            }
            return size;
        }
    }

    static int getBeansWrapperNonClearedInstanceCacheSize() {
        Map instanceCache = BeansWrapperBuilder.getInstanceCache();
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
    
    static void clearBeansWrapperInstanceCacheReferences(boolean enqueue) {
        Map instanceCache = BeansWrapperBuilder.getInstanceCache();
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
