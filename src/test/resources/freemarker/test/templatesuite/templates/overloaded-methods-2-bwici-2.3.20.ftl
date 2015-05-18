<#-- The parts of the IcI 2.3.20 tests that give the same result regardless of method introspection order -->
<#-- Note that the point of 2.3.20 tests is to check if bugs fixed in 2.3.21 are still emulated in pre-2.3.21 mode -->

<#include "overloaded-methods-2-common.ftl">

<@assertFails message="no compatible overloaded">${obj.mNull1(null)}</@>
<@assertEquals actual=obj.mNull1(123) expected="mNull1(int a1 = 123)" />
<@assertEquals actual=obj.mNull2(null) expected="mNull2(Object a1 = null)" />
<@assertFails message="no compatible overloaded">${obj.mVarargs('a', null)}</@>
<@assertFails message="no compatible overloaded">${obj.mVarargs(null, 'a')}</@>
<@assertFails message="multiple compatible overloaded">${obj.mSpecificity('a', 'b')}</@>
<@assertFails message="multiple compatible overloaded">${obj.mBoolean(true)}</@>

<@assertEquals actual=obj.mIntNonOverloaded(123?long) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123.5) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(2147483648) expected=-2147483648 /> <#-- overflow -->
<@assertFails message="no compatible overloaded">${obj.mNumBoxedVSBoxed(123.5)}</@>
<@assertFails message="no compatible overloaded">${obj.mNumBoxedVSBoxed(123?int)}</@>
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?long) expected="mNumBoxedVSBoxed(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?short) expected="mNumBoxedVSBoxed(Short a1 = 123)" />
<@assertEquals 
    actual=obj.mNumUnambigous(2147483648) expected="mNumUnambigous(Integer a1 = -2147483648)" /> <#-- overflow -->

<@assertFails message="multiple compatible overloaded">${obj.mIntPrimVSBoxed(123?int)}</@>

<@assertEquals actual=obj.mNumPrimVSPrim(123?short) expected="mNumPrimVSPrim(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?int) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?long) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertFails message="no compatible overloaded">${obj.mNumPrimVSPrim(123?double)}</@>
<@assertEquals actual=obj.mNumPrimVSPrim(123456) expected="mNumPrimVSPrim(short a1 = -7616)" /> <#-- overflow due to bad choice -->

<@assertEquals actual=obj.mNumPrimAll(123?byte) expected="mNumPrimAll(byte a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?short) expected="mNumPrimAll(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?int) expected="mNumPrimAll(int a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?long) expected="mNumPrimAll(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?float) expected="mNumPrimAll(float a1 = 123.0)" />
<@assertEquals actual=obj.mNumPrimAll(123?double) expected="mNumPrimAll(double a1 = 123.0)" />
<@assertFails message="multiple compatible overloaded">${obj.mNumPrimAll(123)}</@>
<@assertEquals actual=obj.mNumPrimAll(obj.bigInteger(123)) expected="mNumPrimAll(BigInteger a1 = 123)" />

