<#import "/lib/common.ftl" as com>
<#escape x as x?html>

<@com.page title="Add Entry">
  <#if errors?size != 0>
    <p><font color=red>Please correct the following problems:</font>
    <ul>
      <#list errors as e>
        <li><font color=red>${e}</font>
      </#list>
    </ul>
  </#if>
  
  <form method="POST" action="add.a">
    <p>Your name:<br>
    <input type="text" name="name" value="${name}" size=60>
    <p>Your e-mail (optional):<br>
    <input type="text" name="email" value="${email}" size=60>
    <p>Message:<br>
    <textarea name="message" wrap="soft" rows=3 cols=60>${message}</textarea>
    <p><input type="submit" value="Submit">
  </form>
  <p><a href="index.a">Back to the index page</a>
</@com.page>

</#escape>