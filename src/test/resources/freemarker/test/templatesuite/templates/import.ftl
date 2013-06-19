--
<#-- import "/import_lib.ftl" as my -->
--

<#if mail?exists || test?exists>
  <#stop "mail or test should not exist">
</#if>

${my.mail}
<@my.test foo="bar"/>

<#assign mail="jsmith@other1.com">
${my.mail}
<@my.test foo="bar"/>

<#assign mail in my>
  jsmith@other2.com<#t>
</#assign>
${my.mail}
<@my.test foo="bar"/>

<#import "/import_lib.ftl" as my2>
${my2.mail}
<#assign mail="jsmith@other3.com" in my2>
${my.mail}

${my2.doubleUp("foobar")}