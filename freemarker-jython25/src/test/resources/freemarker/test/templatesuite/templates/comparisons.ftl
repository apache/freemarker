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
<html>
<head>
<title>FreeMarker: Numeric Operations Test</title>
</head>
<body>

<p>A simple test follows:</p>

<p>${message}</p>

<p>Start with the increment operator:</p>
<#assign a1 = 0>
<p>a1 = ${a1}</p>
<#assign a1 = a1 + 1>
<p>a1 = ${a1}</p>
<#assign a1 = a1 + 1>
<p>a1 = ${a1}</p>

<p>Now the decrement operator:</p>
<#assign a2 = 5>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>
<p>a2 = ${a2}</p>
<#assign a2 = a2 - 1>

<p>Now the add operator:</p>
<#assign op1 = 5>
<#assign op2 = 3>
<#assign op3 = op1 + op2>
<p>op1 = ${op1}, op2 = ${op2}, op3 = ${op3}</p>
<#assign op3 = op3 + op2>
<p>op3 = ${op3}</p>

<p>And the subtract operator:</p>
<#assign op3 = op1 - op2 >
<p>op1 = ${op1}, op2 = ${op2}, op3 = ${op3}</p>
<#assign op3 = op3 - op2 >
<p>op3 = ${op3}</p>

<p>The comparison operators:</p>
<#assign list1 = [ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 ]>
<#foreach item in list1>
   <p>Item is: ${item}</p>
   <#if item lt 5>
   <p>Item is less than five.</p>
   </#if>
   <#if item <= 7>
   <p>Item is less than or equals to seven.</p>
   </#if>
   <#if item gt 2>
   <p>Item is greater than two.</p>
   </#if>
   <#if (item >= 10)>
   <p>Item is greater than or equal to ten.</p>
   </#if>
</#foreach>
<@noOutput>

