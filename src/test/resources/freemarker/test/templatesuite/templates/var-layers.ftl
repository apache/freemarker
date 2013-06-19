<#import "varlayers_lib.ftl" as lib>
<@foo 1/>
${x} = ${.data_model.x} = ${.globals.x}
<#assign x = 5>
${x} = ${.main.x} = ${.namespace.x}
<#global x = 6>
${.globals.x} but ${.data_model.x} = 4
${y} = ${.globals.y} = ${.data_model.y?default("ERROR")}
Invisiblity test 1.: <#if .main.y?exists || .namespace.y?exists>failed<#else>passed</#if>
Invisiblity test 2.: <#if .main.z?exists || .namespace.z?exists>failed<#else>passed</#if>
Invisiblity test 3.: <#global q = 1><#if .main.q?exists || .namespace.q?exists || .data_model.q?exists>failed<#else>passed</#if>
--
<@lib.foo/>
--
<#macro foo x>
  ${x} = ${.locals.x}
  <#local x = 2>
  ${x} = ${.locals.x}
  <#local y = 3>
  ${y} = ${.locals.y}
</#macro>