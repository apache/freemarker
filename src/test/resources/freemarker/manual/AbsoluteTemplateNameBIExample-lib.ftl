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
<#--
  <@smileyInclude name /> behaves like <#include name>, but prints a "(:" before the
  template, or prints "):" instead if the template is missing.

  Note that just like with #include, if name is relative, it's resolved based on the
  directory of the caller template, not of the template that defines this macro. As
  .get_optional_template resolves relative names based on the current template, we
  had to convert the name to an absolute name based on the caller template before
  passing it to it.
-->
<#macro smileyInclude name>
  <#local t = .get_optional_template(
      name?absolute_template_name(.caller_template_name))>
  <#if t.exists>
    (:
    <@t.include />
  <#else>
    ):
  </#if>
</#macro>