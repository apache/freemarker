[${noSuchVar}] [${noSuchVar!'null'}]
[${noSuchVar.foo.bar}] [${noSuchVar.foo.bar!'null'}]
[${noSuchVar['foo']}] [${noSuchVar['foo']!'null'}]
<#assign b = 22>
<#macro foo a b c>
  ${a?default("A")} ${b?default("B")} ${c?default("C")}
</#macro>
<#call foo 1 wrong wrong>
<@foo 1 wrong wrong />
<#assign m = {"a": wrong, "b": wrong?default("null2")}>${m.a?default("null1")} ${m.b}
<#assign xs = [1, wrong, 2]><#list xs as x>${x?default("null")} </#list>
[${true}] [${false}]
[${beanTrue}] [${beanFalse}]
${beansArray?substring(0, 18)}  <- All BeanModel-s were strings; not anymore
${beansArray?string?substring(0, 18)}
${beansArray?replace('j.v.', 'cofe', 'r')?substring(0, 18)}
${beansArray?seq_index_of("b")}

<#list ['a', 'b'] as x>${x}</#list>
<#list ['a'] as x>${x}</#list>
<#list 'a' as x>${x}</#list>
<#list 'a' as x>${x}<#break>b</#list>

<#assign x = 1>
<#assign x = x + 1>
${x}
<#assign x = x + noSuchVar>
${x}
<#assign x += noSuchVar>
${x}
<@assertFails messageRegexp="(?s).*null or missing.*"><#assign noSuchVar = noSuchVar - 1></@>
<@assertFails messageRegexp="(?s)(?=.*noSuchVar).*assignment.*null or missing.*"><#assign noSuchVar -= 1></@>
<@assertFails messageRegexp="(?s)(?=.*noSuchVar).*assignment.*null or missing.*"><#assign noSuchVar--></@>
<@assertFails messageRegexp="(?s)(?=.*noSuchVar).*assignment.*null or missing.*"><#assign noSuchVar++></@>
<#assign noSuchVar = noSuchVar + 1>
${noSuchVar}<#-- noSuchVar is "1" string here -->