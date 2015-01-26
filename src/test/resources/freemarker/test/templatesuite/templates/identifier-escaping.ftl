<#macro m\-a data\-color>
    <#local \.namespace = 123>
    <a-b>${data\-color}<#nested \.namespace></a-b><#t>
</#macro>
<#macro "m-b2"></#macro>
<#macro "m/b2"></#macro>

<@m\-a data\-color="red"; loop\-var>${loop\-var}</@>

<#function f\-a p\-a>
    <#return p\-a + " works">
</#function>
${f\-a("f-a")}

<#assign \-\-\-\.\: = 'dash-dash-dash etc.'>
${\-\-\-\.\:}
${.vars['---.:']}
<#assign hash = { '--moz-prop': 'propVal' }>
${hash.\-\-moz\-prop}
${hash['--moz-prop']}

<#assign ls\:a = 1..3>
List: <#list ls\:a as \:i>${\:i}</#list>

<#assign sw\-a=1>
Switch: <#switch sw\-a>
    <#case 1>OK<#break>
    <#default>Fails
</#switch>

<#escape \-x as \-x?upper_case>${'escaped'}</#escape>

<#if false && sw\-a == 1>
    <#visit x\-y2 using x\-y1>
    <#recurse x\-y2 using x\-y1>
    <#import i\-a as i\-b>
    <#include i\-c>
</#if>

<#assign @as@_a = 'as1'>
${@as@_a}
<#assign 'as-c' = 'as2'>
${.vars['as-c']}
<#assign "as/b" = 'as3'>
${.vars["as/b"]}
<#assign "as'c" = 'as4'>
${.vars["as'c"]}
<#assign 'as"d' = 'as5'>
${.vars['as"d']}

<#global g\-a=1 g\-b=2 "g-c"=3>

<#macro catchAll x y attrs...>
<catchAll x=${x} y=${y}<#list attrs?keys?sort as k> ${k}=${attrs[k]}</#list> />
</#macro>
<@catchAll x=1 y=2 z=3 data\-foo=4 a\:b\.c=5 />

<#macro dumpNS>
    <#list .namespace?keys?sort as k>
        ${k} = <#local v = .namespace[k]><#if v?is_string>${v}<#else>...</#if><#lt>
    </#list>
</#macro>
<@dumpNS />
