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