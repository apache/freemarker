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
<#list ['a', 'b', 'c'] as x>
    ${x?index}: ${x} [<#list ['A', 'B', 'C'] as x>${x?index}:${x}<#sep>, </#list>]
</#list>

<#list ['a', 'b', 'c'] as i>
    ${i?index}: ${i} <#list ['A', 'B', 'C'] as j>${i?index}${i}/${j?index}${j}<#sep>, </#list>
</#list>

<#list ['a', 'b', 'c']><#items as x>${x?index}:${x}<#sep>, </#items></#list>
<#list ['a', 'b', 'c']><#items as x>${x?counter}. ${x};<#sep> </#items></#list>

<#list ['a', 'b', 'c'] as x>${x}<#if x?hasNext>, </#if></#list>
<#list ['a', 'b', 'c'] as x\-y>${x\-y}<#if x\-y?hasNext>, </#if></#list>

<#list ['a', 'b', 'c'] as x><#if x?isFirst>${x?capFirst}<#else>${x}</#if><#sep>, </#sep><#if x?isLast>.</#if></#list>

<#list ['a', 'b', 'c'] as x>${x?isOddItem?c}/${x?isEvenItem?c}<#sep> </#list>

<#list ['a', 'b', 'c'] as x>
    <td class="${x?itemParity}Row">${x}</td>
</#list>

<#list ['a', 'b', 'c'] as x>
    <td class="row${x?itemParityCap}">${x}</td>
</#list>

<#list ['a', 'b', 'c', 'd', 'e', 'f', 'g'] as x>
    <td class="${x?itemCycle('R', 'G', 'B')}">${x}</td>
</#list>
<@assertFails message="expects 1"><#list 1..1 as x>${x?itemCycle()}</#list></@>