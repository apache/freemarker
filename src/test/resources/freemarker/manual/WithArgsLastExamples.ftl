<#function f a b c d>
  <#return "a=${a}, b=${b}, c=${c}, d=${d}">
</#function>

${f?with_args([2, 3])(1, 2)}
${f?with_args_last([2, 3])(1, 2)}

<#macro m a b others...>
  a=${a}
  b=${b}
  others:
  <#list others as k, v>
    ${k} = ${v}
  </#list>
</#macro>
<@m?with_args({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
<@m?with_args_last({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />

<#macro m a b others...>
  <#list .args as k, v>
    ${k} = ${v}
  </#list>
</#macro>
<@m?with_args({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
<@m?with_args_last({'e': 5, 'f': 6}) a=1 b=2 c=3 d=4 />
