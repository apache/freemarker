<#macro page title>
  <html>
  <head>
    <title>FreeMarker Struts Example - ${title?html}</title>
    <meta http-equiv="Content-type" content="text/html; charset=ISO-8859-1">
  </head>
  <body>
    <h1>${title?html}</h1>
    <hr>
    <#nested>
    <hr>
    <table border="0" cellspacing=0 cellpadding=0 width="100%">
      <tr valign="middle">
        <td align="left">
          <i>FreeMarker Struts Example</i>
        <td align="right">
          <a href="http://freemarker.org"><img src="poweredby_ffffff.png" border=0></a>
    </table>
  </body>
  </html>
</#macro>