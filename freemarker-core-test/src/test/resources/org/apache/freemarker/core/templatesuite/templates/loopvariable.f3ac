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
<#setting locale="en_US">
---
<#macro myLoop from to>
  <#list from..to as x>
  - <#nested x></#list>*
</#macro>
<#list 2..1 as i>
  ${i}
  <@myLoop from=1 to=3; i>
    L1 ${i}
    <@myLoop from=1 to=2; i>
      L2 ${i}: <#list 1..3 as i>${i}; </#list>
    </@>
  </@>
</#list>
---
<#macro repeat count>
  <#list 1..count as x>
    <#nested x, x/2, x==count>
  </#list>
</#macro>
<#macro test2>
<#local c = 123>
<@repeat count=4 ; c, halfc, last>
  <#local c = .locals.c + 0.1>
  ${c} ${halfc}<#if last> Last!</#if>
</@repeat>
${c}
</#macro>
<@test2/>
---