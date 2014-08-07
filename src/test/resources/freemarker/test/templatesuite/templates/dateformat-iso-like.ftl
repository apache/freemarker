<#assign d = "2010-05-15 22:38:05:23 +0200"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
<#setting time_zone="GMT+02">
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

<#setting time_zone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
<@assertEquals actual=d?string.xs  expected="2010-05-15+02:00" />
<@assertEquals actual=d?string.iso expected="2010-05-15" />
<#setting time_zone="GMT+00">
<@assertEquals actual=d?string.xs  expected="2010-05-14Z" />
<@assertEquals actual=d?string.iso expected="2010-05-14" />

<#setting time_zone="GMT+02:30">
<#assign d = "2010-05-15"?datetime("yyyy-MM-dd")>
<@assertEquals actual=d?string.xs  expected="2010-05-15T00:00:00+02:30" />
<@assertEquals actual=d?string.iso expected="2010-05-15T00:00:00+02:30" />

<#setting time_zone="GMT-05">
<#setting locale = "en_US">
<#assign d = "BC 0001-05-15"?date("G yyyy-MM-dd")>
<#-- Tests that: (a) BC 1 isn't 0 like in ISO 8601; (b) No Julian calendar is used.  -->
<@assertEquals actual=d?string.xs  expected="-1-05-13-05:00" />
<@assertEquals actual=d?string.iso expected="0000-05-13" />

<#assign dt = "2010-05-15T01:02:03"?datetime.xs>
<#setting datetime_format="xs">
<@assertEquals actual=dt?string expected="2010-05-15T01:02:03-05:00" />
<#setting datetime_format="xs u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting datetime_format="iso u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting datetime_format="xs fz">
<@assertEquals actual=dt?string expected="2010-05-15T01:02:03-05:00" />
<#setting datetime_format="xs fz u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03Z" />
<#setting datetime_format="xs nz u">
<@assertEquals actual=dt?string expected="2010-05-15T06:02:03" />
<#setting datetime_format="iso m nz">
<@assertEquals actual=dt?string expected="2010-05-15T01:02" />

<#assign d = dt?date>
<#setting date_format="xs">
<@assertEquals actual=d?string expected="2010-05-15-05:00" />
<#setting date_format="iso">
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting date_format="xs fz">
<@assertEquals actual=d?string expected="2010-05-15-05:00" />
<#setting date_format="xs fz u">
<@assertEquals actual=d?string expected="2010-05-15Z" />
<#setting date_format="iso fz u">
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting date_format="xs nz">
<@assertEquals actual=d?string expected="2010-05-15" />

<#assign t = dt?time>
<@assertEquals actual=d?string expected="2010-05-15" />
<#setting time_format="xs">
<@assertEquals actual=t?string expected="01:02:03-05:00" />
<#setting time_format="iso_m">
<@assertEquals actual=t?string expected="01:02-05:00" />
<#setting time_format="xs fz">
<@assertEquals actual=t?string expected="01:02:03-05:00" />
<#setting time_format="xs nz">
<@assertEquals actual=t?string expected="01:02:03" />
<#setting time_format="iso nz ms">
<@assertEquals actual=t?string expected="01:02:03.000" />

<@assertFails message="Use ?date, ?time, or ?datetime">${unknownDate?string.xs}</@>
<@assertFails message="Use ?date, ?time, or ?datetime">${unknownDate?string.iso}</@>
<@assertFails message="malformed">${.now?string.xs_fz_nz}</@>
<@assertFails message="malformed">${.now?string.xs_u_fu}</@>
<@assertFails message="malformed">${.now?string.xs_s_ms}</@>
<@assertFails message="malformed">${.now?string.xs_q}</@>
<@assertFails message="malformed">${.now?string.xss}</@>