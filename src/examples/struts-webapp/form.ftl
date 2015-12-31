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