<#assign d = "2010-05-15 22:38:05:23 +0200"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
<#setting time_zone="GMT+02">
${d?iso_utc} = 2010-05-15T20:38:05Z
${d?iso_utc_ms} = 2010-05-15T20:38:05.023Z
${d?iso_utc_m} = 2010-05-15T20:38Z
${d?iso_utc_h} = 2010-05-15T20Z
${d?iso_utc_nz} = 2010-05-15T20:38:05
${d?iso_utc_ms_nz} = 2010-05-15T20:38:05.023
${d?iso_utc_m_nz} = 2010-05-15T20:38
${d?iso_utc_h_nz} = 2010-05-15T20
${d?iso_local} = 2010-05-15T22:38:05+02:00
${d?iso_local_ms} = 2010-05-15T22:38:05.023+02:00
${d?iso_local_m} = 2010-05-15T22:38+02:00
${d?iso_local_h} = 2010-05-15T22+02:00
${d?iso_local_nz} = 2010-05-15T22:38:05
${d?iso_local_ms_nz} = 2010-05-15T22:38:05.023
${d?iso_local_m_nz} = 2010-05-15T22:38
${d?iso_local_h_nz} = 2010-05-15T22

${d?date?iso_utc} = 2010-05-15
${d?date?iso_utc_ms} = 2010-05-15
${d?date?iso_utc_m} = 2010-05-15
${d?date?iso_utc_h} = 2010-05-15
${d?date?iso_utc_nz} = 2010-05-15
${d?date?iso_utc_ms_nz} = 2010-05-15
${d?date?iso_utc_m_nz} = 2010-05-15
${d?date?iso_utc_h_nz} = 2010-05-15
${d?date?iso_local} = 2010-05-15
${d?date?iso_local_ms} = 2010-05-15
${d?date?iso_local_m} = 2010-05-15
${d?date?iso_local_h} = 2010-05-15
${d?date?iso_local_nz} = 2010-05-15
${d?date?iso_local_ms_nz} = 2010-05-15
${d?date?iso_local_m_nz} = 2010-05-15
${d?date?iso_local_h_nz} = 2010-05-15

${d?time?iso_utc} = 20:38:05Z
${d?time?iso_utc_ms} = 20:38:05.023Z
${d?time?iso_utc_m} = 20:38Z
${d?time?iso_utc_h} = 20Z
${d?time?iso_utc_nz} = 20:38:05
${d?time?iso_utc_ms_nz} = 20:38:05.023
${d?time?iso_utc_m_nz} = 20:38
${d?time?iso_utc_h_nz} = 20
${d?time?iso_local} = 22:38:05+02:00
${d?time?iso_local_ms} = 22:38:05.023+02:00
${d?time?iso_local_m} = 22:38+02:00
${d?time?iso_local_h} = 22+02:00
${d?time?iso_local_nz} = 22:38:05
${d?time?iso_local_ms_nz} = 22:38:05.023
${d?time?iso_local_m_nz} = 22:38
${d?time?iso_local_h_nz} = 22

<#assign dStrange = "600-01-01 23:59:59:123 +0000"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
${dStrange?iso_utc_ms} = 0600-01-01T23:59:59.123Z

<#setting time_zone="GMT+03"> <#-- should not mater -->
${d?iso("UTC")} = 2010-05-15T20:38:05Z
${d?iso_ms("UTC")} = 2010-05-15T20:38:05.023Z
${d?iso_m("UTC")} = 2010-05-15T20:38Z
${d?iso_h("UTC")} = 2010-05-15T20Z
${d?iso_nz("UTC")} = 2010-05-15T20:38:05
${d?iso_ms_nz("UTC")} = 2010-05-15T20:38:05.023
${d?iso_m_nz("UTC")} = 2010-05-15T20:38
${d?iso_h_nz("UTC")} = 2010-05-15T20
${d?iso("GMT+02")} = 2010-05-15T22:38:05+02:00
${d?iso_ms("GMT+02")} = 2010-05-15T22:38:05.023+02:00
${d?iso_m("GMT+02")} = 2010-05-15T22:38+02:00
${d?iso_h("GMT+02")} = 2010-05-15T22+02:00
${d?iso_nz("GMT+02")} = 2010-05-15T22:38:05
${d?iso_ms_nz("GMT+02")} = 2010-05-15T22:38:05.023
${d?iso_m_nz("GMT+02")} = 2010-05-15T22:38
${d?iso_h_nz("GMT+02")} = 2010-05-15T22

