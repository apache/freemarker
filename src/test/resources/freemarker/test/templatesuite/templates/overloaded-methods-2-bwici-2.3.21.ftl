<#-- Since with IcI 2.3.21+ the results must not depend on method introspection order, there's only once template -->

<#include "overloaded-methods-2-common.ftl">

<@assertEquals actual=obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c')) expected='mVarargs(String... a1 = abc)' />

<@assertEquals actual=obj.mNull1(null) expected="mNull1(String a1 = null)" />
<@assertEquals actual=obj.mNull1(123) expected="mNull1(int a1 = 123)" />
<@assertEquals actual=obj.mNull2(null) expected="mNull2(String a1 = null)" />
<@assertEquals actual=obj.mVarargs('a', null) expected="mVarargs(String... a1 = anull)" />
<@assertEquals actual=obj.mVarargs(null, 'a') expected="mVarargs(File a1, String... a2)" />
<@assertEquals actual=obj.mSpecificity('a', 'b') expected="mSpecificity(String a1, Object a2)" />

<@assertEquals actual=obj.mChar('a') expected='mChar(char a1 = a)' />
<@assertEquals actual=obj.mBoolean(true) expected="mBoolean(boolean a1 = true)" />
<@assertEquals actual=obj.mBoolean(null) expected="mBoolean(Boolean a1 = null)" />

<@assertEquals actual=obj.mIntNonOverloaded(123?long) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123.5) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(2147483648) expected=-2147483648 /> <#-- overflow -->
<@assertEquals actual=obj.mNumBoxedVSBoxed(123.5) expected='mNumBoxedVSBoxed(Long a1 = 123)' />
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?int) expected='mNumBoxedVSBoxed(Long a1 = 123)' />
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?long) expected="mNumBoxedVSBoxed(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?short) expected="mNumBoxedVSBoxed(Short a1 = 123)" />
<@assertEquals 
    actual=obj.mNumUnambigous(2147483648) expected="mNumUnambigous(Integer a1 = -2147483648)" /> <#-- overflow -->

<@assertEquals actual=obj.mIntPrimVSBoxed(123?int) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123?short) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123) expected="mIntPrimVSBoxed(int a1 = 123)" />
<#-- This doesn't fail as 123L can be converted to int without loss: -->
<@assertEquals actual=obj.mIntPrimVSBoxed(123?long) expected="mIntPrimVSBoxed(int a1 = 123)" />

<@assertEquals actual=obj.mNumPrimVSPrim(123?short) expected="mNumPrimVSPrim(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?int) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?long) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?double) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123456) expected="mNumPrimVSPrim(long a1 = 123456)" />

<@assertEquals actual=obj.mNumPrimAll(123?byte) expected="mNumPrimAll(byte a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?short) expected="mNumPrimAll(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?int) expected="mNumPrimAll(int a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?long) expected="mNumPrimAll(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?float) expected="mNumPrimAll(float a1 = 123.0)" />
<@assertEquals actual=obj.mNumPrimAll(123?double) expected="mNumPrimAll(double a1 = 123.0)" />
<@assertEquals actual=obj.mNumPrimAll(123) expected="mNumPrimAll(BigDecimal a1 = 123)" />
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

<@assertEquals actual=obj.mNumBoxedAll2nd(123?byte) expected="mNumBoxedAll2nd(Short a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?short) expected="mNumBoxedAll2nd(Short a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?int) expected="mNumBoxedAll2nd(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?long) expected="mNumBoxedAll2nd(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?float) expected="mNumBoxedAll2nd(Double a1 = 123.0)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?double) expected="mNumBoxedAll2nd(Double a1 = 123.0)" />
 
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?int) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?long) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?double) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123.5?double) expected="mNumPrimFallbackToNumber(Number a1 = 123.5)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(obj.bigInteger(123)) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(obj.bigInteger(9223372036854775808))
    expected="mNumPrimFallbackToNumber(Number a1 = 9223372036854775808)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(obj.rational(246, 2)) expected="mNumPrimFallbackToNumber(Number a1 = 246/2)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber('x') expected="mNumPrimFallbackToNumber(Object a1 = x)" />

