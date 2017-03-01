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
<@isIRE>${v}</@>
<@isIRE>${w}</@>
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->

<@assertEquals actual=v!'-' expected='-' />
<@assertEquals actual=(v)!'-' expected='-' />
<@assertEquals actual=(v!) expected='' />
<@assertEquals actual=((v)!) expected='' />
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->
<@assertEquals actual=v?? expected=false />
<@assertEquals actual=(v)?? expected=false />
<@assertEquals actual=v?default('-') expected='-' />
<@assertEquals actual=(v)?default('-') expected='-' />
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->
<@assertEquals actual=v?exists expected=false />
<@assertEquals actual=(v)?exists expected=false />
<@assertEquals actual=v?if_exists expected='' />
<@assertEquals actual=(v)?if_exists expected='' />
<@assertEquals actual=v?has_content expected=false />
<@assertEquals actual=(v)?has_content expected=false />

<@assertEquals actual=v?default(w, '-') expected='-' />
<@assertEquals actual=v!w!'-' expected='-' />
<#assign w = 'W'>
<@assertEquals actual=v?default(w, '-') expected='W' />
<@assertEquals actual=v!w!'-' expected='W' />

<#list ['V', 1.5] as v>
	<@assertEquals actual=v!'-' expected=v />
	<@assertEquals actual=(v)!'-' expected=v />
	<@assert test=v?? />
	<@assert test=(v)?? />
	<@assertEquals actual=v?default('-') expected=v />
	<@assertEquals actual=(v)?default('-') expected=v />
	<@assert test=v?exists />
	<@assert test=(v)?exists />
	<@assertEquals actual=v?if_exists expected=v />
	<@assertEquals actual=(v)?if_exists expected=v />
	<@assert test=v?has_content />
	<@assert test=(v)?has_content />
</#list>
<@assert test=!v?? />
<@assert test=!v?exists />
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->

<@isIRE>${u.v!'-'}</@>
<@assertEquals actual=(u.v)!'-' expected='-' />
<@isIRE>${u.v??}</@>
<@assertEquals actual=(u.v)?? expected=false />
<@isIRE>${u.v?default('-')}</@>
<@assertEquals actual=(u.v)?default('-') expected='-' />
<@isIRE>${u.v?exists}</@>
<@assertEquals actual=(u.v)?exists expected=false />
<@isIRE>${u.v?if_exists}</@>
<@assertEquals actual=(u.v)?if_exists expected='' />
<@isIRE>${u.v?has_content}</@>
<@assertEquals actual=(u.v)?has_content expected=false />

<#assign u = { 'x': 'X' } >
<@assertEquals actual=u.v!'-' expected='-' />
<@assertEquals actual=(u.v)!'-' expected='-' />
<@assertEquals actual=u.v?? expected=false />
<@assertEquals actual=(u.v)?? expected=false />
<@assertEquals actual=u.v?default('-') expected='-' />
<@assertEquals actual=(u.v)?default('-') expected='-' />
<@assertEquals actual=u.v?exists expected=false />
<@assertEquals actual=(u.v)?exists expected=false />
<@assertEquals actual=u.v?if_exists expected='' />
<@assertEquals actual=(u.v)?if_exists expected='' />
<@assertEquals actual=u.v?has_content expected=false />
<@assertEquals actual=(u.v)?has_content expected=false />

<#assign u = { 'v': 'V' } >
<@assertEquals actual=u.v!'-' expected='V' />
<@assertEquals actual=(u.v)!'-' expected='V' />
<@assert test=u.v?? />
<@assert test=(u.v)?? />
<@assertEquals actual=u.v?default('-') expected='V' />
<@assertEquals actual=(u.v)?default('-') expected='V' />
<@assert test=u.v?exists />
<@assert test=(u.v)?exists />
<@assertEquals actual=u.v?if_exists expected='V' />
<@assertEquals actual=(u.v)?if_exists expected='V' />
<@assert test=u.v?has_content />
<@assert test=(u.v)?has_content />

<#list 1..4 as i>
  <#if i == 3><#assign x = 'X'></#if>
  <@assertEquals actual=((x!'-') == '-') expected=(i < 3) />
</#list>

<#macro attemptTest>
  <#attempt>
    ${fails}
  <#recover>
    <@assert test=isNonFastIREMessage(.error) />
  </#attempt>
</#macro>
<@attemptTest />
${(callMacroFromExpression(attemptTest))!}

<#macro interpretTest><@'$\{fails}'?interpret /></#macro>
<#attempt>
  <@interpretTest />
<#recover>
  <@assert test=isNonFastIREMessage(.error) />
</#attempt>
<#attempt>
  ${(callMacroFromExpression(interpretTest))!}
<#recover>
  <@assert test=isNonFastIREMessage(.error) />
</#attempt>

<@assertEquals actual='fails'?eval!'-' expected='-' />
<@assertEquals actual=('fails')?eval!'-' expected='-' />

<#macro isIRE><@assertFails exception="InvalidReferenceException"><#nested></@></#macro>
<#macro isNonFastIRE><@assertFails exception="InvalidReferenceException" message="Tip:"><#nested></@></#macro>
<#function isNonFastIREMessage msg><#return msg?contains('Tip:') && msg?contains('null or missing')></#function>
<#function callMacroFromExpression m>
  <#local captured><@m /></#local>
  <#return captured>
</#function>