${d?date?iso("UTC")} = 2010-05-15
${d?date?iso_ms("UTC")} = 2010-05-15
${d?date?iso_m("UTC")} = 2010-05-15
${d?date?iso_h("UTC")} = 2010-05-15
${d?date?iso_nz("UTC")} = 2010-05-15
${d?date?iso_ms_nz("UTC")} = 2010-05-15
${d?date?iso_m_nz("UTC")} = 2010-05-15
${d?date?iso_h_nz("UTC")} = 2010-05-15
${d?date?iso("GMT+02")} = 2010-05-15
${d?date?iso_ms("GMT+02")} = 2010-05-15
${d?date?iso_m("GMT+02")} = 2010-05-15
${d?date?iso_h("GMT+02")} = 2010-05-15
${d?date?iso_nz("GMT+02")} = 2010-05-15
${d?date?iso_ms_nz("GMT+02")} = 2010-05-15
${d?date?iso_m_nz("GMT+02")} = 2010-05-15
${d?date?iso_h_nz("GMT+02")} = 2010-05-15

${d?time?iso("UTC")} = 20:38:05Z
${d?time?iso_ms("UTC")} = 20:38:05.023Z
${d?time?iso_m("UTC")} = 20:38Z
${d?time?iso_h("UTC")} = 20Z
${d?time?iso_nz("UTC")} = 20:38:05
${d?time?iso_ms_nz("UTC")} = 20:38:05.023
${d?time?iso_m_nz("UTC")} = 20:38
${d?time?iso_h_nz("UTC")} = 20
${d?time?iso("GMT+02")} = 22:38:05+02:00
${d?time?iso_ms("GMT+02")} = 22:38:05.023+02:00
${d?time?iso_m("GMT+02")} = 22:38+02:00
${d?time?iso_h("GMT+02")} = 22+02:00
${d?time?iso_nz("GMT+02")} = 22:38:05
${d?time?iso_ms_nz("GMT+02")} = 22:38:05.023
${d?time?iso_m_nz("GMT+02")} = 22:38
${d?time?iso_h_nz("GMT+02")} = 22

${d?iso(javaUTC)} = 2010-05-15T20:38:05Z
${d?iso(javaGMT02)} = 2010-05-15T22:38:05+02:00
${d?iso(adaptedToStringScalar)} = 2010-05-15T22:38:05+02:00

<#assign d = "12:00:00:1 +0000"?time("HH:mm:ss:S Z")>
${d?iso_utc_ms} = 12:00:00.001Z
<#assign d = "12:00:00:10 +0000"?time("HH:mm:ss:S Z")>
${d?iso_utc_ms} = 12:00:00.01Z
<#assign d = "12:00:00:100 +0000"?time("HH:mm:ss:S Z")>
${d?iso_utc_ms} = 12:00:00.1Z
<#assign d = "12:00:00:0 +0000"?time("HH:mm:ss:S Z")>
${d?iso_utc_ms} = 12:00:00Z

<#setting time_zone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
${d?iso_local} = 2010-05-15
${d?iso_utc} = 2010-05-14

<#setting time_zone="GMT+02:30">
<#assign d = "2010-05-15"?datetime("yyyy-MM-dd")>
${d?iso_local} = 2010-05-15T00:00:00+02:30

<#setting time_zone="America/New_York">
${"2010-05-09 20:00 +0000"?datetime("yyyy-MM-dd HH:mm Z")?iso_local} = 2010-05-09T16:00:00-04:00
${"2010-01-01 20:00 +0000"?datetime("yyyy-MM-dd HH:mm Z")?iso_local} = 2010-01-01T15:00:00-05:00

<#attempt>
  ${d?iso("no such zone")}
<#recover>
  unrecognized time zone name
</#attempt>