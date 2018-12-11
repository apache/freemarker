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
  LB<#lt>
  LB<#lt>
	LB<#lt>
LB<#lt>
  IB
  IC1<#rt>
  IC2<#rt>
C1<#rt>
C2<#rt>
  ICS <#rt>
CS1 <#rt>
  CS2 <#t>
  C3<#t>
  C1<#t>
C2
B
B
  C1<#t>
CB
C1
  C2<#t>
  ICB
  IC<#rt>
  ICB
  IC<#rt>
  CB<#lt>
--
<#macro x t>${t}</#macro>
  ${""}<@x t="LB"/><#lt>
  <@x t="LB"/><#lt>${""}
	<@x t="LB"/><#lt>${""}
<@x t="LB"/><#lt>${""}
  <@x t="IB"/>${""}
${""}  <@x t="IC1"/><#rt>
  <@x t="IC2"/><#rt>${""}
${""}<@x t="C1"/><#rt>
<@x t="C2"/><#rt>${""}
  <@x t="ICS"/> <#rt>${""}
<@x t="CS1"/> <#rt>${""}
  <@x t="CS2"/> <#t>${""}
  <@x t="C3"/><#t>
  <@x t="C1"/><#t>
<@x t="C2"/>${""}
<#nt><@x t="B"/>
<@x t="B"/><#nt>
  <@x t="C1"/><#t>
<@x t="CB"/>${""}
<@x t="C1"/>${""}
  <@x t="C2"/><#t>${""}
  <@x t="ICB"/>${""}
  ${""}<@x t="IC"/><#rt>
  <@x t="ICB"/>${""}
${""}  <@x t="IC"/><#rt>
  <@x t="CB"/>${""}<#lt>
--
   <#lt>  IB
  IC1<#rt>
  <#assign x = 1> <#-- just a comment -->
  C2<#t>
  <#assign x = 1>
  IB
1<#t>
  <#assign x = 1>
2
---
<#t>1
  <#t> 2
  <#lt>3
  4
  <#rt>5
  6
---
a
  <#assign x = 1><#t>
b<#t>  
c
---
  <#if true>
    <#t>foo
  </#if>
---
  <#if true><#-- just a comment -->
    foo<#t>
  </#if>
