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
<@assertEquals actual=v!'-' expected='-' />
<@assertEquals actual=(v)!'-' expected='-' />
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->
<@assertEquals actual=v?? expected=false />
<@assertEquals actual=(v)?? expected=false />
<@assertEquals actual=v?ifExists expected='' />
<@assertEquals actual=(v)?ifExists expected='' />
<@assertEquals actual=v?hasContent expected=false />
<@assertEquals actual=(v)?hasContent expected=false />

<@assertEquals actual=v!w!'-' expected='-' />
<@assertEquals actual=v!w!'-' expected='-' />
<#assign w = 'W'>
<@assertEquals actual=v!w!'-' expected='W' />
<@assertEquals actual=v!w!'-' expected='W' />

<#list ['V', 1.5] as v>
	<@assertEquals actual=v!'-' expected=v />
	<@assertEquals actual=(v)!'-' expected=v />
	<@assert v?? />
	<@assert (v)?? />
	<@assertEquals actual=v!'-' expected=v />
	<@assertEquals actual=(v)!'-' expected=v />
	<@assert v?? />
	<@assert (v)?? />
	<@assertEquals actual=v?ifExists expected=v />
	<@assertEquals actual=(v)?ifExists expected=v />
	<@assert v?hasContent />
	<@assert (v)?hasContent />
</#list>
<@assert !v?? />
<@assert !v?? />
<@isNonFastIRE>${v}</@> <#-- To check that it isn't an IRE.FAST_INSTANCE -->

<@isIRE>${u.v!'-'}</@>
<@assertEquals actual=(u.v)!'-' expected='-' />
<@isIRE>${u.v??}</@>
<@assertEquals actual=(u.v)?? expected=false />
<@isIRE>${u.v!'-'}</@>
<@assertEquals actual=(u.v)!'-' expected='-' />
<@isIRE>${u.v??}</@>
<@assertEquals actual=(u.v)?? expected=false />
<@isIRE>${u.v?ifExists}</@>
<@assertEquals actual=(u.v)?ifExists expected='' />
<@isIRE>${u.v?hasContent}</@>
<@assertEquals actual=(u.v)?hasContent expected=false />

<#assign u = { 'x': 'X' } >
<@assertEquals actual=u.v!'-' expected='-' />
<@assertEquals actual=(u.v)!'-' expected='-' />
<@assertEquals actual=u.v?? expected=false />
<@assertEquals actual=(u.v)?? expected=false />
<@assertEquals actual=u.v!'-' expected='-' />
<@assertEquals actual=(u.v)!'-' expected='-' />
<@assertEquals actual=u.v?? expected=false />
<@assertEquals actual=(u.v)?? expected=false />
<@assertEquals actual=u.v?ifExists expected='' />
<@assertEquals actual=(u.v)?ifExists expected='' />
<@assertEquals actual=u.v?hasContent expected=false />
<@assertEquals actual=(u.v)?hasContent expected=false />

<#assign u = { 'v': 'V' } >
<@assertEquals actual=u.v!'-' expected='V' />
<@assertEquals actual=(u.v)!'-' expected='V' />
<@assert u.v?? />
<@assert (u.v)?? />
<@assertEquals actual=u.v!'-' expected='V' />
<@assertEquals actual=(u.v)!'-' expected='V' />
<@assert u.v?? />
<@assert (u.v)?? />
<@assertEquals actual=u.v?ifExists expected='V' />
<@assertEquals actual=(u.v)?ifExists expected='V' />
<@assert u.v?hasContent />
<@assert (u.v)?hasContent />

<#list 1..4 as i>
  <#if i == 3><#assign x = 'X'></#if>
  <@assertEquals actual=((x!'-') == '-') expected=(i < 3) />
</#list>

<#macro attemptTest>
  <#attempt>
    ${fails}
  <#recover>
    <@assert isNonFastIREMessage(.error) />
  </#attempt>
</#macro>
<@attemptTest />
${(callMacroFromExpression(attemptTest))!}

<#macro interpretTest><@'$\{fails}'?interpret /></#macro>
<#attempt>
  <@interpretTest />
<#recover>
  <@assert isNonFastIREMessage(.error) />
</#attempt>
<#attempt>
  ${(callMacroFromExpression(interpretTest))!}
<#recover>
  <@assert isNonFastIREMessage(.error) />
</#attempt>

<@assertEquals actual='fails'?eval!'-' expected='-' />
<@assertEquals actual=('fails')?eval!'-' expected='-' />

<#macro isIRE><@assertFails exception="InvalidReferenceException"><#nested></@></#macro>
<#macro isNonFastIRE><@assertFails exception="InvalidReferenceException" message="Tip:"><#nested></@></#macro>
<#function isNonFastIREMessage(msg)><#return msg?contains('Tip:') && msg?contains('null or missing')></#function>
<#function callMacroFromExpression(m)>
  <#local captured><@m /></#local>
  <#return captured>
</#function>
