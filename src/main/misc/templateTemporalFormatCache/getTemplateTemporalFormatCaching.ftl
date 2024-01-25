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
// BEGIN Generated with getTemplateTemporalFormatCaching.ftl
<#-- Classes are in order of frequency (guessed). -->
<#list ['LocalDateTime', 'Instant', 'LocalDate', 'LocalTime', 'ZonedDateTime', 'OffsetDateTime', 'OffsetTime', 'YearMonth', 'Year'] as TemporalClass>
  <#assign temporalClass = TemporalClass[0]?lowerCase + TemporalClass[1..]>
  if (temporalClass == ${TemporalClass}.class) {
      result = cachedTemporalFormats.${temporalClass}Format;
      if (result != null) {
          return result;
      }

      result = cachedTemporalFormats.reusable${TemporalClass}Format;
      if (result != null
              && result.canBeUsedForTimeZone(getTimeZone()) && result.canBeUsedForLocale(getLocale())) {
          cachedTemporalFormats.${temporalClass}Format = result;
          return result;
      }

      result = getTemplateTemporalFormat(getTemporalFormat(temporalClass), temporalClass);
      cachedTemporalFormats.${temporalClass}Format = result;
      // We do this ahead of time, to decrease the cost of evictions:
      cachedTemporalFormats.reusable${TemporalClass}Format = result;
      return result;
  }
</#list>
// END Generated with getTemplateTemporalFormatCaching.ftl
