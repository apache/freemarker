<#macro m>m{<#return>}</#macro>
<@m/>

<#macro m><#if true>m{<#return>}</#if></#macro>
<@m/>

<#macro m><#if true>m{<#return></#if>}</#macro>
<@m/>

<#macro b>b{<#nested>}</#macro>
<#macro m><@b><#return></@></#macro>
<@m/>

<#macro b>b{<#nested>}</#macro>
<#macro m>m:<@b><#return></@></#macro>
<@m/>