<#-- Signum-based optimization test, all 9 permutations: -->
<#-- 1 -->
<@assert test= !(0 != 0) />
<@assert test= (0 == 0) />
<@assert test= !(0 > 0) />
<@assert test= (0 >= 0) />
<@assert test= !(0 < 0) />
<@assert test= (0 <= 0) />
<#-- 2 -->
<@assert test= !(3 != 3) />
<@assert test= (3 == 3) />
<@assert test= !(3 > 3) />
<@assert test= (3 >= 3) />
<@assert test= !(3 < 3) />
<@assert test= (3 <= 3) />
<#-- 3 -->
<@assert test= !(-3 != -3) />
<@assert test= (-3 == -3) />
<@assert test= !(-3 > -3) />
<@assert test= (-3 >= -3) />
<@assert test= !(-3 < -3) />
<@assert test= (-3 <= -3) />
<#-- 4 -->
<@assert test= (3 != 0) />
<@assert test= !(3 == 0) />
<@assert test= (3 > 0) />
<@assert test= (3 >= 0) />
<@assert test= !(3 < 0) />
<@assert test= !(3 <= 0) />
<#-- 5 -->
<@assert test= (0 != 3) />
<@assert test= !(0 == 3) />
<@assert test= !(0 > 3) />
<@assert test= !(0 >= 3) />
<@assert test= (0 < 3) />
<@assert test= (0 <= 3) />
<#-- 6 -->
<@assert test= (-3 != 0) />
<@assert test= !(-3 == 0) />
<@assert test= !(-3 > 0) />
<@assert test= !(-3 >= 0) />
<@assert test= (-3 < 0) />
<@assert test= (-3 <= 0) />
<#-- 7 -->
<@assert test= (0 != -3) />
<@assert test= !(0 == -3) />
<@assert test= (0 > -3) />
<@assert test= (0 >= -3) />
<@assert test= !(0 < -3) />
<@assert test= !(0 <= -3) />
<#-- 8 -->
<@assert test= (-3 != 3) />
<@assert test= !(-3 == 3) />
<@assert test= !(-3 > 3) />
<@assert test= !(-3 >= 3) />
<@assert test= (-3 < 3) />
<@assert test= (-3 <= 3) />
<#-- 9 -->
<@assert test= (3 != -3) />
<@assert test= !(3 == -3) />
<@assert test= (3 > -3) />
<@assert test= (3 >= -3) />
<@assert test= !(3 < -3) />
<@assert test= !(3 <= -3) />
<#-- Again, now on runtime: -->
<#assign m3 = -3>
<#assign p3 = 3>
<#assign z = 0>
<#-- 1 -->
<@assert test= !(z != z) />
<@assert test= (z == z) />
<@assert test= !(z > z) />
<@assert test= (z >= z) />
<@assert test= !(z < z) />
<@assert test= (z <= z) />
<#-- 2 -->
<@assert test= !(p3 != p3) />
<@assert test= (p3 == p3) />
<@assert test= !(p3 > p3) />
<@assert test= (p3 >= p3) />
<@assert test= !(p3 < p3) />
<@assert test= (p3 <= p3) />
<#-- 3 -->
<@assert test= !(m3 != m3) />
<@assert test= (m3 == m3) />
<@assert test= !(m3 > m3) />
<@assert test= (m3 >= m3) />
<@assert test= !(m3 < m3) />
<@assert test= (m3 <= m3) />
<#-- 4 -->
<@assert test= (p3 != z) />
<@assert test= !(p3 == z) />
<@assert test= (p3 > z) />
<@assert test= (p3 >= z) />
<@assert test= !(p3 < z) />
<@assert test= !(p3 <= z) />
<#-- 5 -->
<@assert test= (z != p3) />
<@assert test= !(z == p3) />
<@assert test= !(z > p3) />
<@assert test= !(z >= p3) />
<@assert test= (z < p3) />
<@assert test= (z <= p3) />
<#-- 6 -->
<@assert test= (m3 != z) />
<@assert test= !(m3 == z) />
<@assert test= !(m3 > z) />
<@assert test= !(m3 >= z) />
<@assert test= (m3 < z) />
<@assert test= (m3 <= z) />
<#-- 7 -->
<@assert test= (z != m3) />
<@assert test= !(z == m3) />
<@assert test= (z > m3) />
<@assert test= (z >= m3) />
<@assert test= !(z < m3) />
<@assert test= !(z <= m3) />
<#-- 8 -->
<@assert test= (m3 != p3) />
<@assert test= !(m3 == p3) />
<@assert test= !(m3 > p3) />
<@assert test= !(m3 >= p3) />
<@assert test= (m3 < p3) />
<@assert test= (m3 <= p3) />
<#-- 9 -->
<@assert test= (p3 != m3) />
<@assert test= !(p3 == m3) />
<@assert test= (p3 > m3) />
<@assert test= (p3 >= m3) />
<@assert test= !(p3 < m3) />
<@assert test= !(p3 <= m3) />

<#-- String comparison: -->
<#assign s = 'a'>
<@assert test= '' == '' />
<@assert test= 'a' == 'a' />
<@assert test= s == 'a' />
<@assert test= s + 'b' == 'ab' />
<@assert test= 'á' == 'a\x0301' />
<@assert test= 'a\x0301' == 'á'/>
<@assert test= 'a' != 'A' />
<@assert test= s != 'A' />
<@assert test= 'A' != 'a' />
<@assert test= '' != 'a' />
<@assert test= 'a' != '' />
<@assert test= 'ab' != 'ac' />
<@assertFails message="Can't use operator \"<\" on string values.">${s < s}</@>
<@assertFails message="Can't use operator \">\" on string values.">${s > s}</@>
<@assertFails message="Can't use operator \"<=\" on string values.">${s <= s}</@>
<@assertFails message="Can't use operator \">=\" on string values.">${(s >= s)}</@>

</@noOutput>
</body>
</html>
