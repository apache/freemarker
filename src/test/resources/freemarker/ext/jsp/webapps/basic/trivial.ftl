${n + 1}

<#-- Instead of JSTL: -->

<#if t>
  True
</#if>

<#if n == 123>
  Do this
<#else>
  Do that
</#if>

<#list ls as i>
- ${i}
</#list>

[${" foo "?trim}]
