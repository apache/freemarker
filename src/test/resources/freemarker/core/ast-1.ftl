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
1 <@foo x=1 y=2; b1, b2>x</@foo>
2 <@ns.bar 1 2; b1, b2>y</@>
3 <#assign x = 123><#assign x = 123 in ns><#global x = 123>
4 <#if x + 1 == 0>foo${y}bar<#else>${"static"}${'x${baaz * 10}y'}</#if>
5 <#switch x><#case 1>one<#case 2>two<#default>more</#switch>
6 <#macro foo x y=2 z=y+1 q...><#nested x y></#macro>
7 <#function foo x y><#local x = 123><#return 1></#function>
8 <#list xs as x></#list>
9 <#list xs>[<#items as x>${x}<#sep>, </#items>]<#else>None</#list>
10 <#-- A comment -->
11 <#outputFormat "XML"><#noAutoEsc>${a}<#autoEsc>${b}</#autoEsc>${c}</#noAutoEsc></#outputFormat>