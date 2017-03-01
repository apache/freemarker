<#ftl ns_prefixes = {"x" : "http://x", "y" : "http://y"}>
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
<#recurse doc >

<#macro book>
  <html>
    <head>
      <title><#recurse .node["x:title"]></title>
    </head>
    <body>
      <h1><#recurse .node["x:title"]></h1>
      <#recurse>
    </body>
  </html>
</#macro>

<#macro chapter>
  <h2><#recurse .node["y:title"]></h2>
  <#recurse>
</#macro>

<#macro 'x:chapter'>
  <h2><#recurse .node["y:title"]></h2>
  <#recurse>
</#macro>

<#macro para>
  <p><#recurse>
</#macro>

<#macro 'x:para'>
  <p><#recurse>
</#macro>

<#macro 'y:para'>
  <p><#recurse>
</#macro>

<#macro "x:title">
  <#--
    We have handled this element imperatively,
    so we do nothing here.
  -->
</#macro>

<#macro "y:title">
  <#--
    We have handled this element imperatively,
    so we do nothing here.
  -->
</#macro>

<#macro @text>${.node?html}</#macro>