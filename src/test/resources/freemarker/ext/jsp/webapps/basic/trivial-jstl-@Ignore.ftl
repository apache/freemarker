<#-- @Ignore: c:forEach fails because of EL context issues -->

<#assign
    c = JspTaglibs["http://java.sun.com/jsp/jstl/core"]
    fn = JspTaglibs["http://java.sun.com/jsp/jstl/functions"]
>

${n + 1}

<#-- JSTL: -->
<#-- You should NOT call JSTL from FTL, but here we use them for testing taglib JSP compatibility: -->

<@c.if test=t>
  True
</@c.if>

<@c.choose>
  <@c.when test = n == 123>
      Do this
  </@c.when>
  <@c.otherwise>
      Do that
  </@c.otherwise>
</@c.choose>

<@c.forEach var="i" items=ls>
- ${i}
</@c.forEach>

[${fn.trim(" foo ")}]
