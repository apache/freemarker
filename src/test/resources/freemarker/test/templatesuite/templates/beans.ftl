<#list array as item>
${item}
</#list>
${array?size}
${array[0]}
${array[1]}
<#list list as item>
${item}
</#list>
${list.size()}
<#if list.isEmpty()>empty<#else>not empty</#if>
${list[0]}
${map.key}
${map(objKey)}
${obj.foo}
<#if obj.foo?exists>hasfoo<#else>nofoo</#if>
<#if obj.baz?exists>hasbaz<#else>nobaz</#if>
${obj.bar[0]}
${obj.bar(0)}
${obj.getFoo()}
${obj.overloaded(1?int)}
${obj.overloaded("String")}
${resourceBundle.message}
${resourceBundle("format", date)}
<#assign static = statics["freemarker.test.templatesuite.models.BeanTestClass"]>
${static.staticMethod()}
${static.staticOverloaded[1]}
${static.staticOverloaded("String")}
${static.STATIC_FINAL_FIELD}
${static.STATIC_FIELD}
<#assign enum = enums["freemarker.test.templatesuite.models.EnumTestClass"]>
${enum.ONE}
${enum.TWO}
${enum.THREE}
${(enum.ONE == enum.ONE)?string("true", "false")}
${(enum.ONE == enum.TWO)?string("true", "false")}
${enums["freemarker.test.templatesuite.models.BeanTestClass"]?exists?string("true", "false")}
${obj.something}
${obj.publicInner.x}
${obj.publicInner.m()}
<@assertFails message="obj.privateInner.x">${obj.privateInner.x}</@>
<@assertFails message="obj.privateInner.m">${obj.privateInner.m()}</@>