<@assertEquals actual=obj.mNumBoxedAll(123?byte) expected="mNumBoxedAll(Byte a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll(123?short) expected="mNumBoxedAll(Short a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll(123?int) expected="mNumBoxedAll(Integer a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll(123?long) expected="mNumBoxedAll(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll(123?float) expected="mNumBoxedAll(Float a1 = 123.0)" />
<@assertEquals actual=obj.mNumBoxedAll(123?double) expected="mNumBoxedAll(Double a1 = 123.0)" />
<@assertEquals actual=obj.mNumBoxedAll(123) expected="mNumBoxedAll(BigDecimal a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll(obj.bigInteger(123)) expected="mNumBoxedAll(BigInteger a1 = 123)" />

<@assertEquals actual=obj.mNumPrimAll2nd(123?byte) expected="mNumPrimAll2nd(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll2nd(123?short) expected="mNumPrimAll2nd(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll2nd(123?int) expected="mNumPrimAll2nd(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll2nd(123?long) expected="mNumPrimAll2nd(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll2nd(123?float) expected="mNumPrimAll2nd(double a1 = 123.0)" />
<@assertEquals actual=obj.mNumPrimAll2nd(123?double) expected="mNumPrimAll2nd(double a1 = 123.0)" />

<@assertFails message="no compatible overloaded">${obj.mNumBoxedAll2nd(123?byte)}</@>
<@assertEquals actual=obj.mNumBoxedAll2nd(123?short) expected="mNumBoxedAll2nd(Short a1 = 123)" />
<@assertFails message="no compatible overloaded">${obj.mNumBoxedAll2nd(123?int)}</@>
<@assertEquals actual=obj.mNumBoxedAll2nd(123?long) expected="mNumBoxedAll2nd(Long a1 = 123)" />
<@assertFails message="no compatible overloaded">${obj.mNumBoxedAll2nd(123?float)}</@>
<@assertEquals actual=obj.mNumBoxedAll2nd(123?double) expected="mNumBoxedAll2nd(Double a1 = 123.0)" />

<@assertFails message="multiple compatible overloaded">${obj.mNumPrimFallbackToNumber(123?int)}</@>
<@assertFails message="multiple compatible overloaded">${obj.mNumPrimFallbackToNumber(123?long)}</@>
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?double) expected="mNumPrimFallbackToNumber(Number a1 = 123.0)" />
<@assertFails message="multiple compatible overloaded">${obj.mNumPrimFallbackToNumber(123)}</@>
<@assertEquals actual=obj.mNumPrimFallbackToNumber(obj.bigInteger(123)) expected="mNumPrimFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber('x') expected="mNumPrimFallbackToNumber(Object a1 = x)" />

<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?int) expected="mNumBoxedFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?long) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?double) expected="mNumBoxedFallbackToNumber(Number a1 = 123.0)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123) expected="mNumBoxedFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(obj.bigInteger(123)) expected="mNumBoxedFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber('x') expected="mNumBoxedFallbackToNumber(Object a1 = x)" />

<@assertEquals actual=obj.mDecimalLoss(1.5) expected="mDecimalLoss(int a1 = 1)" /><#-- Yes, buggy... -->
<@assertEquals actual=obj.mDecimalLoss(1.5?double) expected="mDecimalLoss(double a1 = 1.5)" />

<#-- BigDecimal conversions chose the smallest target type before IcI 2.3.31, increasing the risk of overflows: -->
<@assertEquals actual=obj.nIntAndLong(1) expected="nIntAndLong(int 1)" />
<@assertEquals actual=obj.nIntAndLong(1?long) expected="nIntAndLong(long 1)" />
<@assertEquals actual=obj.nIntAndShort(1) expected="nIntAndShort(short 1)" />
<@assertEquals actual=obj.nIntAndShort(1?short) expected="nIntAndShort(short 1)" />
<@assertEquals actual=obj.nIntAndShort(1?int) expected="nIntAndShort(int 1)" />
<@assertEquals actual=obj.nLongAndShort(1) expected="nLongAndShort(short 1)" />
<@assertEquals actual=obj.nLongAndShort(1?short) expected="nLongAndShort(short 1)" />
<@assertEquals actual=obj.nLongAndShort(1?long) expected="nLongAndShort(long 1)" />

<#-- Usual wrong choice on null: -->
<@assertEquals actual=obj.varargs1(null, 1, 2, 3.5) expected='varargs1(Object s = null, Object... xs = [1, 2, 3.5])' />

<#-- Some bugs that cause loosing of decimals will occur here... -->
<@assertFails message="multiple compatible overloaded">${obj.varargs1('s', 1, 2, 3.5)}</@>
<@assertEquals actual=obj.varargs1('s', 1, 2, 'c') expected='varargs1(String s = "s", Object... xs = [1, 2, c])' />
<@assertEquals actual=obj.varargs1('s', 1, 'b', 3) expected='varargs1(String s = "s", Object... xs = [1, b, 3])' />
<@assertEquals actual=obj.varargs1('s', 'a', 2, 3) expected='varargs1(String s = "s", Object... xs = [a, 2, 3])' />
<@assertFails message="multiple compatible overloaded">${obj.varargs1('s', 1, 2, 3)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs1('s', 1.1, 2.1, 3.1)}</@>
<@assertEquals actual=obj.varargs1('s', 'a', 'b', 'c') expected='varargs1(String s = "s", Object... xs = [a, b, c])' />
<@assertFails message="multiple compatible overloaded"><@assertEquals actual=obj.varargs1('s', 1?double, 2?byte, 3?byte) expected='varargs1(String s = "s", int... xs = [1, 2, 3])' /></@>
<@assertEquals actual=obj.varargs1(0, 1, 2, 3) expected='varargs1(Object s = 0, Object... xs = [1, 2, 3])' />
<@assertFails message="multiple compatible overloaded">${obj.varargs1('s', 1?double, 2?double, 3?double)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs1('s')}</@>

<@assertEquals actual=obj.varargs2(1, 2.5, 3) expected='varargs2(int... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs2(1, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?int, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?long, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?long, 2?double, 3) expected='varargs2(double... xs = [1.0, 2.0, 3.0])' />

<@assertEquals actual=obj.varargs3(1, 2, 3) expected='varargs3(Comparable... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs3('a', 'b', 'c') expected='varargs3(String... xs = [a, b, c])' />
<@assertEquals actual=obj.varargs3(1, 'b', 'c') expected='varargs3(Comparable... xs = [1, b, c])' />
<@assertEquals actual=obj.varargs3('a', 'b', 3) expected='varargs3(Comparable... xs = [a, b, 3])' />
<@assertEquals actual=obj.varargs3('a', [], 3) expected='varargs3(Object... xs = [a, [], 3])' />
<@assertEquals actual=obj.varargs3(null, 'b', null) expected='varargs3(Object... xs = [null, b, null])' />
<@assertEquals actual=obj.varargs3(null, 2, null) expected='varargs3(Object... xs = [null, 2, null])' />
<@assertEquals actual=obj.varargs3(null, [], null) expected='varargs3(Object... xs = [null, [], null])' />
<@assertEquals actual=obj.varargs3(null, null, null) expected='varargs3(Object... xs = [null, null, null])' />
<@assertEquals actual=obj.varargs3() expected='varargs3(String... xs = [])' />

<@assertFails message="no compatible overloaded">${obj.varargs4(null, null, null)}</@>

<@assertFails message="multiple compatible overloaded">${obj.varargs5(1, 2, 3, 4, 5)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs5(1, 2, 3, 4)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs5(1, 2, 3)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs5(1, 2)}</@>
<@assertFails message="multiple compatible overloaded">${obj.varargs5(1)}</@>
<@assertEquals actual=obj.varargs5() expected='varargs5(int... xs = [])' />

<@assertEquals actual=obj.varargs6('s', 2) expected='varargs6(String a1 = s, int... xs = [2])' />
<@assertEquals actual=obj.varargs6('s') expected='varargs6(String a1 = s, int... xs = [])' />
<@assertEquals actual=obj.varargs6(1, 2) expected='varargs6(Object a1 = 1, int a2 = 2, int... xs = [])' />
<@assertFails message="no compatible overloaded">${obj.varargs6(1)}</@>

<@assertEquals actual=obj.varargs7(1?int, 2?int) expected='varargs7(int... xs = [1, 2])' />
<@assertEquals actual=obj.varargs7(1?short, 2?int) expected='varargs7(short a1 = 1, int... xs = [2])' />

<@assertEquals actual=obj.mNullAmbiguous('a') expected='mNullAmbiguous(String s = a)' />
<@assertEquals actual=obj.mNullAmbiguous(123) expected='mNullAmbiguous(int i = 123)' />
<@assertEquals actual=obj.mNullAmbiguous(1.9) expected='mNullAmbiguous(int i = 1)' />
<@assertFails message="no compatible overloaded">${obj.mNullAmbiguous(1?double)}</@>
<@assertFails message="no compatible overloaded">${obj.mNullAmbiguous(1.9?double)}</@>
<@assertFails message="no compatible overloaded">${obj.mNullAmbiguous(null)}</@>

<@assertEquals actual=obj.mNullAmbiguous2(null) expected='mNullAmbiguous(Object o = null)' />

<@assertFails message="no compatible overloaded">${obj.mNullNonAmbiguous(null)}</@>

<@assertEquals actual=obj.mRareWrappings(obj.stringAdaptedToBoolean2, obj.wrapperNumber, obj.wrapperNumber, obj.wrapperNumber, obj.stringAdaptedToBoolean2)
               expected='mRareWrappings(String s = yes, double d1 = 123.0001, Double d2 = 123.0001, double d3 = 123.0001, b = true)' />
               
<@assertFails message="no compatible overloaded">${obj.mRareWrappings2(obj.adaptedNumber)}</@>

<#-- Test for List VS array problems due to too vague hinting: -->

<@assertEquals actual=obj.mSeqToArrayNonOverloaded(['a', 'b'], 'c') expected='mSeqToArrayNonOverloaded(String[] [a, b], String c)' />

<@assertEquals actual=obj.mSeqToArrayGoodHint(['a', 'b'], 'c') expected='mSeqToArrayGoodHint(String[] [a, b], String c)' />
<@assertEquals actual=obj.mSeqToArrayGoodHint(['a', 'b'], 3) expected='mSeqToArrayGoodHint(String[] [a, b], int 3)' />

<@assertEquals actual=obj.mSeqToArrayGoodHint2(['a', 'b'], 'c') expected='mSeqToArrayGoodHint2(String[] [a, b], String c)' />
<@assertEquals actual=obj.mSeqToArrayGoodHint2('a') expected='mSeqToArrayGoodHint2(String a)' />

<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint(['a', 'b'], 'c') expected='mSeqToArrayPoorHint(String[] [a, b], String c)' /></@>
<@assertEquals actual=obj.mSeqToArrayPoorHint('a', 2) expected='mSeqToArrayPoorHint(String a, int 2)' />

<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint2(['a', 'b']) expected='mSeqToArrayPoorHint2(String[] [a, b])' /></@>
<@assertEquals actual=obj.mSeqToArrayPoorHint2('a') expected='mSeqToArrayPoorHint2(String a)' />

<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint3(['a', 'b']) expected='mSeqToArrayPoorHint3(String[] [a, b])' /></@>
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint3([1, 2]) expected='mSeqToArrayPoorHint3(int[] [a, b])' /></@>

<@assertEquals actual=obj.mStringArrayVsListPreference(['a', 'b']) expected="mStringArrayVsListPreference(List [a, b])" />
<@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(['a', 'b'])}</@>
<#if !dow>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsListPreference(obj.javaObjectArray)}</@>
</#if>
<@assertFails message="no compatible overloaded">${obj.mIntArrayVsIntegerArrayPreference([1, 2])}</@>

