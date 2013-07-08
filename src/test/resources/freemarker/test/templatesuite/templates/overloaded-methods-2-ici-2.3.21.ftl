<#-- Since with IcI 2.3.21+ the results must not depend on method introspection order, there's only once template -->

<@assertEquals actual=obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c')) expected='mVarargs(String... a1 = abc)' />

<@assertEquals actual=obj.mNull1(null) expected="mNull1(String a1 = null)" />
<@assertEquals actual=obj.mNull1(123) expected="mNull1(int a1 = 123)" />
<@assertEquals actual=obj.mNull2(null) expected="mNull2(String a1 = null)" />
<@assertEquals actual=obj.mVarargs('a', null) expected="mVarargs(String... a1 = anull)" />
<@assertFails message="multiple compatible overloaded">${obj.mVarargs(null, 'a')}</@>
<@assertFails message="multiple compatible overloaded">${obj.mSpecificity('a', 'b')}</@>
<#-- TODO No yet fixed: 
<@assertEquals actual=obj.mChar('a') expected='mChar(char a1 = a)' />
<@assertEquals actual=obj.mBoolean(true) expected="mBoolean(boolean a1 = true)" />
-->