package freemarker.ext.beans;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.ACCEPTS_STRING);
        checkPossibleParamTypes(SingleTypeC.class, "mLong",
                TypeFlags.LONG | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mShort",
                TypeFlags.SHORT | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mByte",
                TypeFlags.BYTE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.ACCEPTS_ANY_OBJECT);
        checkPossibleParamTypes(SingleTypeC.class, "mDouble",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mFloat",
                TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mUnknown",
                TypeFlags.UNKNOWN_NUMERICAL_TYPE | TypeFlags.ACCEPTS_NUMBER);
        
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                TypeFlags.BIG_DECIMAL | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                TypeFlags.BIG_INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(SingleTypeC.class, "mVarParamCnt",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER);
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
        public void mVarParamCnt(Double a1,     Float a2, Integer a3) { }
        public void mVarParamCnt(Object a1,     char a2,  boolean a3, File a4, Map a5,    Boolean a6) { }
        public void mVarParamCnt(Long a1,       int a2,   short a3,   byte a4, double a5, float a6) { }
    }

    public void testMultipleNumTypes() {
        checkPossibleParamTypes(MultiTypeC.class, "m1",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.BYTE | TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m2",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.SHORT | TypeFlags.LONG | TypeFlags.FLOAT
                | TypeFlags.ACCEPTS_NUMBER | TypeFlags.ACCEPTS_CHAR
                );

        checkPossibleParamTypes(MultiTypeC.class, "m3",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.BIG_DECIMAL| TypeFlags.BIG_INTEGER
                | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.BIG_INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT
                | TypeFlags.BIG_DECIMAL | TypeFlags.UNKNOWN_NUMERICAL_TYPE
                | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m4",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.ACCEPTS_CHAR
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m5",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT
                | TypeFlags.FLOAT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(MultiTypeC.class, "m6",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", false, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", true, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", false, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertEquals(getPossibleParamTypes(MultiTypeC.class, "m6", true, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        checkPossibleParamTypes(MultiTypeC.class, "m6",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
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
        public void m6(File a1, Throwable a2) { };
        public void m6(File a1, Throwable a2, StringBuilder a3) { };
        public void m6(File a1, Throwable a2, Throwable a3) { };
        public void m6(double a1, int a2, File a3, File a4) { };
        public void m6(File a1, int a2, double a3, File a4) { };
    }

    public void testVarArgs() {
        checkPossibleParamTypes(VarArgsC.class, "m1",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m2",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.BIG_DECIMAL | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m4",
                TypeFlags.INTEGER | TypeFlags.LONG
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m5",
                TypeFlags.LONG | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m6",
                TypeFlags.LONG | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.ACCEPTS_STRING
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m7",
                TypeFlags.INTEGER | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.FLOAT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );

        checkPossibleParamTypes(VarArgsC.class, "m8",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.FLOAT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m9",
                TypeFlags.INTEGER | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m10",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m10",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m11",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m11",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m12",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        checkPossibleParamTypes(VarArgsC.class, "m12",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkPossibleParamTypes(VarArgsC.class, "m13",
                TypeFlags.ACCEPTS_CHAR,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkPossibleParamTypes(VarArgsC.class, "m13",
                TypeFlags.ACCEPTS_CHAR,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.UNKNOWN_NUMERICAL_TYPE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.LONG
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
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
            oms.addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(m, m.getParameterTypes()));
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
