<#macro m1 a b c>
  m1 does things with ${a}, ${b}, ${c}
</#macro>

<#macro m2 a b c>
  m2 does things with ${a}, ${b}, ${c}
  Delegate to m1:
  <@m1?with_args(.args) />
</#macro>

<@m2 a=1 b=2 c=3 />