<#if dow>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(obj.javaStringArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(obj.javaIntArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(obj.javaIntegerArray)}</@>
<#else>
  <@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaStringArray) expected="mStringArrayVsObjectArrayPreference(String[] [a, b])" />
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(obj.javaIntArray)}</@>
  <@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaIntegerArray) expected="mStringArrayVsObjectArrayPreference(Object[] [11, 22])" />
</#if>

<@assertFails message="no compatible overloaded">${obj.mIntegerArrayOverloaded([1, 2], 3)}</@>
<@assertFails message="no compatible overloaded">${obj.mIntegerArrayOverloaded([1?byte, 2?byte], 3)}</@>
<@assertFails message="no compatible overloaded">${obj.mIntegerArrayOverloaded(obj.javaIntegerList, 3)}</@>
<@assertFails message="no compatible overloaded">${obj.mIntegerArrayOverloaded(obj.javaByteList, 3)}</@>

<@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded2(['a', 'b'])}</@>
<#if dow>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded2(obj.javaStringList)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded2(obj.javaStringArray)}</@>
<#else>
  <@assertEquals actual=obj.mStringArrayVarargsOverloaded2(obj.javaStringList) expected="mStringArrayVarargsOverloaded2(String[] [[a, b]])" /> <#-- toString() accident... -->
  <@assertEquals actual=obj.mStringArrayVarargsOverloaded2(obj.javaStringArray) expected="mStringArrayVarargsOverloaded2(String[] [a, b])" />