<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?int) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?long) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?double) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123.5?double) expected="mNumBoxedFallbackToNumber(Number a1 = 123.5)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(obj.bigInteger(123)) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(obj.bigInteger(9223372036854775808))
    expected="mNumBoxedFallbackToNumber(Number a1 = 9223372036854775808)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(obj.rational(246, 2)) expected="mNumBoxedFallbackToNumber(Number a1 = 246/2)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber('x') expected="mNumBoxedFallbackToNumber(Object a1 = x)" />

<@assertEquals actual=obj.mDecimalLoss(1.5) expected="mDecimalLoss(double a1 = 1.5)" />
<@assertEquals actual=obj.mDecimalLoss(1.5?double) expected="mDecimalLoss(double a1 = 1.5)" />

<@assertEquals actual=obj.mNumConversionLoses1(1?double, '', '') expected="Number 1.0 java.lang.Double" />
<@assertEquals actual=obj.mNumConversionLoses1(1?short, '', '') expected="Number 1 java.lang.Short" />
<@assertEquals actual=obj.mNumConversionLoses1(1?long, '', '') expected="Number 1 java.lang.Long" />
<@assertEquals actual=obj.mNumConversionLoses2(1?double, '', '') expected="Number 1.0 java.lang.Double" />
<@assertEquals actual=obj.mNumConversionLoses2(1?short, '', '') expected="Number 1 java.lang.Short" />
<@assertEquals actual=obj.mNumConversionLoses2(1?long, '', '') expected="Number 1 java.lang.Long" />
<@assertEquals actual=obj.mNumConversionLoses3(1?double, '', '') expected="Serializable 1.0 java.lang.Double" />
<@assertEquals actual=obj.mNumConversionLoses3(1?int, '', '') expected="Serializable 1 java.lang.Integer" />
<@assertEquals actual=obj.mNumConversionLoses3(1?short, '', '') expected="Serializable 1 java.lang.Short" />
<@assertEquals actual=obj.mNumConversionLoses3(1?long, '', '') expected="Serializable 1 java.lang.Long" />

<#-- BigDecimal-to-int is preferred over to-long for BC and user expectations: -->
<@assertEquals actual=obj.nIntAndLong(1) expected="nIntAndLong(int 1)" />
<@assertEquals actual=obj.nIntAndLong(1?long) expected="nIntAndLong(long 1)" />
<#-- BigDecimal-to-short is, however unfavored due to the higher chance of overflow: -->
<@assertEquals actual=obj.nIntAndShort(1) expected="nIntAndShort(int 1)" />
<@assertEquals actual=obj.nIntAndShort(1?short) expected="nIntAndShort(short 1)" />
<@assertEquals actual=obj.nLongAndShort(1) expected="nLongAndShort(long 1)" />
<@assertEquals actual=obj.nLongAndShort(1?short) expected="nLongAndShort(short 1)" />

