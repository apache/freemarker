StNuBoMeTaMaHaHxSeCoLiInDiNo
<#foreach x in [
  "a", 1, false,
  testmethod, testmacro, html_escape,
  {"a":1}, [1], testcollection, testnode
]>
<#if x?is_string>1<#else>0</#if> <#if x?is_number>1<#else>0</#if> <#if x?is_boolean>1<#else>0</#if> <#if x?is_method>1<#else>0</#if> <#if x?is_macro>1<#else>0</#if> <#if x?is_transform>1<#else>0</#if> <#if x?is_hash>1<#else>0</#if> <#if x?is_hash_ex>1<#else>0</#if> <#if x?is_sequence>1<#else>0</#if> <#if x?is_collection>1<#else>0</#if> <#if x?is_enumerable>1<#else>0</#if> <#if x?is_indexable>1<#else>0</#if> <#if x?is_directive>1<#else>0</#if> <#if x?is_node>1<#else>0</#if>
</#foreach>
<#macro testmacro></#macro>
