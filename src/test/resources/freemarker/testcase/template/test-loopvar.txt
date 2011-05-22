<#setting locale="en_US">
---
<#macro myLoop from to>
  <#list from..to as x>
  - <#nested x></#list>*
</#macro>
<#list 2..1 as i>
  ${i}
  <@myLoop from=1 to=3; i>
    L1 ${i}
    <@myLoop from=1 to=2; i>
      L2 ${i}: <#list 1..3 as i>${i}; </#list>
    </@>
  </@>
</#list>
---
<#macro repeat count>
  <#list 1..count as x>
    <#nested x, x/2, x==count>
  </#list>
</#macro>
<#macro test2>
<#local c = 123>
<@repeat count=4 ; c, halfc, last>
  <#local c = .locals.c + 0.1>
  ${c} ${halfc}<#if last> Last!</#if>
</@repeat>
${c}
</#macro>
<@test2/>
---