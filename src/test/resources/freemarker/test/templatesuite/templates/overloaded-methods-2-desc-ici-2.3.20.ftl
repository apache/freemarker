<@assertFails message="no compatible overloaded">${obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c'))}</@>
<@assertFails message="no compatible overloaded">${obj.mChar('a')}</@>
<@assertFails message="no compatible overloaded">${obj.mIntPrimVSBoxed(123?long)}</@>
<@assertEquals actual=obj.mIntPrimVSBoxed(123?short) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.mIntPrimVSBoxed(123) expected="mIntPrimVSBoxed(int a1 = 123)" />
<@assertEquals actual=obj.varargs4(1, 2, 3) expected='varargs4(int... xs = [1, 2, 3])' />

<#include 'overloaded-methods-2-ici-2.3.20.ftl'>
