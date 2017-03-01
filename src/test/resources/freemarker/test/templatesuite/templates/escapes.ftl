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
<#assign h=["","a","b","c"]>
<#assign g={"x":1,"y":2,"z":3}>
<#escape x as h[x]>
${1}
${2}
${3}
<#escape x as g[x]>
${"x"}
${"y"}
${"z"}
<#noescape>${1}</#noescape>
<#noescape><#noescape>${1}</#noescape></#noescape>
</#escape>
${1}
${2}
${3}
</#escape>
<#escape x as x?html>
${"<&>"}
<#escape x as x?xml>
${"<&>"}
</#escape>
${"<&>"}
</#escape>
---
<#assign x = "<Mooo>">
${x} = <Mooo>
<#escape x as x?upper_case>
  ${x} = <MOOO>
  <#escape x as x?html>
    ${x} = &lt;MOOO&gt;
    <#noescape>
      ${x} = <MOOO>
    </#noescape>
    ${x} = &lt;MOOO&gt;
  </#escape>
  ${x} = <MOOO>
  <#noescape>
    ${x} = <Mooo>
    <#escape x as x?html>
      ${x} = &lt;Mooo&gt;
      <#noescape>
        ${x} = <Mooo>
      </#noescape>
      ${x} = &lt;Mooo&gt;
    </#escape>
    ${x} = <Mooo>
  </#noescape>
  ${x} = <MOOO>
</#escape>
<#escape az as ["red", "green", "blue"][az-1]>
  ${1} ${2} ${3}
</#escape>
---
<#assign s = 'A&B'>
<#escape x as '<' + x?html + '>[' + x?lower_case + '](' + x + ')'>
  ${s} ${s + 2}
  <#escape x as '{' + x?lower_case + '}' + x>
    ${s} ${s + 2}
  </#escape>
</#escape>