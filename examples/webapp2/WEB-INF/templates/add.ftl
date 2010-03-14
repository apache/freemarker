<#import "/lib/common.ftl" as com>
<#escape x as x?html>

<@com.page title="Entry added">
  <p>You have added the following entry to the guestbook:
  <p><b>Name:</b> ${entry.name}
  <#if entry.email?length != 0>
    <p><b>Email:</b> ${entry.email}
  </#if>
  <p><b>Message:</b> ${entry.message}
  <p><a href="index.a">Back to the index page...</a>
</@com.page>

</#escape>