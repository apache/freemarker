<#setting locale="en_US">
<#setting time_zone="GMT">
<#setting datetime_format="G yyyy-MM-dd HH:mm:ss.S Z">
<#setting date_format="G yyyy-MM-dd Z">
<#setting time_format="HH:mm:ss.S Z">

<@assertEquals expected="AD 1998-10-30 15:30:44.512 +0000" actual='AD 1998-10-30 19:30:44.512 +0400'?datetime?string />
<@assertEquals expected="AD 1998-10-29 +0000" actual='AD 1998-10-30 +0400'?date?string />
<@assertEquals expected="15:30:44.512 +0000" actual='19:30:44.512 +0400'?time?string />

<@assertEquals expected="AD 1998-10-30 15:30:44.512 +0000"
               actual='10/30/1998 19:30:44:512 GMT+04:00'?datetime("MM/dd/yyyy HH:mm:ss:S z")?string />
<@assertEquals expected="AD 1998-10-29 +0000"
               actual='10/30/1998 GMT+04:00'?date("MM/dd/yyyy z")?string />
<@assertEquals expected="15:30:44.512 +0000"
               actual='19:30:44:512 GMT+04:00'?time("HH:mm:ss:S z")?string />

<@assertEquals expected="AD 1998-10-30 15:30:44.512 +0000" actual='1998-10-30T19:30:44.512+04:00'?datetime.xs?string />
<@assertEquals expected="AD 1998-10-29 +0000" actual='1998-10-30+04:00'?date.xs?string />
<@assertEquals expected="15:30:44.512 +0000" actual='19:30:44.512+04:00'?time.xs?string />

<#assign gmtStr='1998-10-30T19:30:44.512'?datetime.xs?string />
<#setting time_zone="GMT+01:00">
<#assign gmt01Str='1998-10-30T19:30:44.512'?datetime.xs?string />
<#setting time_zone="default">
<#assign defStr='1998-10-30T19:30:44.512'?datetime.xs?string />
<@assert test = gmtStr != gmt01Str />
<@assert test = defStr != gmtStr || defStr != gmt01Str />

<#assign refDate = "AD 1998-10-30 +0000"?date>
<#assign refTime = "15:30:44.512 +0000"?time>
<#assign refDateTime = "AD 1998-10-30 15:30:44.512 +0000"?datetime>
<#setting time_zone="UTC">
<#list ['xs', 'xs_nz', 'xs_fz', 'xs s', 'xs ms'] as format>
  <#setting date_format=format>
  <#setting time_format=format>
  <#setting datetime_format=format>
  <@assertEquals expected=refDate actual="1998-10-30Z"?date />
  <@assertEquals expected=refTime actual="15:30:44.512Z"?time />
  <@assertEquals expected=refDateTime actual="1998-10-30T15:30:44.512Z"?datetime />
</#list>
<#list ['iso', 'iso_nz', 'iso_fz', 'iso m'] as format>
  <#setting date_format=format>
  <#setting time_format=format>
  <#setting datetime_format=format>
  <@assertEquals expected=refDate actual="1998-10-30"?date />
  <@assertEquals expected=refDate actual="19981030"?date />
  <@assertEquals expected=refTime actual="15:30:44,512Z"?time />
  <@assertEquals expected=refTime actual="153044,512Z"?time />
  <@assertEquals expected=refDateTime actual="1998-10-30T15:30:44,512Z"?datetime />
  <@assertEquals expected=refDateTime actual="19981030T153044,512Z"?datetime />
</#list>

<#setting time_zone="GMT+01:00">
<#assign refDateTime='1998-10-30T19:30:44.512'?datetime.xs />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512"?datetime.xs />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512"?datetime.iso />
<@assertEquals expected=refDateTime actual="1998-10-30T18:30:44.512"?datetime.xs_u />
<@assertEquals expected=refDateTime actual="1998-10-30T18:30:44.512"?datetime.iso_u />
<#setting time_zone="UTC">
<@assertEquals expected=refDateTime actual="1998-10-30T18:30:44.512"?datetime.xs />
<@assertEquals expected=refDateTime actual="1998-10-30T18:30:44.512"?datetime.iso />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512+01:00"?datetime.xs />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512+01:00"?datetime.xs_u />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512+01"?datetime.iso />
<@assertEquals expected=refDateTime actual="1998-10-30T19:30:44.512+01"?datetime.iso_u />