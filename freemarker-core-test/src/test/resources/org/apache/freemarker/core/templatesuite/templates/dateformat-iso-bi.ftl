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
<#assign d = "2010-05-15 22:38:05:23 +0200"?dateTime("yyyy-MM-dd HH:mm:ss:S Z")>
<#setting timeZone="GMT+02">
<@assertEquals actual=d?isoUtc expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?isoUtcMs expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?isoUtcM expected="2010-05-15T20:38Z" />
<@assertEquals actual=d?isoUtcH expected="2010-05-15T20Z" />
<@assertEquals actual=d?isoUtcNZ expected="2010-05-15T20:38:05" />
<@assertEquals actual=d?isoUtcMsNZ expected="2010-05-15T20:38:05.023" />
<@assertEquals actual=d?isoUtcMNZ expected="2010-05-15T20:38" />
<@assertEquals actual=d?isoUtcHNZ expected="2010-05-15T20" />
<@assertEquals actual=d?isoLocal expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?isoLocalMs expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?isoLocalM expected="2010-05-15T22:38+02:00" />
<@assertEquals actual=d?isoLocalH expected="2010-05-15T22+02:00" />
<@assertEquals actual=d?isoLocalNZ expected="2010-05-15T22:38:05" />
<@assertEquals actual=d?isoLocalMsNZ expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?isoLocalMNZ expected="2010-05-15T22:38" />
<@assertEquals actual=d?isoLocalHNZ expected="2010-05-15T22" />

<@assertEquals actual=d?date?isoUtc expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcMs expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcM expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcH expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcMsNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcMNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoUtcHNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocal expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalMs expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalM expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalH expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalMsNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalMNZ expected="2010-05-15" />
<@assertEquals actual=d?date?isoLocalHNZ expected="2010-05-15" />

<@assertEquals actual=d?time?isoUtc expected="20:38:05Z" />
<@assertEquals actual=d?time?isoUtcMs expected="20:38:05.023Z" />
<@assertEquals actual=d?time?isoUtcM expected="20:38Z" />
<@assertEquals actual=d?time?isoUtcH expected="20Z" />
<@assertEquals actual=d?time?isoUtcNZ expected="20:38:05" />
<@assertEquals actual=d?time?isoUtcMsNZ expected="20:38:05.023" />
<@assertEquals actual=d?time?isoUtcMNZ expected="20:38" />
<@assertEquals actual=d?time?isoUtcHNZ expected="20" />
<@assertEquals actual=d?time?isoLocal expected="22:38:05+02:00" />
<@assertEquals actual=d?time?isoLocalMs expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?isoLocalM expected="22:38+02:00" />
<@assertEquals actual=d?time?isoLocalH expected="22+02:00" />
<@assertEquals actual=d?time?isoLocalNZ expected="22:38:05" />
<@assertEquals actual=d?time?isoLocalMsNZ expected="22:38:05.023" />
<@assertEquals actual=d?time?isoLocalMNZ expected="22:38" />
<@assertEquals actual=d?time?isoLocalHNZ expected="22" />

<#assign dStrange = "600-01-01 23:59:59:123 +0000"?dateTime("yyyy-MM-dd HH:mm:ss:S Z")>
<@assertEquals actual=dStrange?isoUtcMs expected="0600-01-03T23:59:59.123Z" />

<#-- java.sql treatment -->
<@assertEquals actual=sqlDate?isoLocal expected="2010-05-15" />
<@assertEquals actual=sqlDate?isoLocalNZ expected="2010-05-15" />
<@assertEquals actual=sqlTime?isoLocalNZ expected="22:38:05" />
<@assertEquals actual=sqlTime?isoUtcNZ expected="20:38:05" />

<#setting timeZone="GMT+03"> <#-- should not mater -->
<@assertEquals actual=d?iso("UTC") expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?isoMs("UTC") expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?isoM("UTC") expected="2010-05-15T20:38Z" />
<@assertEquals actual=d?isoH("UTC") expected="2010-05-15T20Z" />
<@assertEquals actual=d?isoNZ("UTC") expected="2010-05-15T20:38:05" />
<@assertEquals actual=d?isoMsNZ("UTC") expected="2010-05-15T20:38:05.023" />
<@assertEquals actual=d?isoMNZ("UTC") expected="2010-05-15T20:38" />
<@assertEquals actual=d?isoHNZ("UTC") expected="2010-05-15T20" />
<@assertEquals actual=d?iso("GMT+02") expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?isoMs("GMT+02") expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?isoM("GMT+02") expected="2010-05-15T22:38+02:00" />
<@assertEquals actual=d?isoH("GMT+02") expected="2010-05-15T22+02:00" />
<@assertEquals actual=d?isoNZ("GMT+02") expected="2010-05-15T22:38:05" />
<@assertEquals actual=d?isoMsNZ("GMT+02") expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?isoMNZ("GMT+02") expected="2010-05-15T22:38" />
<@assertEquals actual=d?isoHNZ("GMT+02") expected="2010-05-15T22" />

<@assertEquals actual=d?date?iso("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMs("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoM("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoH("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoNZ("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMsNZ("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMNZ("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?isoHNZ("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMs("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoM("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoH("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoNZ("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMsNZ("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoMNZ("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?isoHNZ("GMT+02") expected="2010-05-15" />

<@assertEquals actual=d?time?iso("UTC") expected="20:38:05Z" />
<@assertEquals actual=d?time?isoMs("UTC") expected="20:38:05.023Z" />
<@assertEquals actual=d?time?isoM("UTC") expected="20:38Z" />
<@assertEquals actual=d?time?isoH("UTC") expected="20Z" />
<@assertEquals actual=d?time?isoNZ("UTC") expected="20:38:05" />
<@assertEquals actual=d?time?isoMsNZ("UTC") expected="20:38:05.023" />
<@assertEquals actual=d?time?isoMNZ("UTC") expected="20:38" />
<@assertEquals actual=d?time?isoHNZ("UTC") expected="20" />
<@assertEquals actual=d?time?iso("GMT+02") expected="22:38:05+02:00" />
<@assertEquals actual=d?time?isoMs("GMT+02") expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?isoM("GMT+02") expected="22:38+02:00" />
<@assertEquals actual=d?time?isoH("GMT+02") expected="22+02:00" />
<@assertEquals actual=d?time?isoNZ("GMT+02") expected="22:38:05" />
<@assertEquals actual=d?time?isoMsNZ("GMT+02") expected="22:38:05.023" />
<@assertEquals actual=d?time?isoMNZ("GMT+02") expected="22:38" />
<@assertEquals actual=d?time?isoHNZ("GMT+02") expected="22" />

<@assertEquals actual=d?iso(javaUTC) expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?iso(javaGMT02) expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?iso(adaptedToStringScalar) expected="2010-05-15T22:38:05+02:00" />

<#assign d = "12:00:00:1 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?isoUtcMs expected="12:00:00.001Z" />
<#assign d = "12:00:00:10 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?isoUtcMs expected="12:00:00.01Z" />
<#assign d = "12:00:00:100 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?isoUtcMs expected="12:00:00.1Z" />
<#assign d = "12:00:00:0 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?isoUtcMs expected="12:00:00Z" />

<#setting timeZone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
<@assertEquals actual=d?isoLocal expected="2010-05-15" />
<@assertEquals actual=d?isoUtc expected="2010-05-14" />

<#setting timeZone="GMT+02:30">
<#assign d = "2010-05-15"?dateTime("yyyy-MM-dd")>
<@assertEquals actual=d?isoLocal expected="2010-05-15T00:00:00+02:30" />

<#setting timeZone="America/New_York">
<@assertEquals actual="2010-05-09 20:00 +0000"?dateTime("yyyy-MM-dd HH:mm Z")?isoLocal expected="2010-05-09T16:00:00-04:00" />
<@assertEquals actual="2010-01-01 20:00 +0000"?dateTime("yyyy-MM-dd HH:mm Z")?isoLocal expected="2010-01-01T15:00:00-05:00" />

<@assertFails>${d?iso("no such zone")}</@>

<#setting timeZone="GMT+02">
<@assertEquals actual=sqlTime?isoLocal expected="22:38:05" />
<@assertEquals actual=sqlTime?isoUtc expected="20:38:05" />
