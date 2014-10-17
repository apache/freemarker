<#setting number_format = ",##0.##">
<#setting locale = "fr_FR">
${1}
${1?c}
${1234567.886}
${1234567.886?c}
<#setting number_format = "0.00">
${1}
${1?c}
${1234567.886}
${1234567.886?c}
${int?c}
${double?c}
${double2?c}
${double3?c}
${double4?c}
${bigDecimal?c}
${bigDecimal2?c}
<#if iciIntValue gte 2003021>
  <@assertEquals expected="INF" actual="INF"?number?c />
  <@assertEquals expected="INF" actual="INF"?number?c />
  <@assertEquals expected="-INF" actual="-INF"?number?c />
  <@assertEquals expected="-INF" actual="-INF"?number?float?c />
  <@assertEquals expected="NaN" actual="NaN"?number?float?c />
  <@assertEquals expected="NaN" actual="NaN"?number?float?c />
<#else>
  <#setting locale = "en_US">
  <#setting number_format = "0.#">
  <@assertEquals expected="INF"?number?string actual="INF"?number?c />
  <@assertEquals expected="-INF"?number?string actual="-INF"?number?c />
  <@assertEquals expected="NaN"?number?string actual="NaN"?number?c />
</#if>