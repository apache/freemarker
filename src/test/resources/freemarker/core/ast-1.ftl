1 <@foo x=1 y=2; b1, b2>x</@foo>
2 <@ns.bar 1 2; b1, b2>y</@>
3 <#assign x = 123><#assign x = 123 in ns><#global x = 123>
4 <#if x + 1 == 0>foo${y}bar<#else>${"static"}${'x${baaz * 10}y'}</#if>
5 <#switch x><#case 1>one<#case 2>two<#default>more</#switch>
6 <#macro foo x y=2 z=y+1 q...><#nested x y></#macro>
7 <#function foo x y><#local x = 123><#return 1></#function>
8 <#list xs as x></#list>
9 <#list xs>[<#items as x>${x}<#sep>, </#items>]<#else>None</#list>
10 <#-- A comment -->