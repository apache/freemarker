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