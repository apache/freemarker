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
<#assign f1InvocationCnt = 0>
<#assign f2InvocationCnt = 0>

<@assertEquals expected="f1 1" actual=true?then(f1(), f2()) />
<@assertEquals expected="f2 1" actual=false?then(f1(), f2()) />
<@assertEquals expected="f1 2" actual=true?then(f1(), f2()) />
<@assertEquals expected="f2 2" actual=false?then(f1(), f2()) />
<@assertEquals expected=2 actual=f1InvocationCnt />
<@assertEquals expected=2 actual=f2InvocationCnt />

<#function f1>
  <#assign f1InvocationCnt++>
  <#return "f1 " + f1InvocationCnt>
</#function>

<#function f2>
  <#assign f2InvocationCnt++>
  <#return "f2 " + f2InvocationCnt>
</#function>

<#assign x = 1>
<@assertEquals expected='Y' actual=(x < 2 * x)?then(-x < x, false)?then('Y', 'N') />

<@assertEquals expected=1 actual=true?then(x, noSuchVar) />
<@assertEquals expected=1 actual=false?then(noSuchVar, x) />

<@assertFails message="noSuchVar1">${true?then(noSuchVar1, noSuchVar2)}</@>
<@assertFails message="noSuchVar2">${false?then(noSuchVar1, noSuchVar2)}</@>
<@assertFails message="noSuchVar3">${noSuchVar3?then(noSuchVar1, noSuchVar2)}</@>

<#assign out><#escape x as x?then(1, 0)>${false} ${true}</#escape></#assign>
<@assertEquals expected="0 1" actual=out />

<#assign out><#escape x as (x < 0)?then(-x * 3, x * 2)>${-1} ${1}</#escape></#assign>
<@assertEquals expected="3 2" actual=out />