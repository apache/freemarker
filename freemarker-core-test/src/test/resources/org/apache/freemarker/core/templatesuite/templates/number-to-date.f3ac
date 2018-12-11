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
${1305575275540?numberToDatetime?isoUtcMs} == 2011-05-16T19:47:55.54Z
${1305575275540?numberToDate?isoUtc} == 2011-05-16
${1305575275540?numberToTime?isoUtcMs} == 19:47:55.54Z

${1305575275540?long?numberToDatetime?isoUtcMs} == 2011-05-16T19:47:55.54Z
${1305575275540?double?numberToDatetime?isoUtcMs} == 2011-05-16T19:47:55.54Z
${bigInteger?numberToDatetime?isoUtcMs} == 2011-05-16T19:47:55.54Z
${bigDecimal?numberToDatetime?isoUtcMs} == 2011-05-16T19:47:55.54Z
${1000?float?numberToDatetime?isoUtc} == 1970-01-01T00:00:01Z
${1000?int?numberToDatetime?isoUtc} == 1970-01-01T00:00:01Z
${0?byte?numberToDatetime?isoUtc} == 1970-01-01T00:00:00Z

<#attempt>
${9999991305575275540?numberToDatetime?isoUtc} <#-- doesn't fit into long -->
<#recover>
failed
</#attempt>