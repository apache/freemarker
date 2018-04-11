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
<#-- Removes US format differences introduced in Java 9: -->
<#function n(s)>
  <#return s?replace('2002,', '2002')?replace('/02,', '/02')?replace(' at', '')>
</#function>
<#setting locale="en_US">
<#setting timeZone="GMT">
<#setting dateTimeFormat="">
${n(date)}
${n(unknownDate?dateTime)}
${n(date?string)}
${n(date?string[""])}
${n(date?string.short)}
${n(date?string.medium)}
${n(date?string.long)}
${n(date?string.short_short)}
${n(date?string.short_medium)}
${n(date?string.short_long)}
${n(date?string.medium_short)}
${n(date?string.medium_medium)}
${n(date?string.medium_long)}
${n(date?string.long_short)}
${n(date?string.long_medium)}
${n(date?string.long_long)}
${n(unknownDate?date)}
${n(date?date?string[""])}
${n(date?date?string.short)}
${n(date?date?string.medium)}
${n(date?date?string.long)}
${n(unknownDate?time)}
${n(date?time?string[""])}
${n(date?time?string.short)}
${n(date?time?string.medium)}
${n(date?time?string.long)}
<#setting locale="hu_hu">
<#setting dateTimeFormat="long_long">
${date}
<#setting locale="en_US">
<#setting dateTimeFormat="EEE, dd MMM yyyyy HH:mm:ss z">
${date}
${unknownDate?string["EEE, dd MMM yyyy HH:mm:ss z"]}
${unknownDate?string("EEE, dd MMM yyyy HH:mm:ss z")}
${unknownDate?string.yyyy}

<#setting dateTimeFormat="yyyy">
<#assign s = date?string>
${s}
<#setting dateTimeFormat="MM">
${s}

<#-- Check ?string lazy evaluation bug was fixed: -->
<#setting dateTimeFormat="yyyy">
<#assign s = date?string>
<#-- no ${s} -->
<#setting dateTimeFormat="MM">
${s}
<#assign s = date?string>
${s}