<#macro page title>
  <html>
  <head>
    <title>FreeMarker Example Web Application 2 - ${title?html}</title>
    <meta http-equiv="Content-type" content="text/html; charset=${.output_encoding}">
  </head>
  <body>
    <h1>${title?html}</h1>
    <hr>
    <#nested>
    <hr>
    <table border="0" cellspacing=0 cellpadding=0 width="100%">
      <tr valign="middle">
        <td align="left">
          <i>FreeMarker Example 2</i>
        <td align="right">
          <a href="http://freemarker.org"><img src="poweredby_ffffff.png" border=0></a>
    </table>
  </body>
  </html>
</#macro>