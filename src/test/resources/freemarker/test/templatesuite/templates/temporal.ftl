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
<@assertEquals expected="Apr 5, 2003 7:07:08 AM" actual=dateTime?string />
<@assertEquals expected="Apr 5, 2003 7:07:08 AM" actual=instant?string />
<@assertEquals expected="Apr 5, 2003 6:07:08 AM" actual=localDateTime?string />
<@assertEquals expected="Apr 5, 2003" actual=localDate?string />
<@assertEquals expected="6:07:08 AM" actual=localTime?string />
<@assertEquals expected="Apr 5, 2003 7:07:08 AM" actual=offsetDateTime?string />
<@assertEquals expected="6:07:08 AM Z" actual=offsetTime?string />
<@assertEquals expected="2003" actual=year?string />
<@assertEquals expected="2003-04" actual=yearMonth?string />
<@assertEquals expected="Apr 5, 2003 7:07:08 AM" actual=zonedDateTime?string />

<#setting timeZone="America/New_York">
<@assertEquals expected="2003-04-05T01:07:08-05:00" actual=instant?string.iso />
<@assertEquals expected="2003-04-05T06:07:08" actual=localDateTime?string.iso />
<@assertEquals expected="2003-04-05" actual=localDate?string.iso />
<@assertEquals expected="06:07:08" actual=localTime?string.iso />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=offsetDateTime?string.iso />
<@assertEquals expected="06:07:08Z" actual=offsetTime?string.iso />
<@assertEquals expected="2003" actual=year?string.iso />
<@assertEquals expected="2003-04" actual=yearMonth?string.iso />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=zonedDateTime?string.iso />

<#setting timeZone="UTC">
<@assertEquals expected="2003-04-05T06:07:08Z" actual=instant?string.iso />
<@assertEquals expected="2003-04-05T06:07:08" actual=localDateTime?string.iso />
<@assertEquals expected="2003-04-05" actual=localDate?string.iso />
<@assertEquals expected="06:07:08" actual=localTime?string.iso />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=offsetDateTime?string.iso />
<@assertEquals expected="06:07:08Z" actual=offsetTime?string.iso />
<@assertEquals expected="2003" actual=year?string.iso />
<@assertEquals expected="2003-04" actual=yearMonth?string.iso />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=zonedDateTime?string.iso />

<@assertEquals expected="2003-04-05T06:07:08Z" actual=instant?string.xs />
<@assertEquals expected="2003-04-05T06:07:08" actual=localDateTime?string.xs />
<@assertEquals expected="2003-04-05" actual=localDate?string.xs />
<@assertEquals expected="06:07:08" actual=localTime?string.xs />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=offsetDateTime?string.xs />
<@assertEquals expected="06:07:08Z" actual=offsetTime?string.xs />
<@assertEquals expected="2003" actual=year?string.xs />
<@assertEquals expected="2003-04" actual=yearMonth?string.xs />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=zonedDateTime?string.xs />

<#setting timeZone="America/New_York">
<#setting locale="fr_FR">
<@assertEquals expected="05/04/03 01:07" actual=instant?string.short />
<@assertEquals expected="5 avr. 2003 01:07:08" actual=instant?string.medium />
<@assertEquals expected="5 avril 2003 01:07:08 EST" actual=instant?string.long />
<@assertEquals expected="samedi 5 avril 2003 01 h 07 EST" actual=instant?string.full />

<@assertEquals expected="05/04/03 01:07" actual=offsetDateTime?string.short />
<@assertEquals expected="5 avr. 2003 01:07:08" actual=offsetDateTime?string.medium />
<@assertEquals expected="5 avril 2003 06:07:08 Z" actual=offsetDateTime?string.long />
<@assertEquals expected="samedi 5 avril 2003 06 h 07 Z" actual=offsetDateTime?string.full />

<@assertEquals expected="05/04/03 06:07" actual=localDateTime?string.short />
<@assertEquals expected="5 avr. 2003 06:07:08" actual=localDateTime?string.medium />
<#-- These fail on Java 8 because of JDK-8085887
<@assertEquals expected="5 avril 2003 06:07:08 ET" actual=localDateTime?string.long />
<@assertEquals expected="samedi 5 avril 2003 06 h 07 ET" actual=localDateTime?string.full />
-->

