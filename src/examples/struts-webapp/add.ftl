<#import "/lib/common.ftl" as com>
<#escape x as x?html>

<@com.page title="Entry added">
  <p>You have added the following entry to the guestbook:
  <p><b>Name:</b> ${guestbookEntry.name}
  <#if guestbookEntry.email?length != 0>
    <p><b>Email:</b> ${guestbookEntry.email}
  </#if>
  <p><b>Message:</b> ${guestbookEntry.message}
  <p><a href="index.do">Back to the index page...</a>
</@com.page>

</#escape>