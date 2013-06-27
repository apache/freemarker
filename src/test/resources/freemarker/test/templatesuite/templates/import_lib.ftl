<#macro test foo>
  Test ${foo}.
  Email: ${mail}
  <#if .main.mail?exists>
    Email in the root: ${.main.mail}
  </#if>
</#macro>

<#function doubleUp foo>
   <#return foo+foo>
</#function>

<#assign mail = "jsmith@acme.com">