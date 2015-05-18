<#setting boolean_format="1,0">
StNuBoMeTaMaHaHxSeCoCxEnInDiNo
<#foreach x in [
  "a", 1, false,
  testmethod, testmacro, html_escape,
  {"a":1}, [1], testcollection, testcollectionEx,
  testnode,
  bean, bean.m, bean.mOverloaded
]>
  ${x?is_string} <#t>
  ${x?is_number} <#t>
  ${x?is_boolean} <#t>
  ${x?is_method} <#t>
  ${x?is_macro} <#t>
  ${x?is_transform} <#t>
  ${x?is_hash} <#t>
  ${x?is_hash_ex} <#t>
  ${x?is_sequence} <#t>
  ${x?is_collection} <#t>
  ${x?is_collection_ex} <#t>
  ${x?is_enumerable} <#t>
  ${x?is_indexable} <#t>
  ${x?is_directive} <#t>
  ${x?is_node}<#lt>
</#foreach>
<#macro testmacro></#macro>