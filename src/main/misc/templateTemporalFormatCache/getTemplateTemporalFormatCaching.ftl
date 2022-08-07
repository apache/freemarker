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
