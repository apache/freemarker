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

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import freemarker.template.Configuration;

public class TypeFlagsTest extends TestCase {

    public TypeFlagsTest(String name) {
        super(name);
    }
    
    private final BeansWrapper bw = new BeansWrapper(Configuration.VERSION_2_3_21); 

    public void testSingleNumType() {
        checkTypeFlags(SingleNumTypeC.class, "mInt",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.ACCEPTS_STRING);
        checkTypeFlags(SingleNumTypeC.class, "mLong",
                TypeFlags.LONG | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mShort",
                TypeFlags.SHORT | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mByte",
                TypeFlags.BYTE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.ACCEPTS_ANY_OBJECT);
        checkTypeFlags(SingleNumTypeC.class, "mDouble",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mFloat",
                TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mUnknown",
                TypeFlags.UNKNOWN_NUMERICAL_TYPE | TypeFlags.ACCEPTS_NUMBER);
        
        checkTypeFlags(SingleNumTypeC.class, "mVarParamCnt",
                TypeFlags.BIG_DECIMAL | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mVarParamCnt",
                TypeFlags.BIG_INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(SingleNumTypeC.class, "mVarParamCnt",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER);
    }
    
    static public class SingleNumTypeC {
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
        checkTypeFlags(MultiNumTypeC.class, "m1",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.BYTE | TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(MultiNumTypeC.class, "m2",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.SHORT | TypeFlags.LONG | TypeFlags.FLOAT
                | TypeFlags.ACCEPTS_NUMBER | TypeFlags.CHARACTER
                );

        checkTypeFlags(MultiNumTypeC.class, "m3",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT 
                | TypeFlags.BIG_DECIMAL| TypeFlags.BIG_INTEGER
                | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.BIG_INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT
                | TypeFlags.BIG_DECIMAL | TypeFlags.UNKNOWN_NUMERICAL_TYPE
                | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(MultiNumTypeC.class, "m4",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.FLOAT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.CHARACTER
                );
        
        checkTypeFlags(MultiNumTypeC.class, "m5",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT
                | TypeFlags.FLOAT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(MultiNumTypeC.class, "m6",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        assertSame(getTypeFlags(MultiNumTypeC.class, "m6", false, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertSame(getTypeFlags(MultiNumTypeC.class, "m6", true, 2), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertSame(getTypeFlags(MultiNumTypeC.class, "m6", false, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        assertSame(getTypeFlags(MultiNumTypeC.class, "m6", true, 3), OverloadedMethodsSubset.ALL_ZEROS_ARRAY);
        checkTypeFlags(MultiNumTypeC.class, "m6",
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                0
                );
    }
    
    static public class MultiNumTypeC {
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

    public void testVarargsNums() {
        checkTypeFlags(VarArgsC.class, "m1",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m2",
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m3",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.INTEGER
                | TypeFlags.BIG_DECIMAL | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m4",
                TypeFlags.INTEGER | TypeFlags.LONG
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m5",
                TypeFlags.LONG | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m6",
                TypeFlags.LONG | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.ACCEPTS_STRING
                );
        
        checkTypeFlags(VarArgsC.class, "m7",
                TypeFlags.INTEGER | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.FLOAT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );

        checkTypeFlags(VarArgsC.class, "m8",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.FLOAT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m9",
                TypeFlags.INTEGER | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m10",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m10",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m11",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT | TypeFlags.ACCEPTS_NUMBER
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m11",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m12",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER
                );
        checkTypeFlags(VarArgsC.class, "m12",
                TypeFlags.INTEGER | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.SHORT
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.DOUBLE | TypeFlags.BYTE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER,
                TypeFlags.LONG | TypeFlags.DOUBLE
                | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT | TypeFlags.ACCEPTS_NUMBER
                );
        
        checkTypeFlags(VarArgsC.class, "m13",
                TypeFlags.CHARACTER,
                TypeFlags.DOUBLE | TypeFlags.ACCEPTS_NUMBER);
        checkTypeFlags(VarArgsC.class, "m13",
                TypeFlags.CHARACTER,
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
    
    public void testAllZeros() {
        for (boolean reverse : new boolean[] { true, false }) {
            assertSame(OverloadedMethodsSubset.ALL_ZEROS_ARRAY, getTypeFlags(AllZeroC.class, "m1", reverse, 0));
            assertSame(OverloadedMethodsSubset.ALL_ZEROS_ARRAY, getTypeFlags(AllZeroC.class, "m2", reverse, 2));
            assertSame(OverloadedMethodsSubset.ALL_ZEROS_ARRAY, getTypeFlags(AllZeroC.class, "m3", reverse, 1));
        }
    }
    
    static public class AllZeroC {
        public void m1() {}
        
        public void m2(File a1, File a2) {}
        
        public void m3(File a1) {}
        public void m3(StringBuilder a1) {}
    }

    public void testAcceptanceNonOverloaded() {
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber1",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.UNKNOWN_NUMERICAL_TYPE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber2",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.BYTE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber3",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.BYTE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber4",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.SHORT);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber5",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.SHORT);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber6",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.INTEGER);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber7",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.INTEGER);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber8",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.LONG);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber9",  TypeFlags.ACCEPTS_NUMBER | TypeFlags.LONG);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber10", TypeFlags.ACCEPTS_NUMBER | TypeFlags.FLOAT);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber11", TypeFlags.ACCEPTS_NUMBER | TypeFlags.FLOAT);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber12", TypeFlags.ACCEPTS_NUMBER | TypeFlags.DOUBLE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber13", TypeFlags.ACCEPTS_NUMBER | TypeFlags.DOUBLE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber14", TypeFlags.ACCEPTS_NUMBER | TypeFlags.BIG_INTEGER);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber15", TypeFlags.ACCEPTS_NUMBER | TypeFlags.BIG_DECIMAL);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mNumber16", TypeFlags.ACCEPTS_NUMBER | TypeFlags.UNKNOWN_NUMERICAL_TYPE);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mDate", TypeFlags.ACCEPTS_DATE);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mSQLDate1", 0);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mSQLDate2", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mString", TypeFlags.ACCEPTS_STRING);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mCharSequence", TypeFlags.ACCEPTS_STRING);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mStringBuilder", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mBool", TypeFlags.ACCEPTS_BOOLEAN);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mBoolean", TypeFlags.ACCEPTS_BOOLEAN);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mMap1", TypeFlags.ACCEPTS_MAP);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mMap2", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mList1", TypeFlags.ACCEPTS_LIST);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mList2", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mSet1", TypeFlags.ACCEPTS_SET);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mSet2", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mCollection", TypeFlags.ACCEPTS_SET | TypeFlags.ACCEPTS_LIST);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mChar1", TypeFlags.CHARACTER);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mChar2", TypeFlags.CHARACTER);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mArray1", TypeFlags.ACCEPTS_ARRAY);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mArray2", TypeFlags.ACCEPTS_ARRAY);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mArray3", TypeFlags.ACCEPTS_ARRAY);
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mArray4", TypeFlags.ACCEPTS_ARRAY);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mObject", TypeFlags.ACCEPTS_ANY_OBJECT);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_NUMBER) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_STRING) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_BOOLEAN) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_MAP) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_LIST) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_SET) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.ACCEPTS_ARRAY) != 0);
        assertTrue((TypeFlags.ACCEPTS_ANY_OBJECT & TypeFlags.CHARACTER) == 0);  // deliberatly 0 
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mMapDate", 0);
        
        checkTypeFlags(AcceptanceNonoverloadedC.class, "mDateBooleanList",
                TypeFlags.ACCEPTS_DATE, TypeFlags.ACCEPTS_BOOLEAN, TypeFlags.ACCEPTS_LIST);
    }
    
    static public class AcceptanceNonoverloadedC {
        public void mNumber1(Number a1) {}
        public void mNumber2(Byte a1) {}
        public void mNumber3(byte a1) {}
        public void mNumber4(Short a1) {}
        public void mNumber5(short a1) {}
        public void mNumber6(Integer a1) {}
        public void mNumber7(int a1) {}
        public void mNumber8(Long a1) {}
        public void mNumber9(long a1) {}
        public void mNumber10(Float a1) {}
        public void mNumber11(float a1) {}
        public void mNumber12(Double a1) {}
        public void mNumber13(double a1) {}
        public void mNumber14(BigInteger a1) {}
        public void mNumber15(BigDecimal a1) {}
        public void mNumber16(RationalNumber a1) {}
        
        public void mDate(Date a1) {}
        public void mSQLDate1(java.sql.Date a1) {}
        public void mSQLDate2(java.sql.Timestamp a1) {}
        
        public void mString(String a1) {}
        public void mCharSequence(CharSequence a1) {}
        public void mStringBuilder(StringBuilder a1) {}
        
        public void mBool(boolean a1) {}
        public void mBoolean(Boolean a1) {}

        public void mMap1(Map a1) {}
        public void mMap2(LinkedHashMap a1) {}
        
        public void mList1(List a1) {}
        public void mList2(ArrayList a1) {}
        
        public void mSet1(Set a1) {}
        public void mSet2(HashSet a1) {}
        
        public void mCollection(Collection a1) {}
        
        public void mMapDate(MapDate a1) {}

        public void mChar1(Character a1) {}
        public void mChar2(char a1) {}

        public void mArray1(Object[] a1) {}
        public void mArray2(int[] a1) {}
        public void mArray3(Integer[] a1) {}
        public void mArray4(Void[] a1) {}
        
        public void mObject(Object a1) {}
        
        public void mDateBooleanList(Date a1, boolean a2, List a3) {}
    }

    public void testAcceptanceOverloaded() {
        checkTypeFlags(AcceptanceOverloadedC.class, "mLongDateList",
                TypeFlags.ACCEPTS_NUMBER | TypeFlags.LONG | TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT
                | TypeFlags.ACCEPTS_DATE | TypeFlags.ACCEPTS_LIST);
        checkTypeFlags(AcceptanceOverloadedC.class, "mBoolean", TypeFlags.ACCEPTS_BOOLEAN);
        checkTypeFlags(AcceptanceOverloadedC.class, "mStringChar",
                TypeFlags.ACCEPTS_STRING | TypeFlags.CHARACTER);
        checkTypeFlags(AcceptanceOverloadedC.class, "mStringFile", TypeFlags.ACCEPTS_STRING);
        checkTypeFlags(AcceptanceOverloadedC.class, "mMapObject", TypeFlags.ACCEPTS_ANY_OBJECT);
        checkTypeFlags(AcceptanceOverloadedC.class, "mSetMap", TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_SET);
        checkTypeFlags(AcceptanceOverloadedC.class, "mCollectionMap",
                TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_SET | TypeFlags.ACCEPTS_LIST);
        checkTypeFlags(AcceptanceOverloadedC.class, "mArray", TypeFlags.ACCEPTS_ARRAY);
        checkTypeFlags(AcceptanceOverloadedC.class, "mArrayList", TypeFlags.ACCEPTS_ARRAY | TypeFlags.ACCEPTS_LIST);
        
        checkTypeFlags(AcceptanceOverloadedC.class, "mStringCollectionThenBooleanThenMapList",
                TypeFlags.ACCEPTS_STRING | TypeFlags.ACCEPTS_LIST | TypeFlags.ACCEPTS_SET,
                TypeFlags.ACCEPTS_BOOLEAN,
                TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_LIST);
    }
    
    static public class AcceptanceOverloadedC {
        public void mLongDateList(long a1) {}
        public void mLongDateList(Date a1) {}
        public void mLongDateList(List a1) {}

        public void mBoolean(boolean a1) {}
        public void mBoolean(Boolean a1) {}
        
        public void mDate(Date a1) {}
        public void mDate(java.sql.Date a1) {}
        public void mDate(java.sql.Timestamp a1) {}
        
        public void mStringChar(String a1) {}
        public void mStringChar(char a1) {}
        public void mStringChar(Character a1) {}

        public void mStringFile(String a1) {}
        public void mStringFile(File a1) {}
        
        public void mMapObject(Map a1) {}
        public void mMapObject(Object a1) {}
        
        public void mSetMap(Set a1) {}
        public void mSetMap(Map a1) {}
        
        public void mCollectionMap(Collection a1) {}
        public void mCollectionMap(Map a1) {}
        
        public void mArray(Object[] a1) {}
        public void mArray(int[] a1) {}
        public void mArray(Integer[] a1) {}
        public void mArray(Void[] a1) {}
        
        public void mArrayList(String[] a1) {}
        public void mArrayList(List a1) {}
        
        public void mStringCollectionThenBooleanThenMapList(String a1, boolean a2, Map a3) {}
        public void mStringCollectionThenBooleanThenMapList(Collection a1, boolean a2, Map a3) {}
        public void mStringCollectionThenBooleanThenMapList(String a1, boolean a2, List a3) {}
    }

    public void testAcceptanceVarargsC() {
        checkTypeFlags(AcceptanceVarArgsC.class, "m1",
                TypeFlags.ACCEPTS_LIST | TypeFlags.ACCEPTS_STRING);
        
        checkTypeFlags(AcceptanceVarArgsC.class, "m2",
                TypeFlags.ACCEPTS_MAP,
                TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_STRING | TypeFlags.ACCEPTS_BOOLEAN,
                TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_STRING);
        checkTypeFlags(AcceptanceVarArgsC.class, "m2",
                TypeFlags.ACCEPTS_MAP,
                TypeFlags.ACCEPTS_MAP | TypeFlags.ACCEPTS_STRING | TypeFlags.ACCEPTS_BOOLEAN);
        checkTypeFlags(AcceptanceVarArgsC.class, "m2",
                TypeFlags.ACCEPTS_MAP);
        
        checkTypeFlags(AcceptanceVarArgsC.class, "m3",
                TypeFlags.ACCEPTS_BOOLEAN);
        checkTypeFlags(AcceptanceVarArgsC.class, "m3",
                TypeFlags.ACCEPTS_BOOLEAN | TypeFlags.ACCEPTS_STRING,
                TypeFlags.ACCEPTS_BOOLEAN | TypeFlags.ACCEPTS_MAP,
                TypeFlags.ACCEPTS_BOOLEAN | TypeFlags.CHARACTER,
                TypeFlags.ACCEPTS_BOOLEAN);
    }
    
    static public class AcceptanceVarArgsC {
        public void m1(List... a1) {}
        public void m1(String... a1) {}

        public void m2(Map a1, String... a2) {}
        public void m2(Map a1, boolean a2, String... a3) {}
        public void m2(Map... a1) {}
        
        public void m3(String a1, Map a2, char a3, Boolean... a4) {}
        public void m3(boolean... a1) {}
    }

    static public class MapDate extends Date implements Map {
    
        public int size() {
            return 0;
        }
    
        public boolean isEmpty() {
            return false;
        }
    
        public boolean containsKey(Object key) {
            return false;
        }
    
        public boolean containsValue(Object value) {
            return false;
        }
    
        public Object get(Object key) {
            return null;
        }
    
        public Object put(Object key, Object value) {
            return null;
        }
    
        public Object remove(Object key) {
            return null;
        }
    
        public void putAll(Map m) {
        }
    
        public void clear() {
        }
    
        public Set keySet() {
            return null;
        }
    
        public Collection values() {
            return null;
        }
    
        public Set entrySet() {
            return null;
        }
        
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
    
    private void checkTypeFlags(Class cl, String methodName, int... expectedTypeFlags) {
        checkTypeFlags(cl, methodName, false, expectedTypeFlags);
        checkTypeFlags(cl, methodName, true, expectedTypeFlags);
    }
    
    private void checkTypeFlags(Class cl, String methodName, boolean revMetOrder, int... expectedTypeFlags) {
        int[] actualParamTypes = getTypeFlags(cl, methodName, revMetOrder, expectedTypeFlags.length);
        assertNotNull("Method " + methodName + "(#" + expectedTypeFlags.length + ") doesn't exist", actualParamTypes);
        if (actualParamTypes != OverloadedMethodsSubset.ALL_ZEROS_ARRAY) {
            assertEquals(expectedTypeFlags.length, actualParamTypes.length);
            for (int i = 0; i < expectedTypeFlags.length; i++) {
                assertEquals(expectedTypeFlags[i], actualParamTypes[i]);
            }
        } else {
            for (int i = 0; i < expectedTypeFlags.length; i++) {
                assertEquals(expectedTypeFlags[i], 0);
            }
        }
    }

    private int[] getTypeFlags(Class cl, String methodName, boolean revMetOrder, int paramCnt) {
        OverloadedMethodsSubset oms = newOverloadedMethodsSubset(cl, methodName, revMetOrder);
        int[] actualParamTypes = oms.getTypeFlags(paramCnt);
        return actualParamTypes;
    }
    
}
