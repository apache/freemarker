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
<#import "varlayers_lib.ftl" as lib>
<@foo 1/>
${x} = ${.data_model.x} = ${.globals.x}
<#assign x = 5>
${x} = ${.main.x} = ${.namespace.x}
<#global x = 6>
${.globals.x} but ${.data_model.x} = 4
${y} = ${.globals.y} = ${.data_model.y?default("ERROR")}
Invisiblity test 1.: <#if .main.y?exists || .namespace.y?exists>failed<#else>passed</#if>
Invisiblity test 2.: <#if .main.z?exists || .namespace.z?exists>failed<#else>passed</#if>
Invisiblity test 3.: <#global q = 1><#if .main.q?exists || .namespace.q?exists || .data_model.q?exists>failed<#else>passed</#if>
--
<@lib.foo/>
--
<#macro foo x>
  ${x} = ${.locals.x}
  <#local x = 2>
  ${x} = ${.locals.x}
  <#local y = 3>
  ${y} = ${.locals.y}
</#macro>