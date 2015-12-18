<#if exp></#if>
<#if exp>1</#if>
<#if exp>${1}2</#if>
<#if exp><#else></#if>
<#if exp>1<#else>1</#if>
<#if exp>${1}2<#else>${1}2</#if>
<#if exp><#elseif exp></#if>
<#if exp><#elseif exp>1</#if>
<#attempt><#recover></#attempt>
<#attempt>1<#recover>1</#attempt>
<#list s as i></#list>
<#list s as i>1</#list>
<#list s as i><#sep></#list>
<#list s as i>1<#sep>1</#list>
<#list s><#items as i><#sep></#items></#list>
<#list s>1<#items as i>1<#sep>1</#items>1</#list>
1
${x + y}