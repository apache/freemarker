<#assign d = "2010-05-15 22:38:05:23 +0200"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
<#setting time_zone="GMT+02">
<@assertEquals actual=d?iso_utc expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?iso_utc_ms expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?iso_utc_m expected="2010-05-15T20:38Z" />
<@assertEquals actual=d?iso_utc_h expected="2010-05-15T20Z" />
<@assertEquals actual=d?iso_utc_nz expected="2010-05-15T20:38:05" />
<@assertEquals actual=d?iso_utc_ms_nz expected="2010-05-15T20:38:05.023" />
<@assertEquals actual=d?iso_utc_m_nz expected="2010-05-15T20:38" />
<@assertEquals actual=d?iso_utc_h_nz expected="2010-05-15T20" />
<@assertEquals actual=d?iso_local expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?iso_local_ms expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?iso_local_m expected="2010-05-15T22:38+02:00" />
<@assertEquals actual=d?iso_local_h expected="2010-05-15T22+02:00" />
<@assertEquals actual=d?iso_local_nz expected="2010-05-15T22:38:05" />
<@assertEquals actual=d?iso_local_ms_nz expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?iso_local_m_nz expected="2010-05-15T22:38" />
<@assertEquals actual=d?iso_local_h_nz expected="2010-05-15T22" />

<@assertEquals actual=d?date?iso_utc expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_ms expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_m expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_h expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_ms_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_m_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_utc_h_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_ms expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_m expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_h expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_ms_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_m_nz expected="2010-05-15" />
<@assertEquals actual=d?date?iso_local_h_nz expected="2010-05-15" />

<@assertEquals actual=d?time?iso_utc expected="20:38:05Z" />
<@assertEquals actual=d?time?iso_utc_ms expected="20:38:05.023Z" />
<@assertEquals actual=d?time?iso_utc_m expected="20:38Z" />
<@assertEquals actual=d?time?iso_utc_h expected="20Z" />
<@assertEquals actual=d?time?iso_utc_nz expected="20:38:05" />
<@assertEquals actual=d?time?iso_utc_ms_nz expected="20:38:05.023" />
<@assertEquals actual=d?time?iso_utc_m_nz expected="20:38" />
<@assertEquals actual=d?time?iso_utc_h_nz expected="20" />
<@assertEquals actual=d?time?iso_local expected="22:38:05+02:00" />
<@assertEquals actual=d?time?iso_local_ms expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?iso_local_m expected="22:38+02:00" />
<@assertEquals actual=d?time?iso_local_h expected="22+02:00" />
<@assertEquals actual=d?time?iso_local_nz expected="22:38:05" />
<@assertEquals actual=d?time?iso_local_ms_nz expected="22:38:05.023" />
<@assertEquals actual=d?time?iso_local_m_nz expected="22:38" />
<@assertEquals actual=d?time?iso_local_h_nz expected="22" />

<#assign dStrange = "600-01-01 23:59:59:123 +0000"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
<@assertEquals actual=dStrange?iso_utc_ms expected="0600-01-03T23:59:59.123Z" />

<#-- java.sql treatment -->
<@assertEquals actual=sqlDate?iso_local expected="2010-05-15" />
<@assertEquals actual=sqlDate?iso_local_nz expected="2010-05-15" />
<@assertEquals actual=sqlTime?iso_local_nz expected="22:38:05" />
<@assertEquals actual=sqlTime?iso_utc_nz expected="20:38:05" />

<#setting time_zone="GMT+03"> <#-- should not mater -->
<@assertEquals actual=d?iso("UTC") expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?iso_ms("UTC") expected="2010-05-15T20:38:05.023Z" />
<@assertEquals actual=d?iso_m("UTC") expected="2010-05-15T20:38Z" />
<@assertEquals actual=d?iso_h("UTC") expected="2010-05-15T20Z" />
<@assertEquals actual=d?iso_nz("UTC") expected="2010-05-15T20:38:05" />
<@assertEquals actual=d?iso_ms_nz("UTC") expected="2010-05-15T20:38:05.023" />
<@assertEquals actual=d?iso_m_nz("UTC") expected="2010-05-15T20:38" />
<@assertEquals actual=d?iso_h_nz("UTC") expected="2010-05-15T20" />
<@assertEquals actual=d?iso("GMT+02") expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?iso_ms("GMT+02") expected="2010-05-15T22:38:05.023+02:00" />
<@assertEquals actual=d?iso_m("GMT+02") expected="2010-05-15T22:38+02:00" />
<@assertEquals actual=d?iso_h("GMT+02") expected="2010-05-15T22+02:00" />
<@assertEquals actual=d?iso_nz("GMT+02") expected="2010-05-15T22:38:05" />
<@assertEquals actual=d?iso_ms_nz("GMT+02") expected="2010-05-15T22:38:05.023" />
<@assertEquals actual=d?iso_m_nz("GMT+02") expected="2010-05-15T22:38" />
<@assertEquals actual=d?iso_h_nz("GMT+02") expected="2010-05-15T22" />

