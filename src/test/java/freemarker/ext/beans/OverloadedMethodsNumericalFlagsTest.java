package freemarker.ext.beans;

import java.beans.MethodDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import freemarker.template.Version;

public class OverloadedMethodsNumericalFlagsTest extends TestCase {

    public OverloadedMethodsNumericalFlagsTest(String name) {
        super(name);
    }
    
    private final BeansWrapper bw = new BeansWrapper(new Version(2, 3, 21)); 

    public void testSingleNumType() {
        checkPossibleParamTypes(SingleTypeC.class, "mInt",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_INTEGER | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT);
        checkPossibleParamTypes(SingleTypeC.class, "mLong",
                OverloadedNumberUtil.FLAG_LONG);
        checkPossibleParamTypes(SingleTypeC.class, "mShort",
                OverloadedNumberUtil.FLAG_SHORT);
        checkPossibleParamTypes(SingleTypeC.class, "mByte",
                OverloadedNumberUtil.FLAG_BYTE,
                0);
        checkPossibleParamTypes(SingleTypeC.class, "mDouble",
                OverloadedNumberUtil.FLAG_DOUBLE);
        checkPossibleParamTypes(SingleTypeC.class, "mFloat",
                OverloadedNumberUtil.FLAG_FLOAT);
        checkPossibleParamTypes(SingleTypeC.class, "mUnknown",
                OverloadedNumberUtil.FLAG_UNKNOWN_TYPE);
        
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                OverloadedNumberUtil.FLAG_BIG_DECIMAL);
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                OverloadedNumberUtil.FLAG_BIG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE);
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                OverloadedNumberUtil.FLAG_DOUBLE,
                OverloadedNumberUtil.FLAG_FLOAT,
                OverloadedNumberUtil.FLAG_INTEGER);
    }
    
    static public class SingleTypeC {
        public void mInt(int a1, String a2) { }
        public void mInt(int a1, int a2) { }
        public void mLong(long a1) { }
        public void mLong(Long a1) { }
        public void mShort(short a1) { }
        public void mByte(byte a1, boolean a2) { }
        public void mByte(byte a1, String a2) { }
        public void mByte(byte a1, Object a2) { }
        public void mDouble(double a1) { }
        public void mFloat(float a1) { }
        public void mUnknown(RationalNumber a1) { };
        public void mVarParamCnt(BigDecimal a1) { }
        public void mVarParamCnt(BigInteger a1, Double a2) { }
        public void mVarParamCnt(Double a1, Float a2, Integer a3) { }
        public void mVarParamCnt(Object a1, char a2, boolean a3, File a4, Map a5, Boolean a6) { }
        public void mVarParamCnt(Long a1, int a2, short a3, byte a4, double a5, float a6) { }
    }

    public void testMultipleNumType() {
        checkPossibleParamTypes(MultiTypeC.class, "m1",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT 
                | OverloadedNumberUtil.FLAG_BYTE | OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_INTEGER
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m2",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT 
                | OverloadedNumberUtil.FLAG_SHORT | OverloadedNumberUtil.FLAG_LONG | OverloadedNumberUtil.FLAG_FLOAT
                );

        checkPossibleParamTypes(MultiTypeC.class, "m3",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT 
                | OverloadedNumberUtil.FLAG_BIG_DECIMAL| OverloadedNumberUtil.FLAG_BIG_INTEGER,
                OverloadedNumberUtil.FLAG_BIG_INTEGER,
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                | OverloadedNumberUtil.FLAG_BIG_DECIMAL | OverloadedNumberUtil.FLAG_UNKNOWN_TYPE
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m4",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT | OverloadedNumberUtil.FLAG_FLOAT
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m5",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                | OverloadedNumberUtil.FLAG_FLOAT | OverloadedNumberUtil.FLAG_DOUBLE
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m6",
                OverloadedNumberUtil.FLAG_INTEGER
                );
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", false, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", true, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", false, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", true, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        checkPossibleParamTypes(MultiTypeC.class, "m6",
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT | OverloadedNumberUtil.FLAG_DOUBLE,
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT | OverloadedNumberUtil.FLAG_DOUBLE,
                0
                );
    }
    
    static public class MultiTypeC {
        public void m1(byte a1) { };
        public void m1(int a1) { };
        public void m1(double a2) { };
        
        public void m2(short a1) { };
        public void m2(long a1) { };
        public void m2(float a1) { };
        public void m2(char a1) { };
        
        public void m3(BigInteger a1, BigInteger a2, BigDecimal a3) { };
        public void m3(BigDecimal a1, BigInteger a2, RationalNumber a3) { };
        
        public void m4(float a1) { };
        public void m4(char a1) { };
        
        public void m5(Float a1) { };
        public void m5(Double a1) { };
        public void m5(Enum a1) { };
        
        public void m6(int a1) { };
        public void m6(String a1, char a2) { };
        public void m6(String a1, char a2, boolean a3) { };
        public void m6(String a1, char a2, char a3) { };
        public void m6(double a1, int a2, String a3, String a4) { };
        public void m6(String a1, int a2, double a3, String a4) { };
    }

    public void testVarArgs() {
        checkPossibleParamTypes(VarArgsC.class, "m1",
                OverloadedNumberUtil.FLAG_INTEGER
                );
        checkPossibleParamTypes(VarArgsC.class, "m2",
                OverloadedNumberUtil.FLAG_DOUBLE,
                OverloadedNumberUtil.FLAG_INTEGER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m3",
                OverloadedNumberUtil.FLAG_INTEGER
                );
        checkPossibleParamTypes(VarArgsC.class, "m3",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_INTEGER
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        checkPossibleParamTypes(VarArgsC.class, "m3",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_INTEGER
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_INTEGER
                | OverloadedNumberUtil.FLAG_BIG_DECIMAL | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m4",
                OverloadedNumberUtil.FLAG_INTEGER | OverloadedNumberUtil.FLAG_LONG
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m5",
                OverloadedNumberUtil.FLAG_LONG
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m6",
                OverloadedNumberUtil.FLAG_LONG | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m7",
                OverloadedNumberUtil.FLAG_INTEGER | OverloadedNumberUtil.FLAG_BYTE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_FLOAT
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );

        checkPossibleParamTypes(VarArgsC.class, "m8",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_FLOAT
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m9",
                OverloadedNumberUtil.FLAG_INTEGER | OverloadedNumberUtil.FLAG_BYTE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_DOUBLE
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m10",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE
                );
        checkPossibleParamTypes(VarArgsC.class, "m10",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE,
                OverloadedNumberUtil.FLAG_LONG | OverloadedNumberUtil.FLAG_DOUBLE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m11",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_SHORT
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        checkPossibleParamTypes(VarArgsC.class, "m11",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_SHORT
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_LONG | OverloadedNumberUtil.FLAG_DOUBLE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m12",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE
                );
        checkPossibleParamTypes(VarArgsC.class, "m12",
                OverloadedNumberUtil.FLAG_INTEGER,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_SHORT
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_BYTE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_LONG | OverloadedNumberUtil.FLAG_DOUBLE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m13",
                0,
                OverloadedNumberUtil.FLAG_DOUBLE);
        checkPossibleParamTypes(VarArgsC.class, "m13",
                0,
                OverloadedNumberUtil.FLAG_DOUBLE,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_UNKNOWN_TYPE
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT,
                OverloadedNumberUtil.FLAG_DOUBLE | OverloadedNumberUtil.FLAG_LONG
                | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT
                );
    }
    
    static public class VarArgsC {
        public void m1(int... va) { }
        
        public void m2(double a1, int... va) { }
        
        public void m3(int... va) { }
        public void m3(int a1, double... va) { }
        public void m3(int a1, double a2, BigDecimal... va) { }
        
        public void m4(int... va) { }
        public void m4(long... va) { }

        public void m5(Long... va) { }
        public void m5(long... va) { }
        
        public void m6(long... va) { }
        public void m6(String... va) { }
        
        public void m7(int a1, double... va) { }
        public void m7(byte a1, float... va) { }

        public void m8(int a1, double... va) { }
        public void m8(int a1, float... va) { }
        
        public void m9(int a1, double... va) { }
        public void m9(byte a1, double... va) { }
        
        public void m10(int a1, double a2, long... va) { }
        public void m10(int a1, double... va) { }
        
        public void m11(int a1, short a2, long... va) { }
        public void m11(int a1, double... va) { }
        
        public void m12(int a1, short a2, byte a3, long... va) { }
        public void m12(int a1, double... va) { }
        
        public void m13(char a1, double a2, RationalNumber a3, Long... va) { }
        public void m13(char a1, Double... va) { }
    }

    private OverloadedMethodsSubset newOverloadedMethodsSubset(Class cl, String methodName, final boolean desc) {
        final Method[] ms = cl.getMethods();
        
        final List<Method> filteredMethods = new ArrayList();
        for (Method m : ms) {
            if (m.getName().equals(methodName)) {
                filteredMethods.add(m);
            }
        }
        // As the order in which getMethods() returns the methods is undefined, we sort them for test predictability: 
        Collections.sort(filteredMethods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                int res = o1.toString().compareTo(o2.toString());
                return desc ? -res : res;
            }
        });
        
        final OverloadedMethodsSubset oms = cl.getName().indexOf("VarArgs") == -1
                ? new OverloadedFixArgsMethods(bw.is2321Bugfixed()) : new OverloadedVarArgsMethods(bw.is2321Bugfixed());
        for (Method m : filteredMethods) {
            oms.addCallableMemberDescriptor(new CallableMemberDescriptor(m, m.getParameterTypes()));
        }
        return oms;
    }
    
    private void checkPossibleParamTypes(Class cl, String methodName, int... expectedParamTypes) {
        checkPossibleParamTypes(cl, methodName, false, expectedParamTypes);
        checkPossibleParamTypes(cl, methodName, true, expectedParamTypes);
    }
    
    private void checkPossibleParamTypes(Class cl, String methodName, boolean revMetOrder, int... expectedParamTypes) {
        int[] actualParamTypes = getPossibleParamTypes(cl, methodName, revMetOrder, expectedParamTypes.length);
        assertNotNull(actualParamTypes);
        assertEquals(expectedParamTypes.length, actualParamTypes.length);
        for (int i = 0; i < expectedParamTypes.length; i++) {
            assertEquals(expectedParamTypes[i], actualParamTypes[i]);
        }
    }

    private int[] getPossibleParamTypes(Class cl, String methodName, boolean revMetOrder, int paramCnt) {
        OverloadedMethodsSubset oms = newOverloadedMethodsSubset(cl, methodName, revMetOrder);
        int[] actualParamTypes = oms.getPossibleNumericalTypes(paramCnt);
        return actualParamTypes;
    }
    
}
