<#assign x1 = .data_model.x>
<#assign x2 = x>
<#assign z2 = z>
<#macro foo>
<@.main.foo 1/>
  ${z} = ${z2} = ${x1} = ${.data_model.x}
  5
  ${x} == ${.globals.x}
  ${y} == ${.globals.y} == ${.data_model.y?default("ERROR")}
</#macro>
