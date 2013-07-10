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
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mNumBoxedVSBoxed(123.5) expected=123 /></@>
<@assertFails message="no compatible overloaded"><@assertEquals actual=obj.mNumBoxedVSBoxed(123?int) expected=123 /></@>
<@assertEquals actual=obj.mNumBoxedVSBoxed(123?long) expected="mNum(Long a1 = 123)" />
<@assertEquals 
    actual=obj.mNumUnambigous(2147483648) expected="mNumUnambigous(Integer a1 = -2147483648)" /> <#-- overflow -->
