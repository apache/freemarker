package freemarker.ext.beans;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.SettingAssignments;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;

public class BeansWrapperSingletonsTest extends TestCase {

    private static final Version V_2_3_0 = new Version(2, 3, 0);
    private static final Version V_2_3_19 = new Version(2, 3, 19);
    private static final Version V_2_3_21 = new Version(2, 3, 21);

    public BeansWrapperSingletonsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testBeansWrapperSettingAssignments() throws Exception {
        assertEquals(V_2_3_21, new SettingAssignments(V_2_3_21).getIncompatibleImprovements());
        assertEquals(new Version(2, 3, 0), new SettingAssignments(new Version(2, 3, 20)).getIncompatibleImprovements());
        try {
            new SettingAssignments(new Version(2, 3, 22));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("upgrade"));
        }

        SettingAssignments sa1;
        SettingAssignments sa2;
        
        sa1 = new SettingAssignments(V_2_3_21);
        sa2 = new SettingAssignments(V_2_3_21);
        assertEquals(sa1, sa2);
        
        sa1.setSimpleMapWrapper(true);
        assertNotEquals(sa1, sa2);
        sa2.setSimpleMapWrapper(true);
        assertEquals(sa1, sa2);
        
        sa1.setExposeFields(true);
        assertNotEquals(sa1, sa2);
        sa2.setExposeFields(true);
        assertEquals(sa1, sa2);
        
        sa1.setExposureLevel(0);
        assertNotEquals(sa1, sa2);
        sa2.setExposureLevel(0);
        assertEquals(sa1, sa2);
        
        sa1.setExposureLevel(1);
        assertNotEquals(sa1, sa2);
        sa2.setExposureLevel(1);
        assertEquals(sa1, sa2);
        
        sa1.setDefaultDateType(TemplateDateModel.DATE);
        assertNotEquals(sa1, sa2);
        sa2.setDefaultDateType(TemplateDateModel.DATE);
        assertEquals(sa1, sa2);
        
        sa1.setStrict(true);
        assertNotEquals(sa1, sa2);
        sa2.setStrict(true);
        assertEquals(sa1, sa2);
        
        AlphabeticalMethodShorter ms = new AlphabeticalMethodShorter(true);
        sa1.setMethodShorter(ms);
        assertNotEquals(sa1, sa2);
        sa2.setMethodShorter(ms);
        assertEquals(sa1, sa2);
        