<@assertEquals actual=d?date?iso("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_ms("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_m("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_h("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_nz("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_ms_nz("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_m_nz("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_h_nz("UTC") expected="2010-05-15" />
<@assertEquals actual=d?date?iso("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_ms("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_m("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_h("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_nz("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_ms_nz("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_m_nz("GMT+02") expected="2010-05-15" />
<@assertEquals actual=d?date?iso_h_nz("GMT+02") expected="2010-05-15" />

<@assertEquals actual=d?time?iso("UTC") expected="20:38:05Z" />
<@assertEquals actual=d?time?iso_ms("UTC") expected="20:38:05.023Z" />
<@assertEquals actual=d?time?iso_m("UTC") expected="20:38Z" />
<@assertEquals actual=d?time?iso_h("UTC") expected="20Z" />
<@assertEquals actual=d?time?iso_nz("UTC") expected="20:38:05" />
<@assertEquals actual=d?time?iso_ms_nz("UTC") expected="20:38:05.023" />
<@assertEquals actual=d?time?iso_m_nz("UTC") expected="20:38" />
<@assertEquals actual=d?time?iso_h_nz("UTC") expected="20" />
<@assertEquals actual=d?time?iso("GMT+02") expected="22:38:05+02:00" />
<@assertEquals actual=d?time?iso_ms("GMT+02") expected="22:38:05.023+02:00" />
<@assertEquals actual=d?time?iso_m("GMT+02") expected="22:38+02:00" />
<@assertEquals actual=d?time?iso_h("GMT+02") expected="22+02:00" />
<@assertEquals actual=d?time?iso_nz("GMT+02") expected="22:38:05" />
<@assertEquals actual=d?time?iso_ms_nz("GMT+02") expected="22:38:05.023" />
<@assertEquals actual=d?time?iso_m_nz("GMT+02") expected="22:38" />
<@assertEquals actual=d?time?iso_h_nz("GMT+02") expected="22" />

<@assertEquals actual=d?iso(javaUTC) expected="2010-05-15T20:38:05Z" />
<@assertEquals actual=d?iso(javaGMT02) expected="2010-05-15T22:38:05+02:00" />
<@assertEquals actual=d?iso(adaptedToStringScalar) expected="2010-05-15T22:38:05+02:00" />

<#assign d = "12:00:00:1 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?iso_utc_ms expected="12:00:00.001Z" />
<#assign d = "12:00:00:10 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?iso_utc_ms expected="12:00:00.01Z" />
<#assign d = "12:00:00:100 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?iso_utc_ms expected="12:00:00.1Z" />
<#assign d = "12:00:00:0 +0000"?time("HH:mm:ss:S Z")>
<@assertEquals actual=d?iso_utc_ms expected="12:00:00Z" />

<#setting time_zone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
<@assertEquals actual=d?iso_local expected="2010-05-15" />
<@assertEquals actual=d?iso_utc expected="2010-05-14" />

<#setting time_zone="GMT+02:30">
<#assign d = "2010-05-15"?datetime("yyyy-MM-dd")>
<@assertEquals actual=d?iso_local expected="2010-05-15T00:00:00+02:30" />

<#setting time_zone="America/New_York">
<@assertEquals actual="2010-05-09 20:00 +0000"?datetime("yyyy-MM-dd HH:mm Z")?iso_local expected="2010-05-09T16:00:00-04:00" />
<@assertEquals actual="2010-01-01 20:00 +0000"?datetime("yyyy-MM-dd HH:mm Z")?iso_local expected="2010-01-01T15:00:00-05:00" />

<@assertFails>${d?iso("no such zone")}</@>
