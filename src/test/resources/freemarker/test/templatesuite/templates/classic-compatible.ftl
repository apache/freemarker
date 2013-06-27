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