        MethodAppearanceFineTuner maft = new MethodAppearanceFineTuner() {
            public void fineTuneMethodAppearance(Class clazz, Method m, MethodAppearanceDecision decision) { }
        };
        sa1.setMethodAppearanceFineTuner(maft);
        assertNotEquals(sa1, sa2);
        sa2.setMethodAppearanceFineTuner(maft);
        assertEquals(sa1, sa2);
    }
    
    public void testBeansWrapperSingletons() throws Exception {
        //TODO BeansWrapper.clearSharedStateForUnitTesting();
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_19, true);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            assertFalse(bw.isStrict());
            assertEquals(TemplateDateModel.UNKNOWN, bw.getDefaultDateType());
            assertSame(bw, bw.getOuterIdentity());
            assertTrue(bw.isClassIntrospectionCacheShared());
            assertNull(bw.getMethodAppearanceFineTuner());
            assertNull(bw.getMethodShorter());
            
            try {
                bw.setExposeFields(true);  // can't modify the settings of a (potential) singleton
                fail();
            } catch (IllegalStateException e) {
                assertTrue(e.getMessage().contains("modify"));
            }
            
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 20), true));
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 0), true));
            
            //TODO SettingAssignments sa = new SettingAssignments(new Version(2, 3, 5));
            //TODO sa.setSimpleMapWrapper(true);
            //TODO assertSame(bw, BeansWrapper.getInstance(sa));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_21, true);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(V_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            assertTrue(bw.isClassIntrospectionCacheShared());
            assertNull(bw.getMethodAppearanceFineTuner());
            assertNull(bw.getMethodShorter());
            
            //TODO assertSame(bw, BeansWrapper.getInstance(Configuration.getVersion(), true));
            
            //TODO SettingAssignments sa = new SettingAssignments(V_2_3_21);
            //TODO sa.setSimpleMapWrapper(true);
            //TODO assertSame(bw, BeansWrapper.getInstance(sa));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_19, true);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_19, false);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 20), false));
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 0), false));
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 5), new SettingAssignments()));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_21, false);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(V_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            //TODO assertSame(bw, BeansWrapper.getInstance(Configuration.getVersion(), false));
            //TODO assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 21), new SettingAssignments()));
        }

        {
            BeansWrapper bw = BeansWrapper.getInstance(V_2_3_21, true);
            assertEquals(V_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isSimpleMapWrapper());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = BeansWrapper.getInstance(sa);
            BeansWrapper bw2 = BeansWrapper.getInstance(sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(V_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
            assertFalse(bw.isStrict());
            assertEquals(TemplateDateModel.UNKNOWN, bw.getDefaultDateType());
            assertSame(bw, bw.getOuterIdentity());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setExposeFields(true);
            BeansWrapper bw = BeansWrapper.getInstance(sa);
            //TODO BeansWrapper bw2 = BeansWrapper.getInstance(sa);
            //TODO assertSame(bw, bw2);
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(V_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertTrue(bw.isExposeFields());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setStrict(true);
            sa.setDefaultDateType(TemplateDateModel.DATETIME);
            sa.setOuterIdentity(new SimpleObjectWrapper());
            BeansWrapper bw = BeansWrapper.getInstance(sa);
            assertTrue(bw.isStrict());
            assertEquals(TemplateDateModel.DATETIME, bw.getDefaultDateType());
            assertSame(SimpleObjectWrapper.class, bw.getOuterIdentity().getClass());
        }
    }

    public void testDefaultObjectWrapperSingletons() throws Exception {
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setSimpleMapWrapper(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(sa);
            assertNotSame(bw, DefaultObjectWrapper.getInstance(sa));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(V_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.isClassIntrospectionCacheShared());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(Configuration.getVersion());
            sa.setSimpleMapWrapper(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(sa);
            assertNotSame(bw, DefaultObjectWrapper.getInstance(sa));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(
                    BeansWrapper.normalizeIncompatibleImprovementsVersion(Configuration.getVersion()),
                    bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
        }
        
        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new SettingAssignments(V_2_3_19));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            
            //TODO assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 20), new SettingAssignments()));
            //TODO assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 0), new SettingAssignments()));
            //TODO assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 5), new SettingAssignments()));
        }
        
        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new SettingAssignments(V_2_3_21));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(V_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.isClassIntrospectionCacheShared());
            
            //TODO assertSame(bw, DefaultObjectWrapper.getInstance(Configuration.getVersion(), new SettingAssignments()));
            //TODO assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 21), new SettingAssignments()));
        }

        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new SettingAssignments(V_2_3_19));
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(sa);
            BeansWrapper bw2 = DefaultObjectWrapper.getInstance(sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(V_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
        }
        
        {
            SettingAssignments sa = new SettingAssignments(V_2_3_19);
            sa.setExposeFields(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(sa);
            BeansWrapper bw2 = DefaultObjectWrapper.getInstance(sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(V_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(true, bw.isExposeFields());
            
            try {
                DefaultObjectWrapper.getInstance(V_2_3_19, false);
                fail();
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                DefaultObjectWrapper.getInstance(V_2_3_19, true);
                fail();
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }
    
    public void testInrospectionCacheCache() throws TemplateModelException {
        SettingAssignments sa;

        assertFalse(new BeansWrapper().isClassIntrospectionCacheShared());
        assertFalse(new BeansWrapper(new Version(2, 3, 21)).isClassIntrospectionCacheShared());
        assertTrue(BeansWrapper.getInstance(new Version(2, 3, 20)).isClassIntrospectionCacheShared());
        
        //TODO BeansWrapper.clearSharedStateForUnitTesting();
        //TODO checkIntrospectionCacheCachePattern(null);
        
        BeansWrapper bw;
        List<BeansWrapper> hardReferences = new LinkedList<BeansWrapper>();
        
        sa = new SettingAssignments(V_2_3_19);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        //sa.setExposureLevel(BeansWrapper.EXPOSE_SAFE);  // this was already set to this
        //sa.setSimpleMapWrapper(true);  // this shouldn't matter for the introspection cache
        //TODO BeansWrapper.getInstance(sa).checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        //TODO checkIntrospectionCacheCachePattern("S");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        assertTrue(bw.isClassIntrospectionCacheShared());
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa = new SettingAssignments(V_2_3_19);
        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(1);
        //TODO checkIntrospectionCacheCachePattern("SW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(3);
        //TODO checkIntrospectionCacheCachePattern("SWnW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertTrue(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposeFields(false);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(2);
        //TODO checkIntrospectionCacheCachePattern("SWWW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertTrue(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(6);
        //TODO checkIntrospectionCacheCachePattern("SWWWnnW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertFalse(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(7);
        //TODO checkIntrospectionCacheCachePattern("SWWWnnWW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertFalse(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(5);
        //TODO checkIntrospectionCacheCachePattern("SWWWnWWW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa = new SettingAssignments(V_2_3_21);
        sa.setExposeFields(false);
        sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        BeansWrapper bw1 = BeansWrapper.getInstance(sa);
        /*
        checkIntrospectionCacheCachePattern("SWWWWWWW");
        bw1.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc1 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        sa.setSimpleMapWrapper(true);  // Shouldn't mater
        BeansWrapper bw2 = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
        bw2.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc2 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        assertSame(isc1, isc2);
        */
        // Wrapping tests:
        assertFalse(hasFoo(bw1));
        assertTrue(hasBar(bw1));
        assertFalse(hasGetBar(bw1));
        assertFalse(hasWait(bw1));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw1);
        /*
        hardReferences.add(bw2);
                
        Reference[] cc = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting();
        for (Reference ref : cc) {
            ref.clear();
        }
        */

        sa.setSimpleMapWrapper(false);
        sa.setExposeFields(false);
        bw1 = BeansWrapper.getInstance(sa);
        /*
        checkIntrospectionCacheCachePattern("SWWWWWWW");
        bw1.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc1r2 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        sa.setSimpleMapWrapper(true);  // Shouldn't mater
        bw2 = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
        bw2.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc2r2 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        assertSame(isc1r2, isc2r2);
        assertNotSame(isc1, isc2r2);  // the Reference was re-created as it was cleared 
        // Wrapping tests:
        */
        assertFalse(hasFoo(bw1));
        assertTrue(hasBar(bw1));
        assertFalse(hasGetBar(bw1));
        assertFalse(hasWait(bw1));
        assertFalse(isSimpleMapWrapper(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw1);
        /*
        hardReferences.add(bw2);
        */
        
        sa = new SettingAssignments(V_2_3_19);
        bw = BeansWrapper.getInstance(sa);
        //TODO bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        //TODO checkIntrospectionCacheCachePattern("SWWWWWWW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa = new SettingAssignments(V_2_3_21);
        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(sa);
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
    }
    
    /*
    private void checkIntrospectionCacheCachePattern(String pattern) {
        Reference[] cc = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting();
        if (pattern == null) {
            assertNull(cc);
        } else {
            assertNotNull(cc);
            
            StringBuilder sb = new StringBuilder(cc.length);
            sb.setLength(cc.length);
            for (int i = 0; i < cc.length; i++) {
                Reference ref = cc[i];
                sb.setCharAt(i,
                        ref == null
                            ? 'n'
                            : (ref instanceof WeakReference
                                    ? 'W'
                                    : (ref instanceof SoftReference ? 'S' : '?')));
            }
            assertEquals(pattern, sb.toString());
        }
    }
    */

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
    
    private boolean hasFoo(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("foo");
        if (r == null) return false;
        assertEquals("FOO", r.getAsString());
        return true;
    }

    private boolean hasBar(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        TemplateScalarModel r = (TemplateScalarModel) thm.get("bar");
        if (r == null) return false;
        assertEquals("BAR", r.getAsString());
        return true;
    }

    private boolean hasGetBar(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        return thm.get("getBar") != null;
    }

    private boolean hasWait(BeansWrapper bw) throws TemplateModelException {
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new C());
        return thm.get("wait") != null;
    }
    
}
