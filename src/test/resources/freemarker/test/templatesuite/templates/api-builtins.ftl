<@assertEquals expected="b" actual=map?api.get(2?int) />
<@assertEquals expected=2 actual=list?api.indexOf(3?int) />
<@assert test=set?api.contains("b") />
<@assert test=!set?api.contains("d") />

<#assign dump = "">
<#list map?api.entrySet() as entry>
    <#assign dump = dump + entry.key + ": " + entry.value>
    <#if entry_has_next>
        <#assign dump = dump + ", ">
    </#if>
</#list>
<@assertEquals expected="1: a, 2: b, 3: c" actual=dump />

<#assign bw = testName?ends_with("-bw")>

<@assert test=map?has_api />
<@assert test=list?has_api />
<@assert test=set?has_api />
<@assert test = bw == s?has_api />
<@assert test=!1?has_api />
<@assert test=!""?has_api />
<@assert test=!{}?has_api />
<@assert test=!true?has_api />

<#if bw>
  <@assertEquals expected="TEST" actual=s?api.toUpperCase() />
</#if>
