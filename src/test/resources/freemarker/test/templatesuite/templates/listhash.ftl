<#setting booleanFormat='Y,N'>

<#macro listings maps>
  <#list maps as m>
    Map:
    
    [
    <#list m as k, v>
      ${k!'null'} = ${v!'null'}
    </#list>
    ]
  
    [
    <#list m as k, v>
      ${k!'null'} = ${v!'null'}<#sep>;</#sep> // @${k?index}=@${v?index}; ${k?itemParity}=${v?itemParity}; ${k?hasNext}=${v?hasNext}
    <#else>
      Empty
    </#list>
    ]
  
    {
    <#list m>
      [
      <#items as k, v>
        ${k!'null'} = ${v!'null'}<#sep>;</#sep> // @${k?index}=@${v?index}; ${k?itemParity}=${v?itemParity}; ${k?hasNext}=${v?hasNext}
      </#items>
      ]
    <#else>
      Empty
    </#list>
    }

  </#list>
</#macro>

Non-empty maps:

<@listings listables.hashEx2s />
<@listings [ listables.hashNonEx2 ] />

Empty maps:

<@listings listables.emptyHashes />

<#list { 'a': { 'aa': 11 }, 'b': { 'ba': 21, 'bb': 22 }, 'c': {} } as k1, v1>
  ${k1} @ ${k1?index}, ${v1?size}
  <#list v1 as k2, v2>
    ${k2} = ${v2}  @ ${k2?index} // inside ${k1} @ ${k1?index}, ${v1?size}
  </#list>
  ${k1} @ ${k1?index}, ${v1?size}
  --
</#list>
