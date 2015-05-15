<#assign animals = ["aardvark", "bear", "cat", "dog"]>
<#assign animal = ["aardvark"]>
<#assign nothing = []>

<@testList animals />

<@testList animal />

<@testList nothing />

<@testList factory.list />

<@testList factory.set />

<@testList factory.getIterator />

<@testList factory.emptyList />

<@testList factory.emptySet />

<@testList factory.getEmptyIterator />

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