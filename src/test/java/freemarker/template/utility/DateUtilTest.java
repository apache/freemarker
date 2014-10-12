/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template.utility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.TestCase;
import freemarker.template.utility.DateUtil.CalendarFieldsToDateConverter;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.DateUtil.TrivialCalendarFieldsToDateConverter;

public class DateUtilTest extends TestCase {
    
    private final TimeZone originalDefaultTZ = TimeZone.getDefault();

    @Override
    protected void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Prague"));
    }

    @Override
    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalDefaultTZ);
    }

    private final DateFormat df
            = new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss:S Z", Locale.US);
    {
        df.setTimeZone(DateUtil.UTC);
    }
    
    private CalendarFieldsToDateConverter cf2dc = new TrivialCalendarFieldsToDateConverter();
    
    private DateToISO8601CalendarFactory calendarFactory
            = new DateUtil.TrivialDateToISO8601CalendarFactory();
    
    public DateUtilTest(String name) {
        super(name);
    }
    
    public void testDateToUTCString() throws ParseException {
        assertEquals(
                "1998-10-30T15:30:00.512Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:512 +0400"), true));
        assertEquals(
                "1998-10-30T15:30:00.5Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:500 +0400"), true));
        assertEquals(
                "1998-10-30T15:30:00.51Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:510 +0400"), true));
        assertEquals(
                "1998-10-30T15:30:00.1Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:100 +0400"), true));
        assertEquals(
                "1998-10-30T15:30:00.01Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:10 +0400"), true));
        assertEquals(
                "1998-10-30T15:30:00.001Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 1998-10-30 19:30:00:1 +0400"), true));
        assertEquals(
                "2000-02-08T06:05:04Z",
                dateToISO8601UTCDateTimeMSString(
                        df.parse("AD 2000-02-08 09:05:04:0 +0300"), true));
        assertEquals(
                "0099-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse(
                        "AD 0099-03-02 09:15:24:0 +0300"), true));
        assertEquals(
                "0010-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("AD 0010-03-02 09:15:24:0 +0300"), true));
        assertEquals(
                "0001-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("AD 0001-03-02 09:15:24:0 +0300"), true));
        assertEquals(
                "0000-02-29T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("BC 0001-03-02 09:15:24:0 +0300"), true));
        assertEquals(
                "-1-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("BC 2-03-02 09:15:24:0 +0300"), true));
        assertEquals(
                "10000-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("AD 10000-02-28 09:15:24:0 +0300"), true));

        Date d = df.parse("AD 1998-10-30 19:30:00:512 +0400");
        assertEquals(
                "1998-10-30",
                dateToISO8601UTCDateString(d));
        assertEquals(
                "15:30:00.512Z",
                dateToISO8601UTCTimeMSString(d, true));
        assertEquals(
                "15:30:00.512",
                dateToISO8601UTCTimeMSString(d, false));
        assertEquals(
                "1998-10-30",
                dateToISO8601UTCDateString(
                        new java.sql.Date(d.getTime())));
        assertEquals(
                "15:30:00.512Z",
                dateToISO8601UTCTimeMSString(
                        new java.sql.Time(d.getTime()), true));
    }

    public void testLocalTime() throws ParseException {
        Date dsum = df.parse("AD 2010-05-09 20:00:00:0 UTC");
        Date dwin = df.parse("AD 2010-01-01 20:00:00:0 UTC");
        
        TimeZone tzRome = TimeZone.getTimeZone("Europe/Rome");
        if (tzRome.getOffset(0) == 0) {
            throw new RuntimeException(
                    "Can't get time zone for Europe/Rome!");
        }
        assertEquals(
                "2010-05-09T22:00:00+02:00",
                dateToISO8601DateTimeString(dsum, tzRome));
        assertEquals(
                "2010-01-01T21:00:00+01:00",
                dateToISO8601DateTimeString(dwin, tzRome));
        assertEquals(
                "2010-05-09",
                dateToISO8601DateString(dsum, tzRome));
        assertEquals(
                "2010-01-01",
                dateToISO8601DateString(dwin, tzRome));
        assertEquals(
                "22:00:00+02:00",
                dateToISO8601TimeString(dsum, tzRome));
        assertEquals(
                "21:00:00+01:00",
                dateToISO8601TimeString(dwin, tzRome));
        
        TimeZone tzNY = TimeZone.getTimeZone("America/New_York");
        if (tzNY.getOffset(0) == 0) {
            throw new RuntimeException(
                    "Can't get time zone for America/New_York!");
        }
        assertEquals(
                "2010-05-09T16:00:00-04:00",
                dateToISO8601DateTimeString(dsum, tzNY));
        assertEquals(
                "2010-01-01T15:00:00-05:00",
                dateToISO8601DateTimeString(dwin, tzNY));
        assertEquals(
                "2010-05-09",
                dateToISO8601DateString(dsum, tzNY));
        assertEquals(
                "2010-01-01",
                dateToISO8601DateString(dwin, tzNY));
        assertEquals(
                "16:00:00-04:00",
                dateToISO8601TimeString(dsum, tzNY));
        assertEquals(
                "15:00:00-05:00",
                dateToISO8601TimeString(dwin, tzNY));
        
        TimeZone tzFixed = TimeZone.getTimeZone("GMT+02:30");
        assertEquals(
                "2010-05-09T22:30:00+02:30",
                dateToISO8601DateTimeString(dsum, tzFixed));
        assertEquals(
                "2010-01-01T22:30:00+02:30",
                dateToISO8601DateTimeString(dwin, tzFixed));
    }

    public void testGetTimeZone() throws Exception {
        assertTrue(DateUtil.getTimeZone("GMT") != DateUtil.UTC);
        assertTrue(DateUtil.getTimeZone("UT1") != DateUtil.UTC);
        assertEquals(DateUtil.getTimeZone("UTC"), DateUtil.UTC);
        
        assertEquals(DateUtil.getTimeZone("Europe/Rome"),
                TimeZone.getTimeZone("Europe/Rome"));
        
        assertEquals(DateUtil.getTimeZone("Iceland"), // GMT and no DST
                TimeZone.getTimeZone("Iceland"));
        
        try {
            DateUtil.getTimeZone("Europe/NoSuch");
            fail();
        } catch (UnrecognizedTimeZoneException e) {
            // good
        }
    }
    
    public void testTimeOnlyDate() throws UnrecognizedTimeZoneException {
        Date t = new Date(0L);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        
        tf.setTimeZone(DateUtil.UTC);
        assertEquals("00:00:00", tf.format(t));
        assertEquals("00:00:00",
                dateToISO8601UTCTimeString(t, false));
        
        TimeZone gmt1 = DateUtil.getTimeZone("GMT+01"); 
        tf.setTimeZone(gmt1);
        assertEquals("01:00:00", tf.format(t)); 
        assertEquals("01:00:00+01:00",
                dateToISO8601TimeString(t, gmt1));
    }
    
    public void testAccuracy() throws ParseException {
        Date d = df.parse("AD 2000-02-08 09:05:04:250 UTC"); 
        assertEquals("2000-02-08T09:05:04Z",
                dateToISO8601UTCDateTimeString(d, true));
        assertEquals("2000-02-08T09:05:04.25Z",
                dateToISO8601String(d, true, true, true,
                        DateUtil.ACCURACY_MILLISECONDS, null));
        assertEquals("2000-02-08T09:05:04Z",
                dateToISO8601String(d, true, true, true,
                        DateUtil.ACCURACY_SECONDS, null));
        assertEquals("2000-02-08T09:05Z",
                dateToISO8601String(d, true, true, true,
                        DateUtil.ACCURACY_MINUTES, null));
        assertEquals("2000-02-08T09Z",
                dateToISO8601String(d, true, true, true,
                        DateUtil.ACCURACY_HOURS, null));
        
        d = df.parse("AD 1998-10-30 19:30:00:000 +0400");
        assertEquals(
                "15:30:00Z",
                dateToISO8601UTCTimeMSString(d, true));
        assertEquals(
                "15:30:00.000Z",
                dateToISO8601UTCTimeMSFString(d, true));
        assertEquals(
                "1998-10-30T15:30:00Z",
                dateToISO8601UTCDateTimeMSString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.000Z",
                dateToISO8601UTCDateTimeMSFString(d, true));
                
        d = df.parse("AD 1998-10-30 19:30:00:100 +0400");
        assertEquals(
                "15:30:00.1Z",
                dateToISO8601UTCTimeMSString(d, true));
        assertEquals(
                "15:30:00.100Z",
                dateToISO8601UTCTimeMSFString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.1Z",
                dateToISO8601UTCDateTimeMSString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.100Z",
                dateToISO8601UTCDateTimeMSFString(d, true));
        
        d = df.parse("AD 1998-10-30 19:30:00:010 +0400");
        assertEquals(
                "15:30:00.01Z",
                dateToISO8601UTCTimeMSString(d, true));
        assertEquals(
                "15:30:00.010Z",
                dateToISO8601UTCTimeMSFString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.01Z",
                dateToISO8601UTCDateTimeMSString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.010Z",
                dateToISO8601UTCDateTimeMSFString(d, true));
        
        d = df.parse("AD 1998-10-30 19:30:00:001 +0400");
        assertEquals(
                "15:30:00.001Z",
                dateToISO8601UTCTimeMSString(d, true));
        assertEquals(
                "15:30:00.001Z",
                dateToISO8601UTCTimeMSFString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.001Z",
                dateToISO8601UTCDateTimeMSString(d, true));
        assertEquals(
                "1998-10-30T15:30:00.001Z",
                dateToISO8601UTCDateTimeMSFString(d, true));
    }

    public void testXSFormatISODeviations() throws ParseException, UnrecognizedTimeZoneException {
        Date dsum = df.parse("AD 2010-05-09 20:00:00:0 UTC");
        Date dwin = df.parse("AD 2010-01-01 20:00:00:0 UTC");
        
        TimeZone tzRome = DateUtil.getTimeZone("Europe/Rome");
        
        assertEquals(
                "2010-01-01T21:00:00+01:00",
                DateUtil.dateToXSString(dwin, true, true, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        assertEquals(
                "2010-05-09T22:00:00+02:00",
                DateUtil.dateToXSString(dsum, true, true, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        assertEquals(
                "2010-01-01+01:00",  // ISO doesn't allow date-only with TZ
                DateUtil.dateToXSString(dwin, true, false, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        assertEquals(
                "2010-05-09+02:00",  // ISO doesn't allow date-only with TZ
                DateUtil.dateToXSString(dsum, true, false, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        assertEquals(
                "21:00:00+01:00",
                DateUtil.dateToXSString(dwin, false, true, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        assertEquals(
                "22:00:00+02:00",
                DateUtil.dateToXSString(dsum, false, true, true, DateUtil.ACCURACY_SECONDS, tzRome, calendarFactory));
        
        assertEquals(
                "-1-02-29T06:15:24Z",  // ISO uses 0 for BC 1
                DateUtil.dateToXSString(
                        df.parse("BC 0001-03-02 09:15:24:0 +0300"),
                        true, true, true, DateUtil.ACCURACY_SECONDS, DateUtil.UTC, calendarFactory));
        assertEquals(
                "-2-02-28T06:15:24Z",  // ISO uses -1 for BC 2
                DateUtil.dateToXSString(
                        df.parse("BC 2-03-02 09:15:24:0 +0300"),
                        true, true, true, DateUtil.ACCURACY_SECONDS, DateUtil.UTC, calendarFactory));
    }
    
    private String dateToISO8601DateTimeString(
            Date date, TimeZone tz) {
        return dateToISO8601String(date, true, true, true,
                DateUtil.ACCURACY_SECONDS, tz);
    }
    
    private String dateToISO8601UTCDateTimeString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, true, true, offsetPart,
                DateUtil.ACCURACY_SECONDS, DateUtil.UTC);
    }

    private String dateToISO8601UTCDateTimeMSString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, true, true, offsetPart,
                DateUtil.ACCURACY_MILLISECONDS, DateUtil.UTC);
    }

    private String dateToISO8601UTCDateTimeMSFString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, true, true, offsetPart,
                DateUtil.ACCURACY_MILLISECONDS_FORCED, DateUtil.UTC);
    }
        
    private String dateToISO8601DateString(Date date, TimeZone tz) {
        return dateToISO8601String(date, true, false, false,
                DateUtil.ACCURACY_SECONDS, tz);
    }

    private String dateToISO8601UTCDateString(Date date) {
        return dateToISO8601String(date, true, false, false,
                DateUtil.ACCURACY_SECONDS, DateUtil.UTC);
    }
    
    private String dateToISO8601TimeString(
            Date date, TimeZone tz) {
        return dateToISO8601String(date, false, true, true,
                DateUtil.ACCURACY_SECONDS, tz);
    }
    
    private String dateToISO8601UTCTimeString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, false, true, offsetPart,
                DateUtil.ACCURACY_SECONDS, DateUtil.UTC);
    }

    private String dateToISO8601UTCTimeMSString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, false, true, offsetPart,
                DateUtil.ACCURACY_MILLISECONDS, DateUtil.UTC);
    }

    private String dateToISO8601UTCTimeMSFString(
            Date date, boolean offsetPart) {
        return dateToISO8601String(date, false, true, offsetPart,
                DateUtil.ACCURACY_MILLISECONDS_FORCED, DateUtil.UTC);
    }
    
    private String dateToISO8601String(
            Date date,
            boolean datePart, boolean timePart, boolean offsetPart,
            int accuracy,
            TimeZone timeZone) {
        return DateUtil.dateToISO8601String(
                date,
                datePart, timePart, offsetPart,
                accuracy,
                timeZone,
                calendarFactory);        
    }
    
    public void testParseDate() throws DateParseException {
        assertDateParsing(
                "AD 1998-10-29 20:00:00:0 +0000",
                null,
                "1998-10-30+04:00", DateUtil.UTC);
        assertDateParsing(
                "AD 1998-10-30 02:00:00:0 +0000",
                null,
                "1998-10-30-02:00", DateUtil.UTC);
        assertDateParsing(
                "AD 1998-10-30 02:00:00:0 +0000",
                "1998-10-30", DateUtil.parseXSTimeZone("-02:00"));
        assertDateParsing(
                null,
                "AD 1998-10-30 02:00:00:0 +0000",
                "19981030", DateUtil.parseXSTimeZone("-02:00"));
        assertDateParsing(
                "AD 1998-10-30 00:00:00:0 +0000",
                null,
                "1998-10-30Z", DateUtil.UTC);
        assertDateParsing(
                "AD 1998-10-30 00:00:00:0 +0000",
                "1998-10-30", DateUtil.UTC);
        assertDateParsing(
                null,
                "AD 1998-10-30 00:00:00:0 +0000",
                "19981030", DateUtil.UTC);

        assertDateParsing(
                "AD 1998-10-29 20:00:00:0 +0000",
                null,
                "1998-10-30+04:00", DateUtil.UTC);
        assertDateParsing(
                "AD 1998-10-30 04:00:00:0 +0000",
                null,
                "1998-10-30-04:00", DateUtil.UTC);
        assertDateParsing(
                "AD 1998-10-30 00:00:00:0 +0000",
                null,
                "1998-10-30Z", DateUtil.UTC);
        
        try {
            // XS doesn't have year 0
            assertDateParsing(
                    "BC 0000-02-05 00:00:00:0 +0000",
                    null,
                    "0000-02-03Z", DateUtil.UTC);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        assertDateParsing(
                null,
                "BC 0001-02-05 00:00:00:0 +0000",
                "0000-02-03", DateUtil.UTC);
        assertDateParsing(
                null,
                "BC 0001-02-05 00:00:00:0 +0000",
                "00000203", DateUtil.UTC);
        
        assertDateParsing(
                "BC 0001-02-05 00:00:00:0 +0000",  // Julian
                "BC 0002-02-05 00:00:00:0 +0000",  // Julian
                "-0001-02-03", DateUtil.UTC);  // Proleptic Gregorian
        assertDateParsing(
                null,
                "BC 0002-02-05 00:00:00:0 +0000",  // Julian
                "-00010203", DateUtil.UTC);  // Proleptic Gregorian

        assertDateParsing(
                "AD 0001-02-05 00:00:00:0 +0000",  // Julian
                null,
                "0001-02-03Z", DateUtil.UTC);  // Proleptic Gregorian
        assertDateParsing(
                "AD 0001-02-05 00:00:00:0 +0000",  // Julian
                "0001-02-03", DateUtil.UTC);  // Proleptic Gregorian
        assertDateParsing(
                null,
                "AD 0001-02-05 00:00:00:0 +0000",  // Julian
                "00010203", DateUtil.UTC);  // Proleptic Gregorian
        assertDateParsing(
                "AD 1001-12-07 00:00:00:0 +0000",  // Julian
                null,
                "1001-12-13Z", DateUtil.UTC);  // Proleptic Gregorian
        assertDateParsing(
                "AD 1001-12-07 00:00:00:0 +0000",  // Julian
                "1001-12-13", DateUtil.UTC);  // Proleptic Gregorian
        
        assertDateParsing(
                "AD 2006-12-31 00:00:00:0 +0000",
                null,
                "2006-12-31Z", DateUtil.UTC);
        assertDateParsing(
                "AD 2006-12-31 00:00:00:0 +0000",
                "2006-12-31", DateUtil.UTC);
        assertDateParsing(
                "AD 2006-01-01 00:00:00:0 +0000",
                null,
                "2006-01-01Z", DateUtil.UTC);
        assertDateParsing(
                "AD 2006-01-01 00:00:00:0 +0000",
                "2006-01-01", DateUtil.UTC);
        assertDateParsing(
                "AD 12006-01-01 00:00:00:0 +0000",
                "12006-01-01", DateUtil.UTC);
        assertDateParsing(
                null,
                "AD 12006-01-01 00:00:00:0 +0000",
                "120060101", DateUtil.UTC);
    }

    public void testParseDateMalformed() {
        assertDateMalformed("1998-10-30x");
        assertDateMalformed("+1998-10-30");
        assertDateMalformed("1998-10-");
        assertDateMalformed("1998-1-30");
        assertDateMalformed("1998-10-30+01");
        assertDateMalformed("1998-00-01");
        assertDateMalformed("1998-13-01");
        assertDateMalformed("1998-10-00");
        assertDateMalformed("1998-10-32");
        assertDateMalformed("1998-02-31");
        
        assertISO8601DateMalformed("2100103");
        assertISO8601DateMalformed("210-01-03");
        assertISO8601DateMalformed("2012-0301");
        assertISO8601DateMalformed("201203-01");
        assertISO8601DateMalformed("2012-01-01+01:00");
    }
    
    public void testParseTime() throws DateParseException {
        assertTimeParsing(
                "AD 1970-01-01 17:30:05:0 +0000",
                "17:30:05", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 17:30:05:0 +0000",
                "173005", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:100 +0000",
                "07:30:00.1", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:120 +0000",
                "07:30:00.12", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "07:30:00.123", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "07:30:00.1235", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "07:30:00.12346", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 07:30:00:123 +0000",
                "073000.12346", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 07:30:00:123 +0000",
                "073000,12346", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:120 +0000",
                "07:30:00.12", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 07:30:00:500 +0000",
                "07:30:00.5", DateUtil.UTC);

        assertTimeParsing(
                "AD 1970-01-01 16:30:05:0 +0000",
                "17:30:05+01:00", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:30:05:0 +0000",
                "173005+01", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 19:00:05:0 +0000",
                "17:30:05-01:30", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 19:00:05:0 +0000",
                "173005-0130", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-01 16:30:05:500 +0000",
                "17:30:05.5+01:00", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:30:05:500 +0000",
                "173005.5+0100", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:30:05:500 +0000",
                "173005.5+01", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:00:00:0 +0000",
                "170000+01", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:00:00:0 +0000",
                "1700+01", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-01 16:00:00:0 +0000",
                "17+01", DateUtil.UTC);
        
        assertTimeParsing(
                "AD 1970-01-01 00:00:00:0 +0000",
                "00:00:00", DateUtil.UTC);
        assertTimeParsing(
                "AD 1970-01-02 00:00:00:0 +0000",
                "24:00:00", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-02 00:00:00:0 +0000",
                "240000", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-02 00:00:00:0 +0000",
                "2400", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-02 00:00:00:0 +0000",
                "24:00", DateUtil.UTC);
        assertTimeParsing(
                null,
                "AD 1970-01-02 00:00:00:0 +0000",
                "24", DateUtil.UTC);
        
        assertTimeParsing(
                "AD 1970-01-01 23:59:59:999 +0000",
                "23:59:59.999", DateUtil.UTC);
    }

    public void testParseTimeMalformed() {
        assertTimeMalformed("00:0000");
        assertTimeMalformed("00:00:00-01:60");
        assertTimeMalformed("24:00:01");
        assertTimeMalformed("00:00:61");
        assertTimeMalformed("00:60:00");
        assertTimeMalformed("25:00:00");
        assertTimeMalformed("2:00:00");
        assertTimeMalformed("02:0:00");
        assertTimeMalformed("02:00:0");
        
        assertISO8601TimeMalformed("1010101");
        assertISO8601TimeMalformed("10101");
        assertISO8601TimeMalformed("101");
        assertISO8601TimeMalformed("1");
        assertISO8601TimeMalformed("101010-1");
        assertISO8601TimeMalformed("101010-100");
        assertISO8601TimeMalformed("101010-10000");
        assertISO8601TimeMalformed("101010+1");
        assertISO8601TimeMalformed("101010+100");
        assertISO8601TimeMalformed("101010+10000");
    }
    
    public void testParseDateTime() throws DateParseException {
        assertDateTimeParsing( 
                "AD 1998-10-30 11:30:00:0 +0000",
                "1998-10-30T15:30:00+04:00", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 11:30:00:0 +0000",
                "19981030T153000+0400", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 11:30:00:500 +0000",
                "1998-10-30T15:30:00.5+04:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 15:30:00:0 +0000",
                "1998-10-30T15:30:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 15:30:00:0 +0000",
                "19981030T1530Z", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 15:30:00:500 +0000",
                "1998-10-30T15:30:00.5Z", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 11:30:00:0 +0000",
                "1998-10-30T15:30:00+04:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 15:30:00:0 +0000",
                "1998-10-30T15:30:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 15:30:00:0 +0000",
                "1998-10-30T15:30:00", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 15:30:00:0 +0000",
                "1998-10-30T15:30", DateUtil.UTC);
        
        assertDateTimeParsing(
                "AD 1998-10-29 20:00:00:0 +0000",
                "1998-10-30T00:00:00+04:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 02:00:00:0 +0000",
                "1998-10-30T00:00:00-02:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 00:00:00:0 +0000",
                "1998-10-30T00:00:00Z", DateUtil.UTC);

        assertDateTimeParsing(
                "AD 1998-10-29 20:00:00:0 +0000",
                "1998-10-30T00:00:00+04:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1998-10-30 00:00:00:0 +0000",
                "1998-10-30T00:00:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 00:00:00:0 +0000",
                "1998-10-30T00:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 00:00:00:0 +0000",
                "1998-10-30T00:00", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 1998-10-30 00:00:00:0 +0000",
                "19981030T00Z", DateUtil.UTC);
        
        // BC years
        try {
            assertDateTimeParsing(
                        "",
                        null,
                        "0000-02-03T00:00:00Z", DateUtil.UTC);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        assertDateTimeParsing(
                null,
                "BC 0001-02-05 00:00:00:0 +0000",
                "0000-02-03T00:00:00Z", DateUtil.UTC);
        
        assertDateTimeParsing(
                "BC 0001-02-05 00:00:00:0 +0000",  // Julian
                "BC 0002-02-05 00:00:00:0 +0000",  // Julian
                "-0001-02-03T00:00:00Z", DateUtil.UTC);  // Proleptic Gregorian

        assertDateTimeParsing(
                "AD 0001-02-05 00:00:00:0 +0000",  // Julian
                "0001-02-03T00:00:00Z", DateUtil.UTC);  // Proleptic Gregorian
        assertDateTimeParsing(
                "AD 1001-12-07 00:00:00:0 +0000",  // Julian
                "1001-12-13T00:00:00Z", DateUtil.UTC);  // Proleptic Gregorian
        assertDateTimeParsing(
                "AD 11001-12-13 00:00:00:0 +0000",
                "11001-12-13T00:00:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                null,
                "AD 11001-12-13 00:00:00:0 +0000",
                "110011213T00Z", DateUtil.UTC);
        
        assertDateTimeParsing(
                "AD 2006-12-31 00:00:00:0 +0000",
                "2006-12-31T00:00:00Z", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 2006-01-01 00:00:00:0 +0000",
                "2006-01-01T00:00:00Z", DateUtil.UTC);
        
        assertDateTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "1970-01-01T07:30:00.123", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "1970-01-01T07:30:00.1235", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-01 07:30:00:123 +0000",
                "1970-01-01T07:30:00.12346", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-01 07:30:00:120 +0000",
                "1970-01-01T07:30:00.12", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-01 07:30:00:500 +0000",
                "1970-01-01T07:30:00.5", DateUtil.UTC);

        assertDateTimeParsing(
                "AD 1970-01-01 16:30:05:0 +0000",
                "1970-01-01T17:30:05+01:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-01 16:30:05:500 +0000",
                "1970-01-01T17:30:05.5+01:00", DateUtil.UTC);
        
        assertDateTimeParsing(
                "AD 1970-01-01 00:00:00:0 +0000",
                "1970-01-01T00:00:00", DateUtil.UTC);
        assertDateTimeParsing(
                "AD 1970-01-02 00:00:00:0 +0000",
                "1970-01-01T24:00:00", DateUtil.UTC);
        
        assertDateTimeParsing(
                "AD 1970-01-01 23:59:59:999 +0000",
                "1970-01-01T23:59:59.999", DateUtil.UTC);
    }

    public void testParseDateTimeMalformed() throws DateParseException {
        assertDateTimeMalformed("1998-00-01T00:00:00");
        assertDateTimeMalformed("1998-13-01T00:00:00");
        assertDateTimeMalformed("1998-10-00T00:00:00");
        assertDateTimeMalformed("1998-10-32T00:00:00");
        assertDateTimeMalformed("1998-02-31T00:00:00");
        assertDateTimeMalformed("1970-01-02T24:00:01");
        assertDateTimeMalformed("1970-01-01T00:00:61");
        assertDateTimeMalformed("1970-01-01T00:60:00");
        assertDateTimeMalformed("1970-01-01T25:00:00");
        
        assertISO8601DateTimeMalformed("197-01-01T20:00:00");
    }
    
    public void testParseXSTimeZone() throws DateParseException {
        assertEquals(0,
                DateUtil.parseXSTimeZone("Z").getOffset(0));
        assertEquals(0,
                DateUtil.parseXSTimeZone("-00:00").getOffset(0));
        assertEquals(0,
                DateUtil.parseXSTimeZone("+00:00").getOffset(0));
        assertEquals(90 * 60 * 1000,
                DateUtil.parseXSTimeZone("+01:30").getOffset(0));
        assertEquals(-4 * 60 * 60 * 1000,
                DateUtil.parseXSTimeZone("-04:00").getOffset(0));
        assertEquals(((-23 * 60) - 59) * 60 * 1000,
                DateUtil.parseXSTimeZone("-23:59").getOffset(0));
        assertEquals(((23 * 60) + 59) * 60 * 1000,
                DateUtil.parseXSTimeZone("+23:59").getOffset(0));
    }

    public void testParseXSTimeZoneWrong() {
        try {
            DateUtil.parseXSTimeZone("04:00").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        try {
            DateUtil.parseXSTimeZone("-04:00x").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        try {
            DateUtil.parseXSTimeZone("-04").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        try {
            DateUtil.parseXSTimeZone("+24:00").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        try {
            DateUtil.parseXSTimeZone("-24:00").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
        try {
            DateUtil.parseXSTimeZone("-01:60").getOffset(0);
            fail();
        } catch (DateParseException e) {
            echo(e);
        }
    }
    
    public void testParseXSDateTimeFTLAndJavax() throws DateParseException {
        // Explicit time zone:
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T13:35:08Z");
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T13:35:08+02:00");
        
        // Default time zone:
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T13:35:08"); // winter
        assertJavaxAndFTLXSDateTimesSame("2014-07-01T13:35:08"); // summer
        
        // Proleptic Gregorian
        assertJavaxAndFTLXSDateTimesSame("1500-01-01T13:35:08Z");
        assertJavaxAndFTLXSDateTimesSame("0200-01-01T13:35:08Z");
        assertJavaxAndFTLXSDateTimesSame("0001-01-01T00:00:00+05:00");
        
        // BC
        assertJavaxAndFTLXSDateTimesSame("0001-01-01T13:35:08Z");
        assertJavaxAndFTLXSDateTimesSame("-0001-01-01T13:35:08Z");
        
        // Hour 24
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T23:59:59");
        if (isAtLeastJava6()) { // Java 5 has broken parser that doesn't allow 24.
            assertJavaxAndFTLXSDateTimesSame("2014-01-31T24:00:00");
            assertJavaxAndFTLXSDateTimesSame("2014-01-01T24:00:00");
        }
        assertJavaxAndFTLXSDateTimesSame("2014-01-02T00:00:00");  // same as the previous
        assertJavaxAndFTLXSDateTimesSame("2014-02-01T00:00:00");  // same as the previous
        
        // Under ms
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T23:59:59.123456789");
        assertJavaxAndFTLXSDateTimesSame("2014-01-01T23:59:59.1235");
    }
    
    private boolean isAtLeastJava6() {
        try {
            Class.forName("java.lang.management.LockInfo");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private final DatatypeFactory datetypeFactory;
    {
        try {
            datetypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        } 
    }
    
    private void assertJavaxAndFTLXSDateTimesSame(String s) throws DateParseException {
        XMLGregorianCalendar xgc = datetypeFactory.newXMLGregorianCalendar(s);
        Date javaxDate = xgc.toGregorianCalendar().getTime();
        Date ftlDate = DateUtil.parseXSDateTime(s, TimeZone.getDefault(), cf2dc);
        assertEquals(javaxDate, ftlDate);
    }

    private void assertDateParsing(String expected, String parsed, TimeZone tz) throws DateParseException {
        assertDateParsing(expected, expected, parsed, tz);
    }

    private void assertDateParsing(String expectedXS, String expectedISO8601, String parsed, TimeZone tz)
            throws DateParseException {
        if (expectedXS != null) {
            assertEquals(
                    expectedXS,
                    df.format(DateUtil.parseXSDate(parsed, tz, cf2dc)));
        }
        if (expectedISO8601 != null) {
            assertEquals(
                    expectedISO8601,
                    df.format(DateUtil.parseISO8601Date(parsed, tz, cf2dc)));
        }
    }

    private void assertDateTimeParsing(String expected, String parsed, TimeZone tz) throws DateParseException {
        assertDateTimeParsing(expected, expected, parsed, tz);
    }

    private void assertDateTimeParsing(String expectedXS, String expectedISO8601, String parsed, TimeZone tz)
            throws DateParseException {
        if (expectedXS != null) {
            assertEquals(
                    expectedXS,
                    df.format(DateUtil.parseXSDateTime(parsed, tz, cf2dc)));
        }
        if (expectedISO8601 != null) {
            assertEquals(
                    expectedISO8601,
                    df.format(DateUtil.parseISO8601DateTime(parsed, tz, cf2dc)));
        }
    }

    private void assertTimeParsing(String expected, String parsed, TimeZone tz) throws DateParseException {
        assertTimeParsing(expected, expected, parsed, tz);
    }

    private void assertTimeParsing(String expectedXS, String expectedISO8601, String parsed, TimeZone tz)
            throws DateParseException {
        if (expectedXS != null) {
            assertEquals(
                    expectedXS,
                    df.format(DateUtil.parseXSTime(parsed, tz, cf2dc)));
        }
        if (expectedISO8601 != null) {
            assertEquals(
                    expectedISO8601,
                    df.format(DateUtil.parseISO8601Time(parsed, tz, cf2dc)));
        }
    }
    
    private void assertDateMalformed(String parsed) {
        try {
            DateUtil.parseXSDate(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
        try {
            DateUtil.parseISO8601Date(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }

    private void assertTimeMalformed(String parsed) {
        try {
            DateUtil.parseXSTime(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
        try {
            DateUtil.parseISO8601Time(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }

    private void assertDateTimeMalformed(String parsed) {
        try {
            DateUtil.parseXSDateTime(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
        try {
            DateUtil.parseISO8601DateTime(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }

    private void assertISO8601DateMalformed(String parsed) {
        try {
            DateUtil.parseISO8601Date(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }
    
    private void assertISO8601TimeMalformed(String parsed) {
        try {
            DateUtil.parseISO8601Time(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }
    
    private void assertISO8601DateTimeMalformed(String parsed) {
        try {
            DateUtil.parseISO8601DateTime(parsed, DateUtil.UTC, cf2dc);
            fail();
        } catch (DateParseException e) {
            // Expected
            echo(e);
        }
    }
    
    private void echo(@SuppressWarnings("unused") DateParseException e) {
        // System.out.println(e);
    }

}