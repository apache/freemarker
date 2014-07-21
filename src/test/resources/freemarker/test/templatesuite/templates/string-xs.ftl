<#assign d = "2010-05-15 22:38:05:23 +0200"?datetime("yyyy-MM-dd HH:mm:ss:S Z")>
<#setting time_zone="GMT+02">
${d?string.xs} = 2010-05-15T22:38:05.023+02:00
${d?string.xs_nz} = 2010-05-15T22:38:05.023
${d?string.xs_z} = 2010-05-15T22:38:05.023+02:00

${d?date?string.xs} = 2010-05-15+02:00
${d?date?string.xs_nz} = 2010-05-15
${d?date?string.xs_z} = 2010-05-15+02:00

${d?time?string.xs} = 22:38:05.023+02:00
${d?time?string.xs_nz} = 22:38:05.023
${d?time?string.xs_z} = 22:38:05.023+02:00

<#-- java.sql treatment -->
${sqlDate?string.xs} = 2010-05-15
${sqlDate?string.xs_z} = 2010-05-15+02:00
${sqlDate?string.xs_nz} = 2010-05-15
${sqlTime?string.xs} = 22:38:05.023
${sqlTime?string.xs_z} = 22:38:05.023+02:00
${sqlTime?string.xs_nz} = 22:38:05.023

<#assign d = "12:00:00:1 +0200"?time("HH:mm:ss:S Z")>
${d?string.xs} = 12:00:00.001+02:00
<#assign d = "12:00:00:10 +0200"?time("HH:mm:ss:S Z")>
${d?string.xs} = 12:00:00.01+02:00
<#assign d = "12:00:00:100 +0200"?time("HH:mm:ss:S Z")>
${d?string.xs} = 12:00:00.1+02:00
<#assign d = "12:00:00:0 +0200"?time("HH:mm:ss:S Z")>
${d?string.xs} = 12:00:00+02:00

<#setting time_zone="GMT+02">
<#assign d = "2010-05-15"?date("yyyy-MM-dd")>
${d?string.xs} = 2010-05-15+02:00
<#setting time_zone="GMT+00">
${d?string.xs} = 2010-05-14Z

<#setting time_zone="GMT+02:30">
<#assign d = "2010-05-15"?datetime("yyyy-MM-dd")>
${d?string.xs} = 2010-05-15T00:00:00+02:30

<#setting time_zone="GMT-05">
<#setting locale = "en_US">
<#assign d = "BC 0001-05-15"?date("G yyyy-MM-dd")>
<#-- Tests that: (a) BC 1 isn't 0 like in ISO 8601; (b) No Julian calendar is used.  -->
${d?string.xs} = -1-05-13-05:00