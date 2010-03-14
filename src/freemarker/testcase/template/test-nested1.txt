<#macro repeat count>
  <#local y = "test">
  <#foreach x in 1..count>
    ${y} ${count}/${x}: <#nested x, "asdf"> <#-- the second body parameter is not used below -->
  </#foreach>
</#macro>
<@repeat count=3>${y?default("undefined")} ${x?default("undefined")} ${count?default("undefined")}</@repeat>
<#global x = "X">
<#global y = "Y">
<#global count = "Count">
<@repeat count=3 ; param1>${y?default("undefined")} ${x?default("undefined")} ${count?default("undefined")} ${param1}</@repeat>