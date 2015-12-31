<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
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