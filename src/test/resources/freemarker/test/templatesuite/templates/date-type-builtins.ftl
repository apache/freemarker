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
<#setting time_zone = "UTC">

<@assert test=unknown?is_unknown_date_like />
<@assert test=!timeOnly?is_unknown_date_like />
<@assert test=!dateOnly?is_unknown_date_like />
<@assert test=!dateTime?is_unknown_date_like />

<@assert test=!unknown?is_date_only />
<@assert test=!timeOnly?is_date_only />
<@assert test=dateOnly?is_date_only />
<@assert test=!dateTime?is_date_only />

<@assert test=!unknown?is_time />
<@assert test=timeOnly?is_time />
<@assert test=!dateOnly?is_time />
<@assert test=!dateTime?is_time />

<@assert test=!unknown?is_datetime />
<@assert test=!timeOnly?is_datetime />
<@assert test=!dateOnly?is_datetime />
<@assert test=dateTime?is_datetime />

<@assertFails message="isn't known if">${unknown?string.xs}</@>
<@assertEquals expected="2003-04-05T06:07:08Z" actual=unknown?datetime_if_unknown?string.xs />
<@assertEquals expected="2003-04-05Z" actual=unknown?date_if_unknown?string.xs />
<@assertEquals expected="06:07:08Z" actual=unknown?time_if_unknown?string.xs />
<@assertEquals expected="2003-04-05T06:07:08Z" actual=dateTime?date_if_unknown?string.xs />
<@assertEquals expected="2003-04-05" actual=dateOnly?time_if_unknown?string.xs />
<@assertEquals expected="06:07:08" actual=timeOnly?date_if_unknown?string.xs />