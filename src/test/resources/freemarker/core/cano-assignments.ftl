<#ftl stripWhitespace=false>
<#assign x = 1>
<#assign x = 1, y = 2>
<#assign x = 1 in ns>
<#assign x = 1, y = 2 in ns>
<#assign a += b + c>
<#assign a += 1, b -= 2, c *= 3, d /= 4, e %= 5, f++, g-->
<#global x = 1>
<#global x = 1, y = 2>
<#global x++>
<#macro m>
  <#local x = 1>
  <#local x = 1, y = 2>
</#macro>
<#assign x>
  foo ${bar}
</#assign>