<@assertEquals actual=obj.varargs1(null, 1, 2, 3.5) expected='varargs1(String s = null, double... xs = [1.0, 2.0, 3.5])' />
<@assertEquals actual=obj.varargs1(null, 1, 2.5, 3) expected='varargs1(String s = null, double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs1(null, 1.5, 2, 3) expected='varargs1(String s = null, double... xs = [1.5, 2.0, 3.0])' />
<@assertEquals actual=obj.varargs1(null, 1, 2, 'c') expected='varargs1(String s = null, Object... xs = [1, 2, c])' />
<@assertEquals actual=obj.varargs1(null, 1, 'b', 3) expected='varargs1(String s = null, Object... xs = [1, b, 3])' />
<@assertEquals actual=obj.varargs1(null, 'a', 2, 3) expected='varargs1(String s = null, Object... xs = [a, 2, 3])' />
<@assertEquals actual=obj.varargs1('s', 1, 2, 3) expected='varargs1(String s = "s", int... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs1('s', 1.1, 2.1, 3.1) expected='varargs1(String s = "s", double... xs = [1.1, 2.1, 3.1])' />
<@assertEquals actual=obj.varargs1('s', 'a', 'b', 'c') expected='varargs1(String s = "s", Object... xs = [a, b, c])' />
<@assertEquals actual=obj.varargs1(null, 1, 2, 3) expected='varargs1(String s = null, int... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs1(null, 1.1, 2.1, 3.1) expected='varargs1(String s = null, double... xs = [1.1, 2.1, 3.1])' />
<@assertEquals actual=obj.varargs1(null, 'a', 'b', 'c') expected='varargs1(String s = null, Object... xs = [a, b, c])' />
<@assertEquals actual=obj.varargs1(null, 1, 2, 3?double) expected='varargs1(String s = null, int... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs1(null, 1, 2?double, 3?double) expected='varargs1(String s = null, double... xs = [1.0, 2.0, 3.0])' />
<@assertEquals actual=obj.varargs1(null, 1, 2?float, 3?float) expected='varargs1(String s = null, double... xs = [1.0, 2.0, 3.0])' />
<@assertEquals actual=obj.varargs1(null, 1?double, 2?byte, 3?byte) expected='varargs1(String s = null, int... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs1(0, 1, 2, 3) expected='varargs1(Object s = 0, Object... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs1('s') expected='varargs1(String s = "s", int... xs = [])' />

<@assertEquals actual=obj.varargs2(1, 2.5, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?int, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?long, 2.5?double, 3) expected='varargs2(double... xs = [1.0, 2.5, 3.0])' />
<@assertEquals actual=obj.varargs2(1?long, 2?double, 3) expected='varargs2(int... xs = [1, 2, 3])' />

<@assertEquals actual=obj.varargs3(1, 2, 3) expected='varargs3(Comparable... xs = [1, 2, 3])' />
<@assertEquals actual=obj.varargs3('a', 'b', 'c') expected='varargs3(String... xs = [a, b, c])' />
<@assertEquals actual=obj.varargs3(1, 'b', 'c') expected='varargs3(Comparable... xs = [1, b, c])' />
<@assertEquals actual=obj.varargs3('a', 'b', 3) expected='varargs3(Comparable... xs = [a, b, 3])' />
<@assertEquals actual=obj.varargs3('a', [], 3) expected='varargs3(Object... xs = [a, [], 3])' />
<@assertEquals actual=obj.varargs3(null, 'b', null) expected='varargs3(String... xs = [null, b, null])' />
<@assertEquals actual=obj.varargs3(null, 2, null) expected='varargs3(Comparable... xs = [null, 2, null])' />
<@assertEquals actual=obj.varargs3(null, [], null) expected='varargs3(Object... xs = [null, [], null])' />
<@assertEquals actual=obj.varargs3(null, null, null) expected='varargs3(String... xs = [null, null, null])' />
<@assertEquals actual=obj.varargs3() expected='varargs3(String... xs = [])' />

<@assertEquals actual=obj.varargs4(null) expected='varargs4(Integer... xs = [null])' />
<@assertEquals actual=obj.varargs4(null, null, null) expected='varargs4(Integer... xs = [null, null, null])' />
<@assertEquals actual=obj.varargs4(1, null, 2) expected='varargs4(Integer... xs = [1, null, 2])' />
<@assertEquals actual=obj.varargs4(1) expected='varargs4(int... xs = [1])' />
<@assertEquals actual=obj.varargs4(1, 2, 3) expected='varargs4(int... xs = [1, 2, 3])' />

<@assertEquals actual=obj.varargs5(1, 2, 3, 4, 5) expected='varargs5(int a1 = 1, int a2 = 2, int a3 = 3, int... xs = [4, 5])' />
<@assertEquals actual=obj.varargs5(1, 2, 3, 4) expected='varargs5(int a1 = 1, int a2 = 2, int a3 = 3, int... xs = [4])' />
<@assertEquals actual=obj.varargs5(1, 2, 3) expected='varargs5(int a1 = 1, int a2 = 2, int a3 = 3, int... xs = [])' />
<@assertEquals actual=obj.varargs5(1, 2) expected='varargs5(int a1 = 1, int a2 = 2, int... xs = [])' />
<@assertEquals actual=obj.varargs5(1) expected='varargs5(int a1 = 1, int... xs = [])' />
<@assertEquals actual=obj.varargs5() expected='varargs5(int... xs = [])' />

<@assertEquals actual=obj.varargs6('s', 2) expected='varargs6(String a1 = s, int... xs = [2])' />
<@assertEquals actual=obj.varargs6('s') expected='varargs6(String a1 = s, int... xs = [])' />
<@assertEquals actual=obj.varargs6(1, 2) expected='varargs6(Object a1 = 1, int a2 = 2, int... xs = [])' />
<@assertFails message="no compatible overloaded">${obj.varargs6(1)}</@>

<@assertEquals actual=obj.varargs7(1?int, 2?int) expected='varargs7(int... xs = [1, 2])' />
<@assertEquals actual=obj.varargs7(1?short, 2?int) expected='varargs7(short a1 = 1, int... xs = [2])' />

<#-- Tests that a pre-2.3.21 bug is fixed now: -->
<@assertEquals actual=obj.mVarargsIgnoredTail(1, 2, 3) expected='mVarargsIgnoredTail(int... is = [1, 2, 3])' />
<@assertEquals actual=obj.mVarargsIgnoredTail(1, 2, 3.5) expected='mVarargsIgnoredTail(int i = 1, double... ds = [2.0, 3.5])' />

<@assertEquals actual=obj.mNullAmbiguous('a') expected='mNullAmbiguous(String s = a)' />
<@assertEquals actual=obj.mNullAmbiguous(123) expected='mNullAmbiguous(int i = 123)' />
<@assertEquals actual=obj.mNullAmbiguous(1.9) expected='mNullAmbiguous(int i = 1)' />
<@assertEquals actual=obj.mNullAmbiguous(1?double) expected='mNullAmbiguous(int i = 1)' />
<@assertFails message="no compatible overloaded">${obj.mNullAmbiguous(1.9?double)}</@>
<@assertFails message="multiple compatible overloaded">${obj.mNullAmbiguous(null)}</@>

<@assertFails message="multiple compatible overloaded">${obj.mNullAmbiguous2(null)}</@>

<@assertEquals actual=obj.mNullNonAmbiguous(null) expected='mNullNonAmbiguous(String s = null)' />

<#-- The primitive int-s will win twice, but then String wins over Object, which is stronger: -->
<@assertEquals actual=obj.mLowRankWins(1, 2, 'a') expected='mLowRankWins(Integer x = 1, Integer y = 2, String s = a)' />

<@assertEquals actual=obj.mRareWrappings(obj.file, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringWrappedAsBoolean)
               expected='mRareWrappings(File f = file, double d1 = 123.0002, Double d2 = 123.0002, double d3 = 123.0002, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.stringWrappedAsBoolean, obj.adaptedNumber, obj.adaptedNumber, obj.adaptedNumber, obj.stringAdaptedToBoolean)
               expected='mRareWrappings(String s = yes, double d1 = 123.0002, Double d2 = 123.0002, double d3 = 123.0002, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.stringAdaptedToBoolean2, obj.wrapperNumber, obj.wrapperNumber, obj.wrapperNumber, obj.stringAdaptedToBoolean2)
               expected='mRareWrappings(String s = yes, double d1 = 123.0001, Double d2 = 123.0001, double d3 = 123.0001, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, obj.booleanWrappedAsAnotherBoolean)
               expected='mRareWrappings(Object o = true, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.adaptedNumber, 0, 0, 0, !obj.booleanWrappedAsAnotherBoolean)
               expected='mRareWrappings(Object o = 124, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = true)' />
<@assertEquals actual=obj.mRareWrappings(obj.booleanWrappedAsAnotherBoolean, 0, 0, 0, !obj.stringAdaptedToBoolean)
               expected='mRareWrappings(Object o = true, double d1 = 0.0, Double d2 = 0.0, double d3 = 0.0, b = true)' />
               
<@assertEquals actual=obj.mRareWrappings2(obj.adaptedNumber) expected='mRareWrappings2(byte b = 124)' />

<#-- Test for List VS array problems due to too vague hinting: -->

<@assertEquals actual=obj.mSeqToArrayNonOverloaded(['a', 'b'], 'c') expected='mSeqToArrayNonOverloaded(String[] [a, b], String c)' />

<@assertEquals actual=obj.mSeqToArrayGoodHint(['a', 'b'], 'c') expected='mSeqToArrayGoodHint(String[] [a, b], String c)' />
<@assertEquals actual=obj.mSeqToArrayGoodHint(['a', 'b'], 3) expected='mSeqToArrayGoodHint(String[] [a, b], int 3)' />

<@assertEquals actual=obj.mSeqToArrayGoodHint2(['a', 'b'], 'c') expected='mSeqToArrayGoodHint2(String[] [a, b], String c)' />
<@assertEquals actual=obj.mSeqToArrayGoodHint2('a') expected='mSeqToArrayGoodHint2(String a)' />

<@assertEquals actual=obj.mSeqToArrayPoorHint(['a', 'b'], 'c') expected='mSeqToArrayPoorHint(String[] [a, b], String c)' />
<@assertEquals actual=obj.mSeqToArrayPoorHint('a', 2) expected='mSeqToArrayPoorHint(String a, int 2)' />

<@assertEquals actual=obj.mSeqToArrayPoorHint2(['a', 'b']) expected='mSeqToArrayPoorHint2(String[] [a, b])' />
<@assertEquals actual=obj.mSeqToArrayPoorHint2('a') expected='mSeqToArrayPoorHint2(String a)' />

<@assertFails message="multiple compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint3(['a', 'b']) expected='mSeqToArrayPoorHint3(String[] [a, b])' /></@>
<@assertFails message="multiple compatible overloaded"><@assertEquals actual=obj.mSeqToArrayPoorHint3([1, 2]) expected='mSeqToArrayPoorHint3(int[] [a, b])' /></@>

<@assertEquals actual=obj.mStringArrayVsListPreference(['a', 'b']) expected="mStringArrayVsListPreference(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVsListPreference(obj.javaObjectArray) expected="mStringArrayVsListPreference(List [a, b])" />
<@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(['a', 'b']) expected="mStringArrayVsObjectArrayPreference(Object[] [a, b])" />
<@assertEquals actual=obj.mIntArrayVsIntegerArrayPreference([1, 2]) expected="mIntArrayVsIntegerArrayPreference(Integer[] [1, 2])" />

<#if dowPre22>
  <@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaStringArray) expected="mStringArrayVsObjectArrayPreference(Object[] [a, b])" />
  <@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaIntArray) expected="mStringArrayVsObjectArrayPreference(Object[] [11, 22])" />
