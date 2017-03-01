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
<@assertEquals expected="b" actual=map?api.get(2?int) />
<@assertEquals expected=2 actual=list?api.indexOf(3?int) />
<@assert test=set?api.contains("b") />
<@assert test=!set?api.contains("d") />

<#assign dump = "">
<#list map?api.entrySet() as entry>
    <#assign dump = dump + entry.key + ": " + entry.value>
    <#if entry_has_next>
        <#assign dump = dump + ", ">
    </#if>
</#list>
<@assertEquals expected="1: a, 2: b, 3: c" actual=dump />

<#assign bw = testName?ends_with("-bw")>

<@assert test=map?has_api />
<@assert test=list?has_api />
<@assert test=set?has_api />
<@assert test = bw == s?has_api />
<@assert test=!1?has_api />
<@assert test=!""?has_api />
<@assert test=!{}?has_api />
<@assert test=!true?has_api />

<#if bw>
  <@assertEquals expected="TEST" actual=s?api.toUpperCase() />
</#if>
