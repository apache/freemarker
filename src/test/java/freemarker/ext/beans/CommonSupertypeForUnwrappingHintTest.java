package freemarker.ext.beans;

import java.io.Serializable;
import java.util.List;

import junit.framework.TestCase;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

public class CommonSupertypeForUnwrappingHintTest extends TestCase {
    
    final OverloadedMethodsSubset buggy
            = new DummyOverloadedMethodsSubset(new BeansWrapper(new Version(2, 3, 20)).is2321Bugfixed());
    final OverloadedMethodsSubset fixed
            = new DummyOverloadedMethodsSubset(new BeansWrapper(new Version(2, 3, 21)).is2321Bugfixed());

    public CommonSupertypeForUnwrappingHintTest(String name) {
        super(name);
    }

    public void testSame() {
        testSame(buggy);
        testSame(fixed);
    }

    /** These will be the same with fixed and buggy: */
    private void testSame(OverloadedMethodsSubset oms) {
        assertEquals(char.class, oms.getCommonSupertypeForUnwrappingHint(char.class, char.class));
        assertEquals(Integer.class, oms.getCommonSupertypeForUnwrappingHint(Integer.class, Integer.class));
        assertEquals(String.class, oms.getCommonSupertypeForUnwrappingHint(String.class, String.class));
    }

    public void testInterfaces() {
        testInterfaces(fixed);
        testInterfaces(buggy);
    }

    /** These will be the same with fixed and buggy: */
    private void testInterfaces(OverloadedMethodsSubset oms) {
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(String.class, Number.class));
        assertEquals(C1I1.class, oms.getCommonSupertypeForUnwrappingHint(C2ExtC1I1.class, C3ExtC1I1.class));
        assertEquals(Object.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, C4I1I2.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, C5I1.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, I1.class));
        assertEquals(I2.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, I2.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(I1I2.class, I1.class));
        assertEquals(I2.class, oms.getCommonSupertypeForUnwrappingHint(I1I2.class, I2.class));
    }
    
    public void testBuggyInterfaces() {
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(String.class, StringBuilder.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(C7ExtC6I1.class, C8ExtC6I1.class));
    }
    
    public void testFixedInterfaces() {
        assertEquals(CharSequence.class, fixed.getCommonSupertypeForUnwrappingHint(String.class, StringBuilder.class));
        assertEquals(C6.class, fixed.getCommonSupertypeForUnwrappingHint(C7ExtC6I1.class, C8ExtC6I1.class));
    }
    
    public void testFixedPrimitive() {
        assertEquals(Integer.class, fixed.getCommonSupertypeForUnwrappingHint(int.class, Integer.class));
        assertEquals(Integer.class, fixed.getCommonSupertypeForUnwrappingHint(Integer.class, int.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(int.class, Long.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(Long.class, int.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(Integer.class, long.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(long.class, Integer.class));
        assertEquals(Boolean.class, fixed.getCommonSupertypeForUnwrappingHint(boolean.class, Boolean.class));
        assertEquals(Boolean.class, fixed.getCommonSupertypeForUnwrappingHint(Boolean.class, boolean.class));
        assertEquals(Character.class, fixed.getCommonSupertypeForUnwrappingHint(char.class, Character.class));
        assertEquals(Character.class, fixed.getCommonSupertypeForUnwrappingHint(Character.class, char.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(int.class, short.class));
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(short.class, int.class));
    }

    public void testBuggyPrimitive() {
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(int.class, Integer.class));
        assertEquals(Integer.class, buggy.getCommonSupertypeForUnwrappingHint(Integer.class, int.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(int.class, Long.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(Long.class, int.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(Integer.class, long.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(long.class, Integer.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(boolean.class, Boolean.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(Boolean.class, boolean.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(char.class, Character.class));
        assertEquals(Character.class, buggy.getCommonSupertypeForUnwrappingHint(Character.class, char.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(int.class, short.class));
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(short.class, int.class));
    }

    public void testFixedMisc() {
        assertEquals(Number.class, fixed.getCommonSupertypeForUnwrappingHint(Long.class, Integer.class));
    }
    
    public void testBuggyMisc() {
        assertEquals(Object.class, buggy.getCommonSupertypeForUnwrappingHint(Long.class, Integer.class));
    }
    
    static interface I1 { };
    static class C1I1 implements I1 { };
    static class C2ExtC1I1 extends C1I1 { };
    static class C3ExtC1I1 extends C1I1 { };
    static interface I2 { };
    static class C3I1I2 implements I1, I2 { };
    static class C4I1I2 implements I1, I2 { };
    static class C5I1 implements I1 { };
    static interface I1I2 extends I1, I2 { };
    static class C6 { };
    static class C7ExtC6I1 extends C6 implements I1 { };
    static class C8ExtC6I1 extends C6 implements I1 { };
    
    private static class DummyOverloadedMethodsSubset extends OverloadedMethodsSubset {

        DummyOverloadedMethodsSubset(boolean bugfixed) {
            super(bugfixed);
        }

        @Override
        Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
            return memberDesc.paramTypes;
        }

        @Override
        void afterWideningUnwrappingHints(Class[] paramTypes, int[] paramNumericalTypes) {
            // Do nothing
        }

        @Override
        MaybeEmptyMemberAndArguments getMemberAndArguments(List tmArgs, BeansWrapper w) throws TemplateModelException {
            throw new RuntimeException("Not implemented in this dummy.");
        }
        
    };
    
}