<#else>
  <@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaStringArray) expected="mStringArrayVsObjectArrayPreference(String[] [a, b])" />
  <@assertFails message="no compatible overloaded">${obj.mStringArrayVsObjectArrayPreference(obj.javaIntArray)}</@>
</#if>
<@assertEquals actual=obj.mStringArrayVsObjectArrayPreference(obj.javaIntegerArray) expected="mStringArrayVsObjectArrayPreference(Object[] [11, 22])" />

<@assertEquals actual=obj.mIntegerArrayOverloaded([1, 2], 3) expected="mIntegerArrayOverloaded(Integer[] [1, 2], int 3)" />
<@assertEquals actual=obj.mIntegerArrayOverloaded([1?byte, 2?byte], 3) expected="mIntegerArrayOverloaded(Integer[] [1, 2], int 3)" />
<@assertEquals actual=obj.mIntegerArrayOverloaded(obj.javaIntegerList, 3) expected="mIntegerArrayOverloaded(Integer[] [1, 2], int 3)" />
<@assertEquals actual=obj.mIntegerArrayOverloaded(obj.javaByteList, 3) expected="mIntegerArrayOverloaded(Integer[] [1, 2], int 3)" />

<@assertEquals actual=obj.mStringArrayOverloaded(['a', 'b'], 3) expected="mStringArrayOverloaded(String[] [a, b], int 3)" />
<@assertEquals actual=obj.mStringArrayOverloaded(obj.javaStringList, 3) expected="mStringArrayOverloaded(String[] [a, b], int 3)" />
<@assertEquals actual=obj.mStringArrayOverloaded(obj.javaCharacterList, 3) expected="mStringArrayOverloaded(String[] [c, C], int 3)" />
<@assertFails message="Failed to convert sequence">${obj.mStringArrayOverloaded([1, 2], 3)}</@>
<@assertFails message="Failed to convert">${obj.mStringArrayOverloaded(obj.javaIntegerList, 3)}</@>