<#-- There combinations are clearly not supported by the "localized pattern" API. -->
<@assertFails message="not supported for java.time.Year">${year?string.short}</@>
<@assertFails message="not supported for java.time.Year">${year?string.medium}</@>
<@assertFails message="not supported for java.time.Year">${year?string.long}</@>
<@assertFails message="not supported for java.time.Year">${year?string.full}</@>
<@assertFails message="not supported for java.time.YearMonth">${yearMonth?string.short}</@>
<@assertFails message="not supported for java.time.YearMonth">${yearMonth?string.medium}</@>
<@assertFails message="not supported for java.time.YearMonth">${yearMonth?string.long}</@>
<@assertFails message="not supported for java.time.YearMonth">${yearMonth?string.full}</@>

<@assertEquals expected="05/04/03 01:07" actual=zonedDateTime?string.short />
<@assertEquals expected="5 avr. 2003 01:07:08" actual=zonedDateTime?string.medium />
<@assertEquals expected="5 avril 2003 06:07:08 UTC" actual=zonedDateTime?string.long />
<@assertEquals expected="samedi 5 avril 2003 06 h 07 UTC" actual=zonedDateTime?string.full />

<@assertEquals expected="05/04/03 06:07" actual=localDateTime?string.short_short />
<@assertEquals expected="05/04/03 06:07:08" actual=localDateTime?string.short_medium />
<#-- These fail on Java 8 because of JDK-8085887
<@assertEquals expected="05/04/03 06:07:08 ET" actual=localDateTime?string.short_long />
<@assertEquals expected="05/04/03 06 h 07 ET" actual=localDateTime?string.short_full />
-->

<@assertEquals expected="5 avr. 2003 06:07:08" actual=localDateTime?string.medium_medium />
<@assertEquals expected="5 avril 2003 06:07:08" actual=localDateTime?string.long_medium />
<@assertEquals expected="samedi 5 avril 2003 06:07:08" actual=localDateTime?string.full_medium />

<#-- These fail on Java 8 because of JDK-8085887
<@assertEquals expected="5 avril 2003 06:07:08 ET" actual=localDateTime?string.long_long />
<@assertEquals expected="samedi 5 avril 2003 06:07:08 ET" actual=localDateTime?string.full_long />
-->

<#-- These fail on Java 8 because of JDK-8085887
<@assertEquals expected="samedi 5 avril 2003 06 h 07 ET" actual=localDateTime?string.full_full />
-->

<@assertEquals expected="2003-04-05" actual=localDateTime?string('yyyy-MM-dd') />
<@assertEquals expected="2003-04-05 06:07:08" actual=localDateTime?string('yyyy-MM-dd HH:mm:ss') />


<#setting locale="en_US">
<#setting datetimeFormat="yyyy MMM dd HH:mm:ss">
<@assertEquals expected="2003 Apr 05 01:07:08" actual=instant?string />
<#setting datetimeFormat="yyyy MMM dd HH:mm:ss">
<@assertEquals expected="2003 Apr 05 06:07:08" actual=localDateTime?string />
<#setting dateFormat="yyyy MMM dd">
<@assertEquals expected="2003 Apr 05" actual=localDate?string />
<#setting datetimeFormat="HH:mm:ss">
<@assertEquals expected="6:07:08 AM" actual=localTime?string />
<#setting datetimeFormat="yyyy MMM dd HH:mm:ss">
<@assertEquals expected="2003 Apr 05 01:07:08" actual=offsetDateTime?string />
<#setting yearFormat="yyyy">
<@assertEquals expected="2003" actual=year?string />
<#setting yearMonthFormat="yyyy MMM">
<@assertEquals expected="2003 Apr" actual=yearMonth?string />
<#setting datetimeFormat="yyyy MMM dd HH:mm:ss">
<@assertEquals expected="2003 Apr 05 01:07:08" actual=zonedDateTime?string />
