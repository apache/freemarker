<@assertFails message="no compatible overloaded">${obj.mVarargs('a', obj.getNnS('b'), obj.getNnS('c'))}</@>
<@assertFails message="no compatible overloaded">${obj.mChar('a')}</@>

<#include 'overloaded-methods-2-ici-2.3.20.ftl'>
