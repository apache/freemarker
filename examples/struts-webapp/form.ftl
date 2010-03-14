<#import "/lib/common.ftl" as com>
<#global html=JspTaglibs["/WEB-INF/struts-html.tld"]>
<#escape x as x?html>

<@com.page title="Add Entry">
  <@html.errors/>
  
  <@html.form action="/add">
    <p>Your name:<br>
    <@html.text property="name" size="60"/>
    <p>Your e-mail (optional):<br>
    <@html.text property="email" size="60"/>
    <p>Message:<br>
    <@html.textarea property="message" rows="3" cols="60"/>
    <p><@html.submit value="Submit"/>
  </@html.form>
  
  <p><a href="index.do">Back to the index page</a>
</@com.page>

</#escape>