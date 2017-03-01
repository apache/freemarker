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
<@testList ["aardvark", "bear", "cat", "dog"] />

<@testList ["aardvark"] />

<@testList [] />

<@testList listables.list />

<@testList listables.set />

<@testList listables.getIterator />

<@testList listables.emptyList />

<@testList listables.emptySet />

<@testList listables.getEmptyIterator />

<#macro testList xs>
=== [${resolve(xs)?join(", ")}] ===
<#assign resolveCallCnt = 0>

-- List+sep:
<#list resolve(xs) as x>
    ${x}<#sep>,</#sep>
</#list>
-- List+else:
<#list resolve(xs) as x>
    ${x}
<#else>
    Empty!
</#list>
-- List+items:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}
    </#items>
    ]
</#list>
-- List+items+else:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}
    </#items>
    ]
<#else>
    Empty!
</#list>
-- List+items+sep+else:
<#list resolve(xs)>
    [
    <#items as x>
        ${x!'U'}<#sep>,</#sep>
    </#items>
    ]
<#else>
    Empty!
</#list>
<@assertEquals expected=5 actual=resolveCallCnt />
--
</#macro>

<#function resolve xs>
    <#assign resolveCallCnt = (resolveCallCnt!0) + 1>
    <#if xs?isMethod>
        <#return xs()>
    <#else>
        <#return xs>
    </#if>
</#function>