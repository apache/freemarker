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
<@assertEquals actual=d?string.xs  expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string.iso expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string('xs')  expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string('iso') expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string.xs_nz  expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?string.iso_nz expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?string.xs_fz  expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string.iso_fz expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?string.xs_u  expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?string.iso_u expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?string.xs_s_u  expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?string.iso_s_u expected="2010-05-15T20:38:05Z" />

<@assertEquals actual=d?date?string.xs  expected="2010-05-15+02:00" />
<@assertEquals actual=d?date?string.iso expected="2010-05-15" />
<@assertEquals actual=d?date?string.xs_nz  expected="2010-05-15" />
<@assertEquals actual=d?date?string.iso_nz expected="2010-05-15" />
<@assertEquals actual=d?date?string.xs_fz  expected="2010-05-15+02:00" />
<@assertEquals actual=d?date?string.iso_fz expected="2010-05-15" />

<@assertEquals actual=d?time?string.xs  expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?string.iso expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?string.xs_nz  expected="22:38:05.023" />
<@assertEquals actual=d?time?string.iso_nz expected="22:38:05.023" />
<@assertEquals actual=d?time?string.xs_fz  expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?string.iso_fz expected="22:38:05.023+02:00" />

<#-- java.sql treatment -->
<@assertEquals actual=sqlDate?string.xs  expected="2010-05-15" />
<@assertEquals actual=sqlDate?string.iso expected="2010-05-15" />
<@assertEquals actual=sqlDate?string.xs_fz  expected="2010-05-15+02:00" />
<@assertEquals actual=sqlDate?string.iso_fz expected="2010-05-15" />
<@assertEquals actual=sqlDate?string.xs_nz  expected="2010-05-15" />
<@assertEquals actual=sqlDate?string.iso_nz expected="2010-05-15" />
<@assertEquals actual=sqlTime?string.xs  expected="22:38:05.023" />
<@assertEquals actual=sqlTime?string.iso expected="22:38:05.023" />
<@assertEquals actual=sqlTime?string.xs_fz  expected="22:38:05.023+02:00" />
<@assertEquals actual=sqlTime?string.iso_fz expected="22:38:05.023+02:00" />
<@assertEquals actual=sqlTime?string.xs_nz  expected="22:38:05.023" />
<@assertEquals actual=sqlTime?string.iso_nz expected="22:38:05.023" />

<#assign d = "12:30:15:1 +0200"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?string.xs  expected="12:30:15.001+02:00" />
<@assertEquals actual=d?string.iso expected="12:30:15.001+02:00" />
<@assertEquals actual=d?string.xs_ms  expected="12:30:15.001+02:00" />
<@assertEquals actual=d?string.iso_ms expected="12:30:15.001+02:00" />
<@assertEquals actual=d?string.iso_s expected="12:30:15+02:00" />
<@assertEquals actual=d?string.iso_m expected="12:30+02:00" />
<@assertEquals actual=d?string.iso_h expected="12+02:00" />
<#assign d = "12:30:15:10 +0200"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?string.xs  expected="12:30:15.01+02:00" />
<@assertEquals actual=d?string.iso expected="12:30:15.01+02:00" />
<@assertEquals actual=d?string.xs_ms  expected="12:30:15.010+02:00" />
<@assertEquals actual=d?string.iso_ms expected="12:30:15.010+02:00" />
<#assign d = "12:30:15:100 +0200"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?string.xs  expected="12:30:15.1+02:00" />
<@assertEquals actual=d?string.iso expected="12:30:15.1+02:00" />
<@assertEquals actual=d?string.xs_ms  expected="12:30:15.100+02:00" />
<@assertEquals actual=d?string.iso_ms expected="12:30:15.100+02:00" />
<#assign d = "12:30:15:0 +0200"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?string.xs  expected="12:30:15+02:00" />
<@assertEquals actual=d?string.iso expected="12:30:15+02:00" />
<@assertEquals actual=d?string.xs_ms  expected="12:30:15.000+02:00" />
<@assertEquals actual=d?string.iso_ms expected="12:30:15.000+02:00" />

<#setting timeZone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
<@assertEquals actual=d?string.xs  expected="2010-05-15+02:00" />
<@assertEquals actual=d?string.iso expected="2010-05-15" />
<#setting timeZone="GMT+00">
<@assertEquals actual=d?string.xs  expected="2010-05-14Z" />
<@assertEquals actual=d?string.iso expected="2010-05-14" />

<#setting timeZone="GMT+02:30">
<#assign d = "2010-05-15"?dateTime("yyyy-MM-dd")>
<@assertEquals actual=d?string.xs  expected="2010-05-15T00:00:00+02:30" />
<@assertEquals actual=d?string.iso expected="2010-05-15T00:00:00+02:30" />

<#setting timeZone="GMT-05">
<#setting locale = "en_US">
<#assign d = "BC 0001-05-15"?date("G yyyy-MM-dd")>
<#-- Tests that: (a) BC 1 isn't 0 like in ISO 8601; (b) No Julian calendar is used.  -->
<@assertEquals actual=d?string.xs  expected="-1-05-13-05:00" />
<@assertEquals actual=d?string.iso expected="0000-05-13" />

<#assign dt = "2010-05-15T01:02:03"?dateTime.xs>
<#setting dateTimeFormat="xs">
<@assertEquals actual=dt?string expected="2010-05-15T01:02:03-05:00" />
<#setting dateTimeFormat="xs u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting dateTimeFormat="iso u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting dateTimeFormat="xs fz">
<@assertEquals actual=dt?string expected="2010-05-15T01:02:03-05:00" />
<#setting dateTimeFormat="xs fz u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting dateTimeFormat="xs nz u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03" />
<#setting dateTimeFormat="iso m nz">
<@assertEquals actual=dt?string expected="2010-05-15T01:02" />

<#assign d = dt?date>
<#setting dateFormat="xs">
<@assertEquals actual=d?string expected="2010-05-15-05:00" />
<#setting dateFormat="iso">
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting dateFormat="xs fz">
<@assertEquals actual=d?string expected="2010-05-15-05:00" />
<#setting dateFormat="xs fz u">
<@assertEquals actual=d?string expected="2010-05-15Z" />
<#setting dateFormat="iso fz u">
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting dateFormat="xs nz">
<@assertEquals actual=d?string expected="2010-05-15" />

<#assign t = dt?time>
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting timeFormat="xs">
<@assertEquals actual=t?string expected="01:02:03-05:00" />
<#setting timeFormat="iso_m">
<@assertEquals actual=t?string expected="01:02-05:00" />
<#setting timeFormat="xs fz">
<@assertEquals actual=t?string expected="01:02:03-05:00" />
<#setting timeFormat="xs nz">
<@assertEquals actual=t?string expected="01:02:03" />
<#setting timeFormat="iso nz ms">
<@assertEquals actual=t?string expected="01:02:03.000" />

<@assertFails message="Use ?date, ?time, or ?dateTime">${unknownDate?string.xs}</@>
<@assertFails message="Use ?date, ?time, or ?dateTime">${unknownDate?string.iso}</@>
<@assertFails message="format string">${.now?string.xs_fz_nz}</@>
<@assertFails message="format string">${.now?string.xs_u_fu}</@>
<@assertFails message="format string">${.now?string.xs_s_ms}</@>
<@assertFails message="format string">${.now?string.xs_q}</@>
<@assertFails message="format string">${.now?string.xss}</@>