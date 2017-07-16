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
<#setting timeZone = "UTC">

<@assert test=unknown?isUnknownDateLike />
<@assert test=!timeOnly?isUnknownDateLike />
<@assert test=!dateOnly?isUnknownDateLike />
<@assert test=!dateTime?isUnknownDateLike />

<@assert test=!unknown?isDateOnly />
<@assert test=!timeOnly?isDateOnly />
<@assert test=dateOnly?isDateOnly />
<@assert test=!dateTime?isDateOnly />

<@assert test=!unknown?isTime />
<@assert test=timeOnly?isTime />
<@assert test=!dateOnly?isTime />
<@assert test=!dateTime?isTime />

<@assert test=!unknown?isDatetime />
<@assert test=!timeOnly?isDatetime />
<@assert test=!dateOnly?isDatetime />
<@assert test=dateTime?isDatetime />

<@assertFails message="isn't known if">${unknown?string.xs}</@>
<@assertEquals expected="2003-04-05T06:07:08Z" actual=unknown?dateTimeIfUnknown?string.xs />
<@assertEquals expected="2003-04-05Z" actual=unknown?dateIfUnknown?string.xs />
<@assertEquals expected="06:07:08Z" actual=unknown?timeIfUnknown?string.xs />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=dateTime?dateIfUnknown?string.xs />
<@assertEquals expected="2003-04-05" actual=dateOnly?timeIfUnknown?string.xs />
<@assertEquals expected="06:07:08" actual=timeOnly?dateIfUnknown?string.xs />