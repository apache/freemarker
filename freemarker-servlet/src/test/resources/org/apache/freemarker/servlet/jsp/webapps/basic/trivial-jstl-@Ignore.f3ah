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
<#-- @Ignore: c:forEach fails because of EL context issues -->

<#assign
    c = JspTaglibs["http://java.sun.com/jsp/jstl/core"]
    fn = JspTaglibs["http://java.sun.com/jsp/jstl/functions"]
>

${n + 1}

<#-- JSTL: -->
<#-- You should NOT call JSTL from FTL, but here we use them for testing taglib JSP compatibility: -->

<@c.if test=t>
  True
</@c.if>

<@c.choose>
  <@c.when test = n == 123>
      Do this
  </@c.when>
  <@c.otherwise>
      Do that
  </@c.otherwise>
</@c.choose>

<@c.forEach var="i" items=ls>
- ${i}
</@c.forEach>

[${fn.trim(" foo ")}]
