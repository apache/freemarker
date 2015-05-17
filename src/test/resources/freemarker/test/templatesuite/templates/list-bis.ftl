<#list ['a', 'b', 'c'] as x>
    ${x?index}: ${x} [<#list ['A', 'B', 'C'] as x>${x?index}:${x}<#sep>, </#list>]
</#list>

<#list ['a', 'b', 'c'] as i>
    ${i?index}: ${i} <#list ['A', 'B', 'C'] as j>${i?index}${i}/${j?index}${j}<#sep>, </#list>
</#list>

<#list ['a', 'b', 'c']><#items as x>${x?index}:${x}<#sep>, </#items></#list>
<#list ['a', 'b', 'c']><#items as x>${x?counter}. ${x};<#sep> </#items></#list>

<#list ['a', 'b', 'c'] as x>${x}<#if x?hasNext>, </#if></#list>
<#list ['a', 'b', 'c'] as x\-y>${x\-y}<#if x\-y?hasNext>, </#if></#list>

<#list ['a', 'b', 'c'] as x><#if x?isFirst>${x?capFirst}<#else>${x}</#if><#sep>, </#sep><#if x?isLast>.</#if></#list>

<#list ['a', 'b', 'c'] as x>${x?isOddItem?c}/${x?isEvenItem?c}<#sep> </#list>

<#list ['a', 'b', 'c'] as x>
    <td class="${x?itemParity}Row">${x}</td>
</#list>

<#list ['a', 'b', 'c'] as x>
    <td class="row${x?itemParityCap}">${x}</td>
</#list>

<#list ['a', 'b', 'c', 'd', 'e', 'f', 'g'] as x>
    <td class="${x?itemCycle('R', 'G', 'B')}">${x}</td>
</#list>
<@assertFails message="expects 1"><#list 1..1 as x>${x?itemCycle()}</#list></@>