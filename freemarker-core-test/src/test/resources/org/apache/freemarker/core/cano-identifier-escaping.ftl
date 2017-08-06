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
<#macro m\-a data\-color>
    <#local \.namespace = 123>
    <a-b>${data\-color}<#nested \.namespace></a-b><#t>
</#macro>
<#macro m\-b2></#macro>
<#macro "m/b2"></#macro>

<@m\-a data\-color="red"; loop\-var, loopVar2>${loop\-var}</@>

<#function f\-a(p\-a)>
    <#return p\-a + " works">
</#function>
${f\-a("f-a")}

<#assign \-\-\-\.\: = 'dash-dash-dash etc.'>
${\-\-\-\.\:}
${.vars['---.:']}
<#assign hash = { '--moz-prop': 'propVal' }>
${hash.\-\-moz\-prop}
${hash['--moz-prop']}

<#assign ls\:a = 1..3>
List: <#list ls\:a as \:i>${\:i}</#list>

<#assign sw\-a=1>
Switch: <#switch sw\-a>
    <#case 1>OK<#break>
    <#default>Fails
</#switch>

<#escape \-x as \-x?upperCase>${'escaped'}</#escape>

<#if false && sw\-a == 1>
    <#visit x\-y2 using x\-y1>
    <#recurse x\-y2 using x\-y1>
    <#import i\-a as i\-b>
    <#include i\-c>
</#if>

<#assign @as@_a = 'as1'>
${@as@_a}
<#assign as\-c = 'as2'>
${.vars['as-c']}
<#assign "as/b" = 'as3'>
${.vars["as/b"]}
<#assign "as'c" = 'as4'>
${.vars["as'c"]}
<#assign 'as"d' = 'as5'>
${.vars['as"d']}

<#global g\-a=1 g\-b=2 "g-c"=3>

<#macro dumpNS>
    <#list .namespace?keys?sort as k>
        ${k} = <#local v = .namespace[k]><#if v?isString>${v}<#else>...</#if><#lt>
    </#list>
</#macro>
<@dumpNS />
