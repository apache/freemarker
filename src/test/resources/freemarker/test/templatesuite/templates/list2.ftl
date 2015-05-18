<@testList ["aardvark", "bear", "cat", "dog"] />

<@testList ["aardvark"] />

<@testList [] />

<@testList listables.list />

<@testList listables.set />

<@testList listables.getIterator />

<@testList listables.emptyList />

<@testList listables.emptySet />

<@testList listables.getEmptyIterator />

<#macro testList xs>
=== [${resolve(xs)?join(", ")}] ===
<#assign resolveCallCnt = 0>

-- List+sep:
<#list resolve(xs) as x>
    ${x}<#sep>,</#sep>
</#list>
-- List+else:
<#list resolve(xs) as x>
    ${x}
<#else>
    Empty!
</#list>
-- List+items:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}
    </#items>
    ]
</#list>
-- List+items+else:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}
    </#items>
    ]
<#else>
    Empty!
</#list>
-- List+items+sep+else:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}<#sep>,</#sep>
    </#items>
    ]
<#else>
    Empty!
</#list>
<@assertEquals expected=5 actual=resolveCallCnt />
--
</#macro>

<#function resolve xs>
    <#assign resolveCallCnt = (resolveCallCnt!0) + 1>
    <#if xs?isMethod>
        <#return xs()>
    <#else>
        <#return xs>
    </#if>
</#function>