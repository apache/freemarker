/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.utility.StringUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * Static utilities related to {@link Temporal}-s, and other {@code java.time} classes.
 *
 * @since 2.3.33
 */
public final class _TemporalUtils {
    private static final Map<Class<? extends Temporal>, TemporalQuery<? extends Temporal>> TEMPORAL_CLASS_TO_QUERY_MAP;
    static {
        TEMPORAL_CLASS_TO_QUERY_MAP = new IdentityHashMap<>();
        TEMPORAL_CLASS_TO_QUERY_MAP.put(Instant.class, Instant::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalDate.class, LocalDate::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalDateTime.class, LocalDateTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(LocalTime.class, LocalTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(OffsetDateTime.class, _TemporalUtils::offsetDateTimeFrom);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(OffsetTime.class, OffsetTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(ZonedDateTime.class, ZonedDateTime::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(Year.class, Year::from);
        TEMPORAL_CLASS_TO_QUERY_MAP.put(YearMonth.class, YearMonth::from);
    }

    /**
     * {@link Temporal} subclasses directly suppoerted by FreeMarker.
     */
    // Not private because of tests
    static final List<Class<? extends Temporal>> SUPPORTED_TEMPORAL_CLASSES = Arrays.asList(
            Instant.class,
            LocalDate.class,
            LocalDateTime.class,
            LocalTime.class,
            OffsetDateTime.class,
            OffsetTime.class,
            ZonedDateTime.class,
            Year.class,
            YearMonth.class);

    // Not private because of tests
    static final boolean SUPPORTED_TEMPORAL_CLASSES_ARE_FINAL = SUPPORTED_TEMPORAL_CLASSES.stream()
            .allMatch(cl -> (cl.getModifiers() & Modifier.FINAL) != 0);

    private _TemporalUtils() {
        throw new AssertionError();
    }

    /**
     * Creates a temporal query that can be used to create an object of the specified temporal class from a typical
     * parsing result.
     */
    public static TemporalQuery<? extends Temporal> getTemporalQuery(Class<? extends Temporal> temporalClass) {
        TemporalQuery<? extends Temporal> temporalQuery = TEMPORAL_CLASS_TO_QUERY_MAP.get(temporalClass);
        if (temporalQuery == null) {
            Class<? extends Temporal> normalizedTemporalClass = normalizeSupportedTemporalClass(temporalClass);
            if (temporalClass != normalizedTemporalClass) {
                temporalQuery = TEMPORAL_CLASS_TO_QUERY_MAP.get(normalizedTemporalClass);
            }
        }
        if (temporalQuery == null) {
            throw new IllegalArgumentException("Unsupported temporal class: " + temporalClass.getName());
        }
        return temporalQuery;
    }

    private static OffsetDateTime offsetDateTimeFrom(TemporalAccessor temporal) {
        return ZonedDateTime.from(temporal).toOffsetDateTime();
    }

    /**
     * Creates a {@link DateTimeFormatter} from a pattern that uses the syntax that's used by the
     * {@link SimpleDateFormat} constructor.
     *
     * @param pattern The pattern with {@link SimpleDateFormat} syntax.
     * @param locale The locale of the output of the formatter
     *
     * @return
     *
     * @throws IllegalArgumentException If the pattern is not a valid {@link SimpleDateFormat} pattern (based on the
     * syntax documented for Java 15).
     */
    public static DateTimeFormatter dateTimeFormatterFromSimpleDateFormatPattern(String pattern, Locale locale) {
        return createDateTimeFormatterBuilderFromSimpleDateFormatPattern(pattern, locale)
                .toFormatter(locale)
                .withDecimalStyle(DecimalStyle.of(locale))
                .withChronology(getChronologyForLocaleWithLegacyRules(locale));
    }

    private static DateTimeFormatterBuilder createDateTimeFormatterBuilderFromSimpleDateFormatPattern(
            String pattern, Locale locale) {
        DateTimeFormatterBuilder builder = tryCreateDateTimeFormatterBuilderFromSimpleDateFormatPattern(pattern, locale, false);
        if (builder == null) {
            builder = tryCreateDateTimeFormatterBuilderFromSimpleDateFormatPattern(pattern, locale, true);
        }
        return builder;
    }

    /**
     * @param standaloneFormGuess Guess if we only will have one field.
     * @return If {@code null}, then {@code standaloneFormGuess} was wrong, and it also mattered, so retry with the
     *         inverse of it.
     */
    private static DateTimeFormatterBuilder tryCreateDateTimeFormatterBuilderFromSimpleDateFormatPattern(
            String pattern, Locale locale, boolean standaloneFormGuess) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();

        builder.parseCaseInsensitive(); // Must be before pattern(s) appended!

        int numberOfFields = 0;
        int len = pattern.length();
        int pos = 0;
        int lastClosingQuotePos = Integer.MIN_VALUE;
        boolean standaloneFormGuessWasUsed = false;
        do {
            char c = pos < len ? pattern.charAt(pos++) : 0;
            if (StringUtil.isUsAsciiLetter(c)) {
                int startPos = pos - 1;
                while (pos < len && pattern.charAt(pos) == c) {
                    pos++;
                }
                standaloneFormGuessWasUsed |= applyRepeatedLetter(
                        c, pos - startPos, locale, pattern, standaloneFormGuess, builder);
                numberOfFields++;
            } else if (c == '\'') {
                int literalStartPos = pos;
                if (lastClosingQuotePos == literalStartPos - 2) {
                    builder.appendLiteral('\'');
                }
                while (pos < len && pattern.charAt(pos) != '\'') {
                    pos++;
                }
                if (literalStartPos == pos) {
                    builder.appendLiteral('\'');
                    // Doesn't set lastClosingQuotePos
                } else {
                    builder.appendLiteral(pattern.substring(literalStartPos, pos));
                    lastClosingQuotePos = pos;
                }
                pos++; // Because char at pos was already processed
            } else {
                int literalStartPos = pos - 1;
                while (pos < len && !isUsAsciiLetterOrApostrophe(pattern.charAt(pos))) {
                    pos++;
                }
                builder.appendLiteral(pattern.substring(literalStartPos, pos));
                // No pos++, because the char at pos is not yet processed
            }
        } while (pos < len);
        if (standaloneFormGuessWasUsed && standaloneFormGuess != (numberOfFields == 1)) {
            return null;
        }
        return builder;
    }

    private static boolean applyRepeatedLetter(
            char c, int width, Locale locale, String pattern,
            boolean standaloneField,
            DateTimeFormatterBuilder builder) {
        boolean standaloneFieldArgWasUsed = false;
        switch (c) {
            case 'y':
                appendYearLike(width, ChronoField.YEAR_OF_ERA, builder);
                break;
            case 'Y':
                appendYearLike(width, WeekFields.of(locale).weekBasedYear(), builder);
                break;
            case 'M':
            case 'L':
                if (width <= 2) {
                    appendValueWithSafeWidth(ChronoField.MONTH_OF_YEAR, width, 2, builder);
                } else if (width == 3) {
                    TextStyle textStyle;
                    if (c == 'M') {
                        standaloneFieldArgWasUsed = true;
                        textStyle = standaloneField ? TextStyle.SHORT_STANDALONE : TextStyle.SHORT;
                    } else {
                        textStyle = TextStyle.SHORT_STANDALONE;
                    }

                    if (textStyle == TextStyle.SHORT_STANDALONE
                            && !_JavaTimeBugUtils.hasGoodShortStandaloneMonth(locale)) {
                        textStyle = TextStyle.SHORT;
                    }

                    builder.appendText(ChronoField.MONTH_OF_YEAR, textStyle);
                } else {
                    TextStyle textStyle;
                    if (c == 'M') {
                        standaloneFieldArgWasUsed = true;
                        textStyle = standaloneField ? TextStyle.FULL_STANDALONE : TextStyle.FULL;
                    } else {
                        textStyle = TextStyle.FULL_STANDALONE;
                    }

                    if (textStyle == TextStyle.FULL_STANDALONE
                            && !_JavaTimeBugUtils.hasGoodFullStandaloneMonth(locale)) {
                        textStyle = TextStyle.FULL;
                    }

                    builder.appendText(ChronoField.MONTH_OF_YEAR, textStyle);
                }
                break;
            case 'd':
                appendValueWithSafeWidth(ChronoField.DAY_OF_MONTH, width, 2, builder);
                break;
            case 'D':
                if (width == 1) {
                    builder.appendValue(ChronoField.DAY_OF_YEAR);
                } else if (width == 2) {
                    // 2 wide if possible, but don't lose a digit over 99. SimpleDateFormat does this too.
                    builder.appendValue(ChronoField.DAY_OF_YEAR, 2, 3, SignStyle.NOT_NEGATIVE);
                } else {
                    // Here width is at least 3, so we are safe.
                    builder.appendValue(ChronoField.DAY_OF_YEAR, width);
                }
                break;
            case 'h':
                appendValueWithSafeWidth(ChronoField.CLOCK_HOUR_OF_AMPM, width, 2, builder);
                break;
            case 'H':
                appendValueWithSafeWidth(ChronoField.HOUR_OF_DAY, width, 2, builder);
                break;
            case 'k':
                appendValueWithSafeWidth(ChronoField.CLOCK_HOUR_OF_DAY, width, 2, builder);
                break;
            case 'K':
                appendValueWithSafeWidth(ChronoField.HOUR_OF_AMPM, width, 2, builder);
                break;
            case 'a':
                // From experimentation with SimpleDataFormat it seemed that the number of repetitions doesn't matter.
                builder.appendText(ChronoField.AMPM_OF_DAY, TextStyle.SHORT);
                break;
            case 'm':
                appendValueWithSafeWidth(ChronoField.MINUTE_OF_HOUR, width, 2, builder);
                break;
            case 's':
                appendValueWithSafeWidth(ChronoField.SECOND_OF_MINUTE, width, 2, builder);
                break;
            case 'S':
                // This is quite dangerous, like "s.SS" gives misleading output, but SimpleDateFormat does this.
                appendValueWithSafeWidth(ChronoField.MILLI_OF_SECOND, width, 3, builder);
                break;
            case 'u':
                builder.appendValue(ChronoField.DAY_OF_WEEK, width);
                break;
            case 'w':
                appendValueWithSafeWidth(WeekFields.of(locale).weekOfWeekBasedYear(), width, 2, builder);
                break;
            case 'W':
                appendValueWithSafeWidth(WeekFields.of(locale).weekOfMonth(), width, 1, builder);
                break;
            case 'E':
                if (width <= 3 ) {
                    builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
                } else {
                    builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL);
                }
                break;
            case 'G':
                // Width apparently doesn't matter for SimpleDateFormat, and we mimic that. (It's not always a perfect
                // match though, like japanese calendar era "Reiwa" VS "R".)
                builder.appendText(ChronoField.ERA, TextStyle.SHORT);
                break;
            case 'F':
                // While SimpleDateFormat documentation says it's "day of week in month", the actual output is "aligned
                // week of month" (a bug, I assume). With DateTimeFormatter "F" is "aligned day of week in month", but
                // our goal here is to mimic the behaviour of SimpleDateFormat.
                appendValueWithSafeWidth(ChronoField.ALIGNED_WEEK_OF_MONTH, width, 1, builder);
                break;
            case 'z':
                if (width < 4) {
                    builder.appendZoneText(TextStyle.SHORT);
                } else {
                    builder.appendZoneText(TextStyle.FULL);
                }
                break;
            case 'Z':
                // Width apparently doesn't matter for SimpleDateFormat, and we mimic that.
                builder.appendOffset("+HHMM","+0000");
                break;
            case 'X':
                if (width == 1) {
                    // We lose the minutes here, just like SimpleDateFormat did.
                    builder.appendOffset("+HH", "Z");
                } else if (width == 2) {
                    builder.appendOffset("+HHMM", "Z");
                } else if (width == 3) {
                    builder.appendOffset("+HH:MM", "Z");
                } else {
                    throw new IllegalArgumentException("Can't create DateTimeFormatter from SimpleDateFormat pattern "
                            + StringUtil.jQuote(pattern) + ": "
                            + " \"X\" width in SimpleDateFormat patterns must be less than 4.");
                }
                break;
            default:
                throw new IllegalArgumentException("Can't create DateTimeFormatter from SimpleDateFormat pattern "
                        + StringUtil.jQuote(pattern) + ": "
                        + StringUtil.jQuote(c) + " is an invalid or unsupported SimpleDateFormat pattern letter.");
        }
        return standaloneFieldArgWasUsed;
    }

    private static void appendYearLike(int width, TemporalField field, DateTimeFormatterBuilder builder) {
        if (width != 2) {
            builder.appendValue(field, width, 19, SignStyle.NORMAL);
        } else {
            builder.appendValueReduced(field, 2, 2, 2000);
        }
    }

    private static String repeatChar(char c, int count) {
        char[] chars = new char[count];
        for (int i = 0; i < count; i++) {
            chars[i] = c;
        }
        return new String(chars);
    }

    /**
     * Used for non-negative numerical fields, behaves like {@link SimpleDateFormat} regarding the field width.
     *
     * @param width The width specified in the pattern
     * @param safeWidth The minimum width needed to safely display any valid value
     */
    private static void appendValueWithSafeWidth(
            TemporalField field, int width, int safeWidth, DateTimeFormatterBuilder builder) {
        builder.appendValue(field, width, width < safeWidth ? safeWidth : width, SignStyle.NOT_NEGATIVE);
    }

    private static boolean isUsAsciiLetterOrApostrophe(char c) {
        return StringUtil.isUsAsciiLetter(c) || c == '\'';
    }

    /**
     * Gives the {@link Chronology} for a {@link Locale} that {@link Calendar#getInstance(Locale)} would; except, that
     * returned a {@link Calendar} instead of a {@link Chronology}, so this is somewhat complicated to do.
     */
    private static Chronology getChronologyForLocaleWithLegacyRules(Locale locale) {
        // Usually null
        String askedCalendarType = locale.getUnicodeLocaleType("ca");

        Calendar calendar = Calendar.getInstance(locale);

        Locale chronologyLocale;
        String legacyLocalizedCalendarType = calendar.getCalendarType();
        // The pre-java.time API gives different localized defaults sometimes, or at least for th_TH. To be on the safe
        // side, for the two non-gregory types that pre-java.time Java supported out-of-the-box, we force the calendar
        // type in the Locale, for which later we will ask the Chronology.
        if (("buddhist".equals(legacyLocalizedCalendarType) || "japanese".equals(legacyLocalizedCalendarType))
                && !legacyLocalizedCalendarType.equals(askedCalendarType)) {
            chronologyLocale = createLocaleWithCalendarType(
                    locale,
                    legacyCalendarTypeToJavaTimeApiCompatibleName(legacyLocalizedCalendarType));
        } else {
            // Even if there's no difference in the default chronology of the locale, the calendar type names that
            // worked with the legacy API might not be recognized by the java.time API.
            String compatibleAskedCalendarType = legacyCalendarTypeToJavaTimeApiCompatibleName(askedCalendarType);
            if (askedCalendarType != compatibleAskedCalendarType) { // deliberately doesn't use equals(...)
                chronologyLocale = createLocaleWithCalendarType(locale, compatibleAskedCalendarType);
            } else {
                chronologyLocale = locale;
            }
        }
        Chronology chronology = Chronology.ofLocale(chronologyLocale);
        return chronology;
    }

    private static String legacyCalendarTypeToJavaTimeApiCompatibleName(String legacyType) {
        // "gregory" is the Calendar.calendarType in the old API, but Chronology.ofLocale calls it "ISO".
        return "gregory".equals(legacyType) ? "ISO" : legacyType;
    }

    private static Locale createLocaleWithCalendarType(Locale locale, String legacyApiCalendarType) {
        return new Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("ca", legacyApiCalendarType)
                .build();
    }

    /**
     * Ensures that {@code ==} can be used to check if the class is assignable to one of the {@link Temporal} subclasses
     * that FreeMarker directly supports. At least in Java 8 they are all final anyway, but just in case this changes in
     * a future Java version, use this method before using {@code ==}.
     *
     * @throws IllegalArgumentException If the temporal class is not currently supported by FreeMarker.
     */
    public static Class<? extends Temporal> normalizeSupportedTemporalClass(Class<? extends Temporal> temporalClass) {
        if (SUPPORTED_TEMPORAL_CLASSES_ARE_FINAL) {
            return temporalClass;
        } else {
            if (Instant.class.isAssignableFrom(temporalClass)) {
                return Instant.class;
            } else if (LocalDate.class.isAssignableFrom(temporalClass)) {
                return LocalDate.class;
            } else if (LocalDateTime.class.isAssignableFrom(temporalClass)) {
                return LocalDateTime.class;
            } else if (LocalTime.class.isAssignableFrom(temporalClass)) {
                return LocalTime.class;
            } else if (OffsetDateTime.class.isAssignableFrom(temporalClass)) {
                return OffsetDateTime.class;
            } else if (OffsetTime.class.isAssignableFrom(temporalClass)) {
                return OffsetTime.class;
            } else if (ZonedDateTime.class.isAssignableFrom(temporalClass)) {
                return ZonedDateTime.class;
            } else if (YearMonth.class.isAssignableFrom(temporalClass)) {
                return YearMonth.class;
            } else if (Year.class.isAssignableFrom(temporalClass)) {
                return Year.class;
            } else {
                throw new IllegalArgumentException("Unsupported temporal class: " + temporalClass.getName());
            }
        }
    }

    /**
     * Tells if the temporal class is one that doesn't store, nor have an implied time zone, or offset.
     *
     * @throws IllegalArgumentException If the temporal class is not currently supported by FreeMarker.
     */
    public static boolean isLocalTemporalClass(Class<? extends Temporal> temporalClass) {
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        if (temporalClass == Instant.class
                || temporalClass == OffsetDateTime.class
                || temporalClass == ZonedDateTime.class
                || temporalClass == OffsetTime.class) {
            return false;
        }
        return true;
    }

    /**
     * Returns the local variation of a non-local class, or {@code null} if no local pair is known, or the class is not
     * recognized .
     *
     * @throws IllegalArgumentException If the temporal class is not currently supported by FreeMarker.
     */
    public static Class<? extends Temporal> tryGetLocalTemporalClassForNonLocal(Class<? extends Temporal> temporalClass) {
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        if (temporalClass == OffsetDateTime.class) {
            return LocalDateTime.class;
        }
        if (temporalClass == ZonedDateTime.class) {
            return LocalDateTime.class;
        }
        if (temporalClass == OffsetTime.class) {
            return LocalTime.class;
        }
        if (temporalClass == Instant.class) {
            return LocalDateTime.class;
        }
        return null;
    }

    /**
     * Returns the FreeMarker configuration format setting name for a temporal class.
     *
     * @throws IllegalArgumentException If {@code temporalClass} is not a supported {@link Temporal} subclass.
     */
    public static String temporalClassToFormatSettingName(Class<? extends Temporal> temporalClass, boolean camelCase) {
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        if (temporalClass == Instant.class
                || temporalClass == LocalDateTime.class
                || temporalClass == ZonedDateTime.class
                || temporalClass == OffsetDateTime.class) {
            return camelCase
                    ? Configuration.DATETIME_FORMAT_KEY_CAMEL_CASE
                    : Configuration.DATETIME_FORMAT_KEY_SNAKE_CASE;
        } else if (temporalClass == LocalDate.class) {
            return camelCase
                    ? Configuration.DATE_FORMAT_KEY_CAMEL_CASE
                    : Configuration.DATE_FORMAT_KEY_SNAKE_CASE;
        } else if (temporalClass == LocalTime.class || temporalClass == OffsetTime.class) {
            return camelCase
                    ? Configuration.TIME_FORMAT_KEY_CAMEL_CASE
                    : Configuration.TIME_FORMAT_KEY_SNAKE_CASE;
        } else if (temporalClass == YearMonth.class) {
            return camelCase
                    ? Configuration.YEAR_MONTH_FORMAT_KEY_CAMEL_CASE
                    : Configuration.YEAR_MONTH_FORMAT_KEY_SNAKE_CASE;
        } else if (temporalClass == Year.class) {
            return camelCase
                    ? Configuration.YEAR_FORMAT_KEY_CAMEL_CASE
                    : Configuration.YEAR_FORMAT_KEY_SNAKE_CASE;
        } else {
            throw new IllegalArgumentException("Unsupported temporal class: " + temporalClass.getName());
        }
    }

}
