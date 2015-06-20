

<#assign xs = []>

<#list x as xs>
    ${x}
</#list>

<#assign a = 1>
<#assign b = 1>

<@b>
    x
</@b>

<@c />

<@d></@d>

a
<#-- comment -->
b