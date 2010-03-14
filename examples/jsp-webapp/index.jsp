<%@ taglib uri="/WEB-INF/fmtag.tld" prefix="fm" %>
<jsp:useBean id="mybean"  class="freemarker.examples.jsp.SimpleBean"/>
<jsp:useBean id="mybeanreq" class="freemarker.examples.jsp.SimpleBean" scope="request"/>
<fm:template>
<html>
  <head>
    <title>FreeMarker JSP Example</title>
  </head>
  <body>
    <h1>FreeMarker JSP example</h1>
    <hr>
    <p>
      This page is a JSP page, yet most of its contents is generated using
      a FreeMarker template. The below lines are the output of calling
      properties on a JSP-declared bean from the FreeMarker template:
    </p>
    
    <#assign mybean = page.mybean>
    <#assign mybeanreq = request.mybeanreq>
    
    <p>page: ${mybean.string}
    <#list mybean.array as item>
      <br>${item}
    </#list>
    <br>request : ${mybeanreq.string}
    
    <p><b>Note:</b> Starting from FreeMarker 2.2 you can use custom JSP tags in
       FreeMarker templates. If you want to migrate from JSP to FTL (i.e. FreeMarker templates),
       then that's probably a better option than embedding FTL into JSP pages.
</body>
</html>
</fm:template>