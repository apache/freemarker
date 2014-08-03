<#include "dateformat-iso-bi-common.ftl">

<#setting time_zone="GMT+02">
<@assertEquals actual=sqlTime?iso_local expected="22:38:05+02:00" />
<@assertEquals actual=sqlTime?iso_utc expected="20:38:05Z" />