<@assertEquals actual=obj.mCharArrayOverloaded(['a', 'b'], 3) expected="mCharArrayOverloaded(char[] [a, b], int 3)" />
<@assertEquals actual=obj.mCharArrayOverloaded(obj.javaCharacterList, 3) expected="mCharArrayOverloaded(char[] [c, C], int 3)" />
<@assertEquals actual=obj.mCharArrayOverloaded(obj.javaStringList, 3) expected="mCharArrayOverloaded(char[] [a, b], int 3)" />
<@assertFails message="Failed to convert sequence">${obj.mCharArrayOverloaded(['aa', 'bb'], 3)}</@>
<@assertFails message="Failed to convert">${obj.mCharArrayOverloaded(obj.javaString2List, 3)}</@>
<@assertEquals actual=obj.mCharArrayOverloaded(['a', 'b'], 's') expected="mCharArrayOverloaded(Character[] [a, b], String s)" />
<@assertEquals actual=obj.mCharArrayOverloaded(obj.javaCharacterList, 's') expected="mCharArrayOverloaded(Character[] [c, C], String s)" />
<@assertEquals actual=obj.mCharArrayOverloaded(obj.javaStringList, 's') expected="mCharArrayOverloaded(Character[] [a, b], String s)" />
<@assertFails message="Failed to convert sequence">${obj.mCharArrayOverloaded(['aa', 'bb'], 's')}</@>
<@assertFails message="Failed to convert">${obj.mCharArrayOverloaded(obj.javaString2List, 's')}</@>

<@assertEquals actual=obj.mStringArrayArrayOverloaded([['a', 'b'], ['c']], 3) expected="mStringArrayArrayOverloaded(String[][] [[a, b], [c]], int 3)" />
<@assertEquals actual=obj.mStringArrayArrayOverloaded(obj.javaStringListList, 3) expected="mStringArrayArrayOverloaded(String[][] [[a, b], [c]], int 3)" />
<@assertEquals actual=obj.mStringArrayArrayOverloaded(obj.javaStringSequenceList, 3) expected="mStringArrayArrayOverloaded(String[][] [[a, b], [c]], int 3)" />
<@assertFails message="Failed to convert">${obj.mStringArrayArrayOverloaded(obj.javaStringList, 3)}</@>
<@assertFails message="Failed to convert">${obj.mStringArrayArrayOverloaded(obj.javaIntegerListList, 3)}</@>
<@assertEquals actual=obj.mIntArrayArrayOverloaded(obj.javaListOfIntArrays) expected="mIntArrayArrayOverloaded([[1, 2, 3], [], [4]])" />
<@assertEquals actual=obj.mArrayOfListsOverloaded(obj.javaListOfIntArrays) expected="mArrayOfListsOverloaded([[1, 2, 3], [], [4]])" />
<@assertEquals actual=obj.mIntArrayArrayNonOverloaded(obj.javaListOfIntArrays) expected="mIntArrayArrayNonOverloaded([[1, 2, 3], [], [4]])" />
<@assertEquals actual=obj.mArrayOfListsNonOverloaded(obj.javaListOfIntArrays) expected="mArrayOfListsNonOverloaded([[1, 2, 3], [], [4]])" />

<@assertEquals actual=obj.mStringArrayVarargsOverloaded2(['a', 'b']) expected="mStringArrayVarargsOverloaded2(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded2(obj.javaStringList) expected="mStringArrayVarargsOverloaded2(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded2(obj.javaStringArray) expected="mStringArrayVarargsOverloaded2(String[] [a, b])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded2(['a']) expected="mStringArrayVarargsOverloaded2(String[] [a])" />

<#-- Situations that lead to array-to-List conversion: -->
<@assertEquals actual=obj.mListOrString(obj.javaStringArray) expected="mListOrString(List [a, b])" />
<@assertEquals actual=obj.mListOrString(obj.javaEmptyStringArray) expected="mListOrString(List [])" />
<@assertEquals actual=obj.mListOrString(obj.javaIntArray) expected="mListOrString(List [11, 22])" />
<@assertEquals actual=obj.mListListOrString(obj.javaStringArrayArray) expected="mListListOrString(List [[a, b], [], [c]])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringArray) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringList, obj.javaStringArray) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />
<@assertEquals actual=obj.mStringArrayVarargsOverloaded4(obj.javaStringArray, obj.javaStringList) expected="mStringArrayVarargsOverloaded4(List[] [[a, b], [a, b]])" />

<@assertEquals actual=obj.mMapOrBoolean(obj.hashAndScalarModel) expected="mMapOrBoolean(Map {})" />
<@assertEquals actual=obj.mMapOrBoolean(obj.booleanAndScalarModel) expected="mMapOrBoolean(boolean true)" />
<@assertEquals actual=obj.mMapOrBoolean(obj.allModels) expected="mMapOrBoolean(boolean true)" />

<@assertEquals actual=obj.mMapOrBooleanVarargs(obj.hashAndScalarModel) expected="mMapOrBooleanVarargs(Map... [{}])" />
<@assertEquals actual=obj.mMapOrBooleanVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel) expected="mMapOrBooleanVarargs(Map... [{}, {}])" />
<@assertEquals actual=obj.mMapOrBooleanVarargs(obj.allModels) expected="mMapOrBooleanVarargs(boolean... [true])" />
<@assertEquals actual=obj.mMapOrBooleanVarargs(obj.allModels, obj.allModels) expected="mMapOrBooleanVarargs(boolean... [true, true])" />

<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel) expected="mMapOrBooleanFixedAndVarargs(Map {})" />
<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel) expected="mMapOrBooleanFixedAndVarargs(Map... [{}, {}])" />
<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.hashAndScalarModel, obj.hashAndScalarModel, obj.hashAndScalarModel) expected="mMapOrBooleanFixedAndVarargs(Map... [{}, {}, {}])" />
<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.allModels) expected="mMapOrBooleanFixedAndVarargs(boolean true)" />
<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.allModels, obj.allModels) expected="mMapOrBooleanFixedAndVarargs(boolean... [true, true])" />
<@assertEquals actual=obj.mMapOrBooleanFixedAndVarargs(obj.allModels, obj.allModels, obj.allModels) expected="mMapOrBooleanFixedAndVarargs(boolean... [true, true, true])" />

<@assertEquals actual=obj.mNumberOrArray(obj.allModels) expected="mNumberOrArray(Number 1)" />
<@assertEquals actual=obj.mNumberOrArray([obj.allModels]) expected="mNumberOrArray(Object[] [1])" />
<@assertEquals actual=obj.mIntOrArray(obj.allModels) expected="mIntOrArray(int 1)" />
<@assertEquals actual=obj.mDateOrArray(obj.allModels) expected="mDateOrArray(Date 0)" />
<@assertEquals actual=obj.mStringOrArray(obj.allModels) expected="mStringOrArray(String s)" />
<@assertEquals actual=obj.mBooleanOrArray(obj.allModels) expected="mBooleanOrArray(boolean true)" />
<@assertEquals actual=obj.mMapOrArray(obj.allModels) expected="mMapOrArray(Map {})" />
<@assertEquals actual=obj.mListOrArray(obj.allModels) expected="mListOrArray(List [])" />
<@assertEquals actual=obj.mSetOrArray(obj.allModels) expected="mSetOrArray(Set [])" />

<@assertEquals actual=obj.mCharOrCharacterOverloaded('c') expected="mCharOrCharacterOverloaded(char c)" />
<@assertEquals actual=obj.mCharOrCharacterOverloaded(obj.javaString) expected="mCharOrCharacterOverloaded(char s)" />
<@assertEquals actual=obj.mCharOrCharacterOverloaded(null) expected="mCharOrCharacterOverloaded(Character null)" />

<@assertEquals actual=obj.mCharOrBooleanOverloaded('c') expected="mCharOrBooleanOverloaded(char c)" />
<@assertEquals actual=obj.mCharOrBooleanOverloaded(true) expected="mCharOrBooleanOverloaded(boolean true)" />

<@assertEquals actual=obj.mCharOrStringOverloaded('c', true) expected="mCharOrStringOverloaded(char c, boolean true)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded('c', true) expected="mCharacterOrStringOverloaded(Character c, boolean true)" />

<@assertEquals actual=obj.mCharOrStringOverloaded2('c') expected="mCharOrStringOverloaded2(char c)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded2('c') expected="mCharacterOrStringOverloaded2(Character c)" />
<@assertEquals actual=obj.mCharOrStringOverloaded2('ss') expected="mCharOrStringOverloaded2(String ss)" />
<@assertEquals actual=obj.mCharacterOrStringOverloaded2('ss') expected="mCharacterOrStringOverloaded2(String ss)" />

<#-- The exmple given in bug report 363 -->
<#assign theMap = {'name':'Billy', 'lastName', 'Pilgrim'} />
<@assertEquals actual=obj.bugReport363(theMap, []) expected="Executed: testMethod(Map fields, List listField) on input: fields={name=Billy, lastName=Pilgrim} and listField=[]" />
<@assertEquals actual=obj.bugReport363(theMap, null) expected="Executed: testMethod(Map fields, List listField) on input: fields={name=Billy, lastName=Pilgrim} and listField=null" />
