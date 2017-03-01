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
[
<#list listables.list as i>
    [<#list listables.list as j>(${i}@${i_index}, ${j}@${j_index})<#sep>, </#sep></#list>]<#sep>,</#sep>
</#list>
]

<#macro hits xs style="">
    <#list xs>
        <p>${xs?size} hits:
        <div class="hits">
            <#switch style>
                <#case "hidden">
                    ...
                    <#break>
                <#case "other">
                    <#items as x>
                        <div class="hitOther">${x}</div>
                    </#items>
                    <#break>
                <#case "none">
                <#default>
                    <#items as x>
                        <div class="hit">${x}</div>
                    </#items>
                    <#break>
            </#switch>
        </div>
    <#else>
        <p>Nothing.
    </#list>
</#macro>

<@hits ['a', 'b'] />

<@hits ['a', 'b'], "other" />

<@hits ['a', 'b'], "hidden" />

<@hits [] />

<#list listables.list as i><#if i_index gt 1>...<#break></#if>${i}<#sep>, </#sep></#list>
<#list listables.list>[<#items as i><#if i_index gt 1>...<#break></#if>${i}<#sep>, </#sep></#items>]</#list>

<@testAutoClosedSep 1..3 />
<@testAutoClosedSep [1] />
<@testAutoClosedSep [] />

<#macro testAutoClosedSep xs>
<#list xs as x>${x}<#sep>, <#else>Empty</#list>
<#list xs as x>${x}<#sep><#if x_index == 0> /*first*/, <#else>, </#if><#else>Empty</#list>
<#list xs>[<#items as x>${x}<#sep>, </#items>]<#else>Empty</#list>
</#macro>