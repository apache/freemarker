1 <#assign x = 1>
2 <#assign x = 1, y = 2>
3 <#assign x = 1 in ns>
4 <#assign x = 1, y = 2 in ns>
5 <#global x = 1>
6 <#global x = 1, y = 2>
<#macro m>
  7 <#local x = 1>
  8 <#local x = 1, y = 2>
</#macro>
9 <#assign a += 1, b -= 2, c *= 3, d /= 4, e %= 5, f++, g-->