</#if>
<@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded2(['a'])}</@>

<#if dow>
  <#-- As with DOW we never end up with array-s after unwrapping, they just work like Lists: -->
  <@assertEquals actual=obj.mListOrString(obj.javaStringArray) expected="mListOrString(List [a, b])" />
  <@assertEquals actual=obj.mListListOrString(obj.javaStringArrayArray) expected="mListListOrString(List [[a, b], [], [c]])" />
  <@assertEquals actual=obj.mListOrString(obj.javaIntArray) expected="mListOrString(List [11, 22])" />
  <@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringArray) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />
  <@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringList, obj.javaStringArray) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />
  <@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringList) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />
<#else>
  <#-- Pure BeansWrapper unwraps to array-s, but before IcI 2.3.21 it couldn't treat them as Lists: -->
  <@assertFails message="no compatible overloaded">${obj.mListOrString(obj.javaStringArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mListListOrString(obj.javaStringArrayArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mListOrString(obj.javaIntArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded4(obj.javaStringList, obj.javaStringArray)}</@>
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringList)}</@>
</#if>

<@assertFails message="no compatible overloaded">${obj.mIntArrayArrayOverloaded(obj.javaListOfIntArrays)}</@>
<@assertFails message="no compatible overloaded">${obj.mArrayOfListsOverloaded(obj.javaListOfIntArrays)}</@>

<@assertFails message="no compatible overloaded">${obj.mMapOrBoolean(obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBoolean(obj.booleanAndScalarModel)}</@>

<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanVarargs(obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanVarargs(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanVarargs(obj.allModels, obj.allModels)}</@>

<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel, obj.hashAndScalarModel)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.allModels, obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrBooleanFixedAndVarargs(obj.allModels, obj.allModels, obj.allModels)}</@>

<@assertEquals actual=obj.mNumberOrArray(obj.allModels) expected="mNumberOrArray(Number 1)" />
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mNumberOrArray([obj.allModels]) expected="mNumberOrArray(Object[] [1])" /></@>
<@assertEquals actual=obj.mIntOrArray(obj.allModels) expected="mIntOrArray(int 1)" />
<@assertFails message="no compatible overloaded">${obj.mDateOrArray(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mStringOrArray(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mBooleanOrArray(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mMapOrArray(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mListOrArray(obj.allModels)}</@>
<@assertFails message="no compatible overloaded">${obj.mSetOrArray(obj.allModels)}</@>

<@assertFails message="no compatible overloaded">${obj.mCharOrCharacterOverloaded(null)}</@>
<@assertFails message="no compatible overloaded">${obj.mCharOrBooleanOverloaded('c')}</@>
<@assertEquals actual=obj.mCharOrBooleanOverloaded(true) expected="mCharOrBooleanOverloaded(boolean true)" />

<@assertFails message="no compatible overloaded">${obj.mCharOrStringOverloaded('c', true)}</@>
<@assertFails message="no compatible overloaded">${obj.mCharacterOrStringOverloaded('c', true)}</@>
<@assertEquals actual=obj.mCharOrStringOverloaded2('c') expected="mCharOrStringOverloaded2(String c)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded2('c') expected="mCharacterOrStringOverloaded2(String c)" />

<#-- The exmple given in bug report 363 -->
<#assign theMap = {'name':'Billy', 'lastName', 'Pilgrim'} />
<@assertEquals actual=obj.bugReport363(theMap, []) expected="Executed: testMethod(Map fields, List listField) on input: fields={name=Billy, lastName=Pilgrim} and listField=[]" />
<@assertEquals actual=obj.bugReport363(theMap, null) expected="Executed: testMethod(Object... fields) on input: fields=[{name=Billy, lastName=Pilgrim}, null]" />
