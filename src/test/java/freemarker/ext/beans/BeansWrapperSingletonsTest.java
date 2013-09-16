package freemarker.ext.beans;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapper.SettingAssignments;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;

public class BeansWrapperSingletonsTest extends TestCase {

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
        SettingAssignments sa1 = new SettingAssignments();
        SettingAssignments sa2 = new SettingAssignments();
        assertEquals(sa1, sa2);
        assertEquals(sa1, SettingAssignments.DEFAULT);
        assertEquals(sa1, SettingAssignments.SIMPLE_MAP_WRAPPER_FALSE);
        
        sa1.setSimpleMapWrapper(true);
        assertNotEquals(sa1, sa2);
        sa2.setSimpleMapWrapper(true);
        assertEquals(sa1, sa2);
        assertEquals(sa1, SettingAssignments.SIMPLE_MAP_WRAPPER_TRUE);
        
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
    }
    
    public void testBeansWrapperSingletons() throws Exception {
        BeansWrapper.clearSharedStateForUnitTesting();
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 19), true);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 20), true));
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 0), true));
            
            SettingAssignments sa = new SettingAssignments();
            sa.setSimpleMapWrapper(true);
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 5), sa));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 21), true);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapModel);
            
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 22), true));
            
            SettingAssignments sa = new SettingAssignments();
            sa.setSimpleMapWrapper(true);
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 21), sa));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 19), true);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 19), false);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 20), false));
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 0), false));
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 5), new SettingAssignments()));
        }
        
        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 21), false);
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 22), false));
            assertSame(bw, BeansWrapper.getInstance(new Version(2, 3, 21), new SettingAssignments()));
        }

        {
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 21), true);
            assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
        }
        
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
            BeansWrapper bw2 = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 19), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
        }
        
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setExposeFields(true);
            BeansWrapper bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
            BeansWrapper bw2 = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), BeansWrapper.class);
            assertEquals(new Version(2, 3, 19), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof MapModel);
            assertEquals(true, bw.isExposeFields());
        }
    }

    public void testDefaultObjectWrapperSingletons() throws Exception {
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setSimpleMapWrapper(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa);
            assertNotSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 19), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
        }
        
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setSimpleMapWrapper(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 22), sa);
            assertNotSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 22), sa));
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 22), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
        }
        
        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), new SettingAssignments());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            
            assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 20), new SettingAssignments()));
            assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 0), new SettingAssignments()));
            assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 5), new SettingAssignments()));
        }
        
        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 21), new SettingAssignments());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            
            assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 22), new SettingAssignments()));
            assertSame(bw, DefaultObjectWrapper.getInstance(new Version(2, 3, 21), new SettingAssignments()));
        }

        {
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), new SettingAssignments());
            assertEquals(new Version(2, 3, 0), bw.getIncompatibleImprovements());
        }
        
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa);
            BeansWrapper bw2 = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 19), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
        }
        
        {
            SettingAssignments sa = new SettingAssignments();
            sa.setExposeFields(true);
            BeansWrapper bw = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa);
            BeansWrapper bw2 = DefaultObjectWrapper.getInstance(new Version(2, 3, 19), sa);
            assertNotSame(bw, bw2);  // not cached
            
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(new Version(2, 3, 19), bw.getIncompatibleImprovements());
            assertTrue(bw.isReadOnly());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(true, bw.isExposeFields());
            
            try {
                DefaultObjectWrapper.getInstance(new Version(2, 3, 19), false);
                fail();
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                DefaultObjectWrapper.getInstance(new Version(2, 3, 19), true);
                fail();
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
        
    }
    
    public void testInrospectionCacheCache() throws TemplateModelException {
        SettingAssignments sa;
        
        BeansWrapper.clearSharedStateForUnitTesting();
        checkIntrospectionCacheCachePattern(null);
        
        BeansWrapper bw;
        List<BeansWrapper> hardReferences = new LinkedList<BeansWrapper>();
        
        sa = new SettingAssignments();
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        sa.setExposureLevel(BeansWrapper.EXPOSE_SAFE);  // this was alrady set to this
        sa.setSimpleMapWrapper(true);  // this shouldn't matter for the introspection cache
        BeansWrapper.getInstance(new Version(2, 3, 19), sa).checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        checkIntrospectionCacheCachePattern("S");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa = new SettingAssignments();
        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(1);
        checkIntrospectionCacheCachePattern("SW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(3);
        checkIntrospectionCacheCachePattern("SWnW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertTrue(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposeFields(false);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(2);
        checkIntrospectionCacheCachePattern("SWWW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertTrue(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(6);
        checkIntrospectionCacheCachePattern("SWWWnnW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertFalse(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);

        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(7);
        checkIntrospectionCacheCachePattern("SWWWnnWW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertFalse(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(5);
        checkIntrospectionCacheCachePattern("SWWWnWWW");
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertFalse(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setSimpleMapWrapper(false);
        sa.setExposeFields(false);
        BeansWrapper bw1 = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
        checkIntrospectionCacheCachePattern("SWWWWWWW");
        bw1.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc1 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        sa.setSimpleMapWrapper(true);  // Shouldn't mater
        BeansWrapper bw2 = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
        bw2.checkIfUsesSharedIntrospectionCacheForUnitTesting(4);
        Reference isc2 = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting()[4];
        assertSame(isc1, isc2);
        // Wrapping tests:
        assertFalse(hasFoo(bw1));
        assertTrue(hasBar(bw1));
        assertFalse(hasGetBar(bw1));
        assertFalse(hasWait(bw1));
        // Prevent introspection cache GC:
        hardReferences.add(bw1);
        hardReferences.add(bw2);
        
        Reference[] cc = BeansWrapper.getIntrospectionCacheCacheSnapshotForUnitTesting();
        for (Reference ref : cc) {
            ref.clear();
        }

        sa.setSimpleMapWrapper(false);
        sa.setExposeFields(false);
        bw1 = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
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
        assertFalse(hasFoo(bw1));
        assertTrue(hasBar(bw1));
        assertFalse(hasGetBar(bw1));
        assertFalse(hasWait(bw1));
        // Prevent introspection cache GC:
        hardReferences.add(bw1);
        hardReferences.add(bw2);
        
        sa = new SettingAssignments();
        bw = BeansWrapper.getInstance(new Version(2, 3, 19), sa);
        bw.checkIfUsesSharedIntrospectionCacheForUnitTesting(0);
        checkIntrospectionCacheCachePattern("SWWWWWWW");
        // Wrapping tests:
        assertFalse(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
        
        sa.setExposeFields(true);
        bw = BeansWrapper.getInstance(new Version(2, 3, 21), sa);
        // Wrapping tests:
        assertTrue(hasFoo(bw));
        assertTrue(hasBar(bw));
        assertTrue(hasGetBar(bw));
        assertFalse(hasWait(bw));
        // Prevent introspection cache GC:
        hardReferences.add(bw);
    }
    
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

    private void assertNotEquals(Object o1, Object o2) {
        assertFalse(o1.equals(o2));
    }
    
    public class C {
        
        public String foo = "FOO";
        
        public String getBar() {
            return "BAR";
        }
        
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
