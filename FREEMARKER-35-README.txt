This is the developlment branch for Jira issue FREEMARKER-35, java.time support.

The most likely working way of contribing is just finding bugs in what we have, and provide tests for
them! There can be a lot of edge cases (or use cases) that we missed, as date/time is a complicated topic.

For those who are willing to invest a lot of enregy, and are experienced developers, see the TODO list!
But please discuss it on the dev list before invest your time into anything there.


CHANGE LOG
----------

I.e., what already works:

- New template model type: TemplateTemporalModel, to wrap all the java.time.temporal.Temporal subtypes
  (hence not Duration, and Period).
- DefaultObjectWrapper (and BeansWrapper): Added temporalSupport. By default true from IcI 2.3.33.
- The date_format, time_format, and datetime_format settings "just works" for TemplateTemporalModel.
  We mimic the behavior of the traditional pre-java.time formatters for Temporal-s; both formatting, and parsing.
- New settings: year_format, year_month_format, custom_temporal_formats
- Improved global caching for TemplateNumberFormat-s, TemplateDateFormat-s as well.

TODO
----

These are notes about what's left from this FREEMARKER-35. Some entries has possibly become obsolete over time,
and we just didn't get to them again since then to realize that. Also, some entries are just ideas, or rough
plans, so this is not a strict spefication.

- Add BI-s:
  - ?instant, ?localDate, ?localTime, ?localDateTime, ?offsetTime, ?offsetDateTime, ?zonedDateTime.
    - Converts both from j.u.Date, and from String. 
    - Use MissingTimeZoneParserPolicy.ASSUME_CURRENT_TIME_ZONE, so that format->parse->format gives initial value back.
    - ?offsetTime should fail if it had to convert to the current time zone.
  - ?unambiguousOffsetDateTime, ?unambiguousZonedDateTime: Use MissingTimeZoneParserPolicy.FAIL.
    Note: We don't need ?unambiguousOffsetTime, as ?offsetTime is already like that.
  - ?offsetOrLocalDateTime, ?zonedOrLocalDateTime, ?offsetOrLocalTime: Use MissingTimeZoneParserPolicy.FALL_BACK_TO_LOCAL_TEMPORAL
- Existing date/time BI-s, must work for j.t.Temporals as well:
  - Casting:
    - ?datetime, ?date, ?time should work if LHO is TemplateTemporalModel, and convert to the appropriate Temporal.
      This is consistent with the new ?isDatetime, etc.
      But, there's the assymetry that when converting from String, these will always create TDM instead of TTM.
    - ${someDateTimeTemporal?datetime} worked before if the datetimeFormat was "iso", because someDateTimeTemporal was coerced to plain text via Object.toString(), and then parsed by ?datetime.
      But for a TTM we should work with the Temporal value instead.
  - ?is${Type}: ?isDatetime etc. should be true for TTM with appropriate Temporal type. (Beware with is_date VS is_date_only.)
    - Add isLocalDateTime, etc.
    - Add ?isJava{Util,Time}Based{Date,Datetime,Time,Temporal}
  - Anything else? Won't be BC to do after release if we miss something!
- isTimeZoneBound(): Can it return false for local temporals?
- Tests for parsing epoch millies
- ISO/XS parsing:
  - Add missing parsing tests
  - XS -1 vs ISO year 0? Though it should only matter with Instance, and there we already have the Juilan issue.
  - ISO-like format parameters (like "iso u"). Note that "u" should influences what GUESS_TIME_ZONE_IF_MISSING does.
    - nz: non-locals must be converted to the current time zone
    - fz, fu: Assume the locale is in the current or UTC time zone. Document warning for where!
  - Second can be 60 at leap seconds
- Do DateTimeFormatter.ofLocalizedXxx also need DateTimeFormatter tweaks (decimal symbols, etc.)?
- Check if we have test for "" pattern; must be same as "medium"
- Implement extended syntax (always enabled):
  - Start with "v<1|2>:" Because "v" is illegal in standard patterns, it's BC.
  - "...;for<Type>=<pattern>": Because "f" is illegal in standard patterns, it's BC.
  - Error if: ";" WS* Identifier, and Identifier contains any letter not defined by the standard formatters as of today.
  - v2 should ban Y, unless ";enableWeekYear" is present
- Custom formats for Date-s and Temporal-s are now overlapping on the format setting level; test behavior
- ?c should show toString format. We also need to do that with traditional dates.
- Add AliasTemporalDateFormatFactory, similar to AliasTemplateDateFormatFactory
- Test: Pure BeansWrapper TemporalModel
- Test: Method calls, and overloaded method calls
- Should ?api be suppoted by default Temporal wrapping? Then, maybe we need a DefaultTemporalAdapter instead of SimpleTemporal.
- Comparisions (and ?sort)
- Add temporal?with_time_zone(string). Refer to it in error messgages where the zone filed is missed.
- Support this on date and temporal format settings: "<somepattern>; time_zone=<zoneID>|.time_zone"
  If we have no time for it, still have to reserve ; for this purpose!
  Note: Needs ICI for old date formats. Needs to support ';' and 'Joe''s house'. Will likely add others formatter paramters later, like "locale".
- Check these:
  - Parsing:
    - Use smart mode (not lenient mode)!
    - Don't forget to set parsing to case insensitive.
  - xs and iso parameters must start with space or '_'
  - No TODO [FREEMARKER-35] was left
  - DateTimeFormatter from v2 pattern uses:
              builder
                  .parseCaseInsensitive() // Must be before pattern(s) appended!
                  .appendPattern(pattern)
                  .toFormat(locale)
                  .withDecimalStyle(DecimalStyle.of(locale))
                  .withChronology(Chronology.ofLocale(locale));
  - Why is the "à" before the time part missing for string.full?
    <#-- Java 8 JDK-8085887 workaround test (it was, but we are running on Java 16): -->
    <@assertEquals expected="5 avril 2003 à 06:07:08" actual=localDateTime?string.long />
    <@assertEquals expected="samedi 5 avril 2003 06:07:08" actual=localDateTime?string.full />
- _TemporalUtils: Setting related utils should be moved to the published class where we also already expose published API to return the valid setting names.
- Manual (i.e., documentation):
  - Update based on the "Change log" above
  - Add table of Java 8 format style examples
