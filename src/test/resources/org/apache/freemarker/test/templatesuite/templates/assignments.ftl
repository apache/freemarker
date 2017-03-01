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
<#assign x = 1>
<@assertEquals expected=1 actual=x />
<#assign x = x + 1>
<@assertEquals expected=2 actual=x />
<#assign x += 1>
<@assertEquals expected=3 actual=x />
<#assign x /= 2>
<@assertEquals expected=1.5 actual=x />
<#assign x *= 2>
<@assertEquals expected=3 actual=x />
<#assign x %= 2>
<@assertEquals expected=1 actual=x />
<#assign x += x  x += x  x += x>
<@assertEquals expected=8 actual=x />
<#assign x += x, x += x, x += x>
<@assertEquals expected=64 actual=x />
<#assign x++>
<@assertEquals expected=65 actual=x />
<#assign x-->
<@assertEquals expected=64 actual=x />
<#assign x--, x--, x--, x++, x -= 60>
<@assertEquals expected=2 actual=x />

<#assign x = 'a'>
<#assign x += 1>
<@assertEquals expected='a1' actual=x />

<#assign x = 1>
<#assign x += 'a'>
<@assertEquals expected='1a' actual=x />

<#assign x = [11]>
<#assign x += [22]>
<@assertEquals expected=11 actual=x[0] />
<@assertEquals expected=22 actual=x[1] />
<@assertEquals expected=2 actual=x?size />

<#assign x = { 'a': 11 }>
<#assign x += { 'b': 22 }>
<@assertEquals expected=11 actual=x.a />
<@assertEquals expected=22 actual=x.b />
<@assertEquals expected=2 actual=x?size />

<#assign x = 1>
<#global g = 11>
<#global g -= x>
<@assertEquals expected=10 actual=g />
<#global g *= 2>
<@assertEquals expected=20 actual=g />
<#global g /= 2.5>
<@assertEquals expected=8 actual=g />
<#global g += 2>
<@assertEquals expected=10 actual=g />
<#global g++>
<@assertEquals expected=11 actual=g />
<#global g-->
<@assertEquals expected=10 actual=g />

<#macro m>
    <#local v = x + g>
    <@assertEquals expected=11 actual=v />
    <#local v -= x>
    <@assertEquals expected=10 actual=v />
    <#local v *= 2>
    <@assertEquals expected=20 actual=v />
    <#local v /= 2.5>
    <@assertEquals expected=8 actual=v />
    <#local v += 2>
    <@assertEquals expected=10 actual=v />
    <#local v++>
    <@assertEquals expected=11 actual=v />
    <#local v-->
    <@assertEquals expected=10 actual=v />
</#macro>
<@m />

<#assign foo = 'a'>
<@assertFails messageRegexp=r".*expected.*number.*assignment.*foo.*string.*"><#assign foo -= 1></@>
<@assertFails messageRegexp=r".*expected.*number.*assignment.*foo.*string.*"><#assign foo++></@>
<@assertFails messageRegexp=r".*expected.*number.*assignment.*foo.*string.*"><#assign foo--></@>
<#assign x = 1>
<@assertFails messageRegexp=r"(?s).*expected.*number.*string.*'a'.*"><#assign x -= 'a'></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*-=.*"><#assign noSuchVar -= 1></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*\+=.*"><#assign noSuchVar += 1></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*\*=.*"><#assign noSuchVar *= 1></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*/=.*"><#assign noSuchVar /= 1></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*%=.*"><#assign noSuchVar %= 1></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*\+\+.*"><#assign noSuchVar++></@>
<@assertFails messageRegexp=r"(?s).*assignment.*noSuchVar.*missing.*--.*"><#assign noSuchVar--></@>
<@assertFails messageRegexp=r'(?s).*assignment.*noSuchVar.*missing.*\+=.*"\$".*'><#assign $noSuchVar += 1></@>
