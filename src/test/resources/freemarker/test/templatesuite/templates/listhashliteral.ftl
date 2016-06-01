<#list [ { "a": 1, "b": 2, "a": 3 }, { } ] as h>
  KVPs:
  <#list h as k, v>
    ${k} = ${v}
  </#list>
  
  Keys:
  <#list h?keys as k>
    ${k}
  </#list>
  
  Values:
  <#list h?values as v>
    ${v}
  </#list>

</#list>