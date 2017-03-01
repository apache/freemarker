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
<#include 'range-common.ftl'>

<@assertEquals actual=(4..)?size expected=2147483647 />
<@assertEquals actual=limitedJoin(4.., 3) expected="4, 5, 6, ..." />

<@assertEquals actual=(4..)[0] expected=4 />
<@assertEquals actual=(4..)[1] expected=5 />
<@assertEquals actual=(4..)[1000000] expected=1000004 />
<@assertFails message="out of bounds">
	<@assertEquals actual=(4..)[-1] expected=5 />
</@>

<#assign r = 2147483646..>
<@assertEquals actual=r?size expected=2147483647 />
<@assertEquals actual=limitedJoin(r, 3) expected="2147483646, 2147483647, 2147483648, ..." />
<@assertEquals actual=r[100] expected=2147483746 />

<#assign r = -2..>
<@assertEquals actual=limitedJoin(r, 5) expected="-2, -1, 0, 1, 2, ..." />
<@assertEquals actual=r[0] expected=-2 />
<@assertEquals actual=r[1] expected=-1 />

<#function limitedJoin range limit>
	<#assign joined="">
	<#list range as i>
		<#assign joined = joined + i?c>
		<#if i_has_next><#assign joined = joined + ', '></#if>
		<#local limit = limit - 1>
		<#if limit == 0><#assign joined = joined + "..."><#break></#if>
	</#list>
	<#return joined>
</#function>