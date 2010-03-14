${UNDEFINED!"foo"}

<#assign duck = (FOO.BAR)!"luck">
${duck}

<#list UNDEFINED![] as item>
   ${item}
</#list>

${UNDEFINED![]?size}

<#if UNDEFINED??>
   UNDEFINED is defined.
<#else>
   UNDEFINED is undefined.
</#if>