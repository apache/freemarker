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
<#setting locale="en_US">
<#setting time_zone="GMT">
<#setting datetime_format="">
${date}
${unknownDate?datetime}
${date?string}
${date?string[""]}
${date?string.short}
${date?string.medium}
${date?string.long}
${date?string.short_short}
${date?string.short_medium}
${date?string.short_long}
${date?string.medium_short}
${date?string.medium_medium}
${date?string.medium_long}
${date?string.long_short}
${date?string.long_medium}
${date?string.long_long}
${unknownDate?date}
${date?date?string[""]}
${date?date?string.short}
${date?date?string.medium}
${date?date?string.long}
${unknownDate?time}
${date?time?string[""]}
${date?time?string.short}
${date?time?string.medium}
${date?time?string.long}
<#setting locale="hu_hu">
<#setting datetime_format="long_long">
${date}
<#setting locale="en_US">
<#setting datetime_format="EEE, dd MMM yyyyy HH:mm:ss z">
${date}
${unknownDate?string["EEE, dd MMM yyyy HH:mm:ss z"]}
${unknownDate?string("EEE, dd MMM yyyy HH:mm:ss z")}
${unknownDate?string.yyyy}

<#setting datetime_format="yyyy">
<#assign s = date?string>
${s}
<#setting datetime_format="MM">
${s}

<#-- Check ?string lazy evaluation bug was fixed: -->
<#setting datetime_format="yyyy">
<#assign s = date?string>
<#-- no ${s} -->
<#setting datetime_format="MM">
${s}
<#assign s = date?string>
${s}