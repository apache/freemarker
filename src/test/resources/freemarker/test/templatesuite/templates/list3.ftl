[
<#list listables.list as i>
    [<#list listables.list as j>(${i}@${i_index}, ${j}@${j_index})<#sep>, </#sep></#list>]<#sep>,</#sep>
</#list>
]

<#macro hits xs style="">
    <#list xs>
        <p>${xs?size} hits:
        <div class="hits">
            <#switch style>
                <#case "hidden">
                    ...
                    <#break>
                <#case "other">
                    <#items as x>
                        <div class="hitOther">${x}</div>
                    </#items>
                    <#break>
                <#case "none">
                <#default>
                    <#items as x>
                        <div class="hit">${x}</div>
                    </#items>
                    <#break>
            </#switch>
        </div>
    <#else>
        <p>Nothing.
    </#list>
</#macro>

<@hits ['a', 'b'] />

<@hits ['a', 'b'], "other" />

<@hits ['a', 'b'], "hidden" />

<@hits [] />

<#list listables.list as i><#if i_index gt 1>...<#break></#if>${i}<#sep>, </#sep></#list>