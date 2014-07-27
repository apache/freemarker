<#-- Mostly just checks if the expressions doesn't fail -->
<#assign works = .data_model>
<#attempt>
  ${noSuchVariableExists}
<#recover>
  <#assign works = .error>
</#attempt>
<#assign works = .globals>
${.lang} == en
${.locale} == en_US
<#assign works = .locals!>
<#assign works = .main>
<#assign works = .node!>
${.output_encoding?lower_case} == utf-8
${.template_name} == specialvars.ftl
${.url_escaping_charset?lower_case} == iso-8859-1
<#assign foo = "x">
${.vars['foo']} == x
<#assign works = .version>
${.now?is_datetime?c} == true