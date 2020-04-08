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
package freemarker.template.utility;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class TemporalUtil {
	private static final Pattern FORMAT_STYLE_PATTERN = Pattern.compile("^(short|medium|long|full)(_(short|medium|long|full))?$");
	private final static DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
	private final static DateTimeFormatter MEDIUM_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
	private final static DateTimeFormatter LONG_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
	private final static DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);

	private final static DateTimeFormatter XSD_FORMAT = new DateTimeFormatterBuilder()
			.append(DateTimeFormatter.ISO_LOCAL_DATE)
			.optionalStart()
			.appendLiteral('T')
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
			.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
			.optionalEnd()
				.optionalStart()
			.appendOffsetId()
			.optionalEnd()
			.toFormatter();
	private final static DateTimeFormatter XSD_TIME_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
			.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
			.optionalStart()
				.appendOffsetId()
			.optionalEnd()
			.toFormatter();
	public static final DateTimeFormatter XSD_YEARMONTH_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.optionalStart()
				.appendOffsetId()
			.optionalEnd()
			.toFormatter();

	public static final DateTimeFormatter ISO8601_FORMAT = new DateTimeFormatterBuilder()
			.append(DateTimeFormatter.ISO_LOCAL_DATE)
			.optionalStart()
				.appendLiteral('T')
				.appendValue(ChronoField.HOUR_OF_DAY, 2)
				.appendLiteral(":")
				.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
				.appendLiteral(":")
				.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
				.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
				.optionalStart()
					.appendOffsetId()
				.optionalEnd()
			.optionalEnd()
			.toFormatter();
	public static final DateTimeFormatter ISO8601_TIME_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.appendLiteral(":")
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
			.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
			.toFormatter();
	public static final DateTimeFormatter ISO8601_YEARMONTH_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.toFormatter();
	public static final DateTimeFormatter ISO8601_YEAR_FORMAT = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR)
			.toFormatter();

	private static DateTimeFormatter getISO8601Formatter(Temporal temporal) {
		if (temporal instanceof LocalTime)
			return ISO8601_TIME_FORMAT;
		else if (temporal instanceof Year)
			return ISO8601_YEAR_FORMAT;
		else if (temporal instanceof YearMonth)
			return ISO8601_YEARMONTH_FORMAT;
		else
			return ISO8601_FORMAT;
	}

	private static DateTimeFormatter getXSFormatter(Temporal temporal) {
		if (temporal instanceof LocalTime)
			return XSD_TIME_FORMAT;
		else if (temporal instanceof Year)
			return ISO8601_YEAR_FORMAT;//ISO same as XSD here
		else if (temporal instanceof YearMonth)
			return XSD_YEARMONTH_FORMAT;
		else
			return XSD_FORMAT;
	}

	public static String format(Temporal temporal, String format, Locale locale, TimeZone timeZone) {
		//TODO: cache these DateTimeFormatter instances (withLocale & withZone create new instances too, when they differ from the instance)
		if (temporal instanceof Instant)
			temporal = ((Instant) temporal).atZone(timeZone == null ? ZoneOffset.UTC : timeZone.toZoneId());

		String[] formatSplt = format.split("_");
		DateTimeFormatter dtf;
		if ("xs".equals(format))
			dtf = getXSFormatter(temporal);
		else if ("iso".equals(format))
			dtf =  getISO8601Formatter(temporal);
		else if (FORMAT_STYLE_PATTERN.matcher(format).matches()) {
			boolean isYear = temporal instanceof Year;
			boolean isYearMonth = temporal instanceof YearMonth;
			if (isYear || isYearMonth) {
				String reducedPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.valueOf(formatSplt[0].toUpperCase()), null, IsoChronology.INSTANCE, locale);
				if (isYear)
					dtf = DateTimeFormatter.ofPattern(removeNonYM(reducedPattern, false));
				else
					dtf = DateTimeFormatter.ofPattern(removeNonYM(reducedPattern, true));
			} else if ("short".equals(format))
				dtf =  SHORT_FORMAT;
			else if ("medium".equals(format))
				dtf =  MEDIUM_FORMAT;
			else if ("long".equals(format))
				dtf =  LONG_FORMAT;
			else if ("full".equals(format))
				dtf = FULL_FORMAT;
			else
				dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.valueOf(formatSplt[0].toUpperCase()), FormatStyle.valueOf(formatSplt[1].toUpperCase()));
		} else if (!"".equals(format))
			dtf = DateTimeFormatter.ofPattern(format);
		else
			return temporal.toString();

		dtf = dtf.withLocale(locale);
		if (temporal instanceof OffsetDateTime)
			dtf = dtf.withZone(((OffsetDateTime) temporal).getOffset());
		else if (!(temporal instanceof ZonedDateTime))
			dtf = dtf.withZone(timeZone.toZoneId());
		return dtf.format(temporal);
	}

	private static String removeNonYM(String pattern, boolean withMonth) {
		boolean separator = false;
		boolean copy = true;
		StringBuilder newPattern = new StringBuilder();
		for (char c : pattern.toCharArray()) {
			if (c == '\'')
				separator = !separator;
			if (!separator && Character.isAlphabetic(c))
				copy = c == 'y' || c == 'u' || (withMonth && (c == 'M' || c == 'L'));
			if (copy)
				newPattern.append(c);
		}
		return newPattern.toString();
	}
}
