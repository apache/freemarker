package freemarker.template.utility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;

public class DateUtilTest extends TestCase {

    private final DateFormat df
            = new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss:S Z", Locale.US);
    {
        df.setTimeZone(DateUtil.UTC);
    }
    
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
                "0100-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse(
                        "AD 0100-02-28 09:15:24:0 +0300"), true));
        assertEquals(
                "0010-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("AD 0010-02-28 09:15:24:0 +0300"), true));
        assertEquals(
                "0001-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("AD 0001-02-28 09:15:24:0 +0300"), true));
        assertEquals(
                "0000-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("BC 0001-02-28 09:15:24:0 +0300"), true));
        assertEquals(
                "-1-02-28T06:15:24Z",
                dateToISO8601UTCDateTimeString(
                        df.parse("BC 2-02-28 09:15:24:0 +0300"), true));
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
    
}
