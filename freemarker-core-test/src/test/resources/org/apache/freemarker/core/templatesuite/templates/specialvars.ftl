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
<#-- Mostly just checks if the expressions doesn't fail -->
<#assign works = .dataModel>
<#attempt>
  ${noSuchVariableExists}
<#recover>
  <#assign works = .error>
</#attempt>
<#assign works = .globals>
${.lang} == en
${.locale} == en_US
<#assign works = .locals!>
<#assign works = .main>
<#assign works = .node!>
${.outputEncoding?lowerCase} == utf-8
${.currentTemplateName} == specialvars.ftl
${.urlEscapingCharset?lowerCase} == iso-8859-1
<#assign foo = "x">
${.vars['foo']} == x
<#assign works = .version>
${.now?isDatetime?c} == true