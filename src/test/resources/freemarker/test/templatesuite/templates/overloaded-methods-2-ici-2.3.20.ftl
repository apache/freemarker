<#-- The parts of the IcI 2.3.20 tests that give the same result regardless of method introspection order -->

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