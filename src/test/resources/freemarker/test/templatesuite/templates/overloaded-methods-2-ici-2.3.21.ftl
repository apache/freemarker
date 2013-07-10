<#-- Since with IcI 2.3.21+ the results must not depend on method introspection order, there's only once template -->

<@assertEquals actual=obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c')) expected='mVarargs(String... a1 = abc)' />

<@assertEquals actual=obj.mNull1(null) expected="mNull1(String a1 = null)" />
<@assertEquals actual=obj.mNull1(123) expected="mNull1(int a1 = 123)" />
<@assertEquals actual=obj.mNull2(null) expected="mNull2(String a1 = null)" />
<@assertEquals actual=obj.mVarargs('a', null) expected="mVarargs(String... a1 = anull)" />
<@assertFails message="multiple compatible overloaded">${obj.mVarargs(null, 'a')}</@>
<@assertFails message="multiple compatible overloaded">${obj.mSpecificity('a', 'b')}</@>

<@assertEquals actual=obj.mChar('a') expected='mChar(char a1 = a)' />
<@assertEquals actual=obj.mBoolean(true) expected="mBoolean(boolean a1 = true)" />
<@assertEquals actual=obj.mBoolean(null) expected="mBoolean(Boolean a1 = null)" />

<@assertEquals actual=obj.mIntNonOverloaded(123?long) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(123.5) expected=123 />
<@assertEquals actual=obj.mIntNonOverloaded(2147483648) expected=-2147483648 /> <#-- FIXME overflow -->
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mNumBoxedVSBoxed(123.5) expected=123 /></@><#-- FIXME -->
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mNumBoxedVSBoxed(123?int) expected=123 /></@><#-- FIXME -->
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?long) expected="mNumBoxedVSBoxed(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?short) expected="mNumBoxedVSBoxed(Short a1 = 123)" />
<@assertEquals 
    actual=obj.mNumUnambigous(2147483648) expected="mNumUnambigous(Integer a1 = -2147483648)" /> <#-- FIXME overflow -->

<@assertEquals actual=obj.mIntPrimVSBoxed(123?int) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123?short) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123) expected="mIntPrimVSBoxed(int a1 = 123)" />
<#-- This doesn't fail as the hint will be Integer, and thus unwrapping will truncate the Long to that: -->
<@assertEquals actual=obj.mIntPrimVSBoxed(123?long) expected="mIntPrimVSBoxed(int a1 = 123)" />

<@assertEquals actual=obj.mNumPrimVSPrim(123?short) expected="mNumPrimVSPrim(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?int) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimVSPrim(123?long) expected="mNumPrimVSPrim(long a1 = 123)" />
<@assertFails message="no compatible overloaded">${obj.mNumPrimVSPrim(123?double)}</@><#-- FIXME? -->
<@assertEquals actual=obj.mNumPrimVSPrim(123456) expected="mNumPrimVSPrim(short a1 = -7616)" /><#-- FIXME overflow, had to choose long -->

<@assertEquals actual=obj.mNumPrimAll(123?byte) expected="mNumPrimAll(byte a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?short) expected="mNumPrimAll(short a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?int) expected="mNumPrimAll(int a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?long) expected="mNumPrimAll(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimAll(123?float) expected="mNumPrimAll(float a1 = 123.0)" />
<@assertEquals actual=obj.mNumPrimAll(123?double) expected="mNumPrimAll(double a1 = 123.0)" />
<@assertFails message="multiple compatible overloaded">${obj.mNumPrimAll(123)}</@><#-- FIXME -->
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

<#-- FIXME
<@assertEquals actual=obj.mNumBoxedAll2nd(123?byte) expected="mNumBoxedAll2nd(Short a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?short) expected="mNumBoxedAll2nd(Short a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?int) expected="mNumBoxedAll2nd(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?long) expected="mNumBoxedAll2nd(Long a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?float) expected="mNumBoxedAll2nd(Double a1 = 123.0)" />
<@assertEquals actual=obj.mNumBoxedAll2nd(123?double) expected="mNumBoxedAll2nd(Double a1 = 123.0)" />
 -->
 
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?int) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?long) expected="mNumPrimFallbackToNumber(long a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123?double) expected="mNumPrimFallbackToNumber(Number a1 = 123.0)" />
<#-- FIXME
<@assertEquals actual=obj.mNumPrimFallbackToNumber(123) expected="mNumPrimFallbackToNumber(Number a1 = 123)" />
-->
<@assertEquals actual=obj.mNumPrimFallbackToNumber(obj.bigInteger(123)) expected="mNumPrimFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumPrimFallbackToNumber('x') expected="mNumPrimFallbackToNumber(Object a1 = x)" />

<#-- FIXME
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?int) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
-->
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?long) expected="mNumBoxedFallbackToNumber(Long a1 = 123)" />
<#-- FIXME
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123?double) expected="mNumBoxedFallbackToNumber(Number a1 = 123.0)" />
-->
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(123) expected="mNumBoxedFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber(obj.bigInteger(123)) expected="mNumBoxedFallbackToNumber(Number a1 = 123)" />
<@assertEquals actual=obj.mNumBoxedFallbackToNumber('x') expected="mNumBoxedFallbackToNumber(Object a1 = x)" />

<#-- FIXME
<@assertEquals actual=obj.mDecimalLoss(1.5) expected="mDecimalLoss(double a1 = 1.5)" />
-->
<@assertEquals actual=obj.mDecimalLoss(1.5?double) expected="mDecimalLoss(double a1 = 1.5)" />