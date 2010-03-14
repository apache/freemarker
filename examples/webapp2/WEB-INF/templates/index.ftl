<#import "/lib/common.ftl" as com>
<#escape x as x?html>

<@com.page title="Index">
  <a href="form.a">Add new message</a> | <a href="help.html">How this works?</a>
  
  <#if guestbook?size = 0>
    <p>No messages.
  <#else>
    <p>The messages are:
    <table border=0 cellspacing=2 cellpadding=2 width="100%">
      <tr align=center valign=top>
        <th bgcolor="#C0C0C0">Name
        <th bgcolor="#C0C0C0">Message
      <#list guestbook as e>
        <tr align=left valign=top>
          <td bgcolor="#E0E0E0">${e.name} <#if e.email?length != 0> (<a href="mailto:${e.email}">${e.email}</a>)</#if>
          <td bgcolor="#E0E0E0">${e.message}
      </#list>
    </table>
  </#if>
</@com.page>

</#escape>