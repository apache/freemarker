<#setting locale='de_DE'>
<@assertEquals expected='de_DE' actual=.locale />
<@assertEquals expected='de' actual=.lang />
<@assertEquals expected='java.util.Locale "de_DE"' actual=javaObjectInfo.info(.locale_object) />

<#setting number_format="'f'#">
<@assertEquals expected='f1' actual=1?string />

<#setting boolean_format="t,f">
<@assertEquals expected='t' actual=true?string />

<#setting date_format="'df'">
<@assertEquals expected='df' actual=.now?date?string />

<#setting time_format="'tf'">
<@assertEquals expected='tf' actual=.now?time?string />

<#setting datetime_format="'dtf'">
<@assertEquals expected='dtf' actual=.now?string />

<#setting time_zone='GMT+00'>
<#assign t1='2000'?datetime('yyyy')>
<#setting time_zone='GMT+01'>
<#assign t2='2000'?datetime('yyyy')>
<@assertEquals expected=1000*60*60 actual=t1?long-t2?long />

<#setting sql_date_and_time_time_zone='GMT+01'>

<#setting url_escaping_charset='ISO-8859-1'>
<@assertEquals expected='%E1' actual='á'?url />
<#setting url_escaping_charset='UTF-8'>
<@assertEquals expected='%C3%A1' actual='á'?url />

<@assertFails>${noSuchWar}</@assertFails>
<#setting classic_compatible=true>
<@assertEquals expected='[]' actual="[${noSuchWar}]" />

<#setting output_encoding="ISO-8859-2">
<@assertEquals expected="ISO-8859-2" actual=.output_encoding />
