package freemarker.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template.utility.DateUtil;

public class SQLTimeZoneTest extends TemplateOutputTest {
    
    private TimeZone lastDefaultTimeZone;

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    {
        df.setTimeZone(DateUtil.UTC);
    }
    
    // Values that JDBC in GMT+02:00 would produce
    private final java.sql.Date sqlDate = new java.sql.Date(utcToLong("2014-07-11T22:00:00")); // 2014-07-12
    private final Time sqlTime = new Time(utcToLong("1970-01-01T10:30:05")); // 12:30:05
    private final Timestamp sqlTimestamp = new Timestamp(utcToLong("2014-07-12T10:30:05")); // 2014-07-12T12:30:05
    private final Date javaDate = new Date(utcToLong("2014-07-12T10:30:05")); // 2014-07-12T12:30:05
    
    public TimeZone getLastDefaultTimeZone() {
        return lastDefaultTimeZone;
    }

    public void setLastDefaultTimeZone(TimeZone lastDefaultTimeZone) {
        this.lastDefaultTimeZone = lastDefaultTimeZone;
    }

    public java.sql.Date getSqlDate() {
        return sqlDate;
    }

    public Time getSqlTime() {
        return sqlTime;
    }

    public Timestamp getSqlTimestamp() {
        return sqlTimestamp;
    }

    public Date getJavaDate() {
        return javaDate;
    }

    @Before
    public void setup() {
        lastDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"));
    }
    
    private static final String FTL =
            "${sqlDate} ${sqlTime} ${sqlTimestamp} ${javaDate?datetime}\n"
            + "${sqlDate?iso_local} ${sqlTime?iso_local} ${sqlTimestamp?iso_local} ${javaDate?datetime?iso_local}\n"
            + "<#setting time_zone='GMT'>\n"
            + "${sqlDate} ${sqlTime} ${sqlTimestamp} ${javaDate?datetime}\n"
            + "${sqlDate?iso_local} ${sqlTime?iso_local} ${sqlTimestamp?iso_local} ${javaDate?datetime?iso_local}\n";

    private static final String OUTPUT_BEFORE_SETTZ_GMT2
            = "2014-07-12 12:30:05 2014-07-12T12:30:05 2014-07-12T12:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T12:30:05+02:00 2014-07-12T12:30:05+02:00\n";

    private static final String OUTPUT_BEFORE_SETTZ_GMT1_SQL_DIFFERENT
            = "2014-07-12 12:30:05 2014-07-12T11:30:05 2014-07-12T11:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n";

    private static final String OUTPUT_BEFORE_SETTZ_GMT1_SQL_SAME
            = "2014-07-11 11:30:05 2014-07-12T11:30:05 2014-07-12T11:30:05\n"
            + "2014-07-11 11:30:05+01:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n";
    
    private static final String OUTPUT_AFTER_SETTZ_SQL_SAME
            = "2014-07-11 10:30:05 2014-07-12T10:30:05 2014-07-12T10:30:05\n"
            + "2014-07-11 10:30:05Z 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n";
    
    private static final String OUTPUT_AFTER_SETTZ_SQL_DIFFERENT
            = "2014-07-12 12:30:05 2014-07-12T10:30:05 2014-07-12T10:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n";
    
    @Test
    public void testWithDefaultTZAndNoUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        assertFalse(cfg.getUseSystemDefaultTimeZoneForSQLDateAndTime());
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT2 + OUTPUT_AFTER_SETTZ_SQL_SAME, cfg);
    }

    @Test
    public void testWithDefaultTZAndUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setUseSystemDefaultTimeZoneForSQLDateAndTime(true);
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT2 + OUTPUT_AFTER_SETTZ_SQL_DIFFERENT, cfg);
    }
    
    @Test
    public void testWithGMT1AndNoUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        assertFalse(cfg.getUseSystemDefaultTimeZoneForSQLDateAndTime());
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT1_SQL_SAME + OUTPUT_AFTER_SETTZ_SQL_SAME, cfg);
    }

    @Test
    public void testWithGMT1AndUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setUseSystemDefaultTimeZoneForSQLDateAndTime(true);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT1_SQL_DIFFERENT + OUTPUT_AFTER_SETTZ_SQL_DIFFERENT, cfg);
    }

    @Test
    public void testWithGMT2AndNoUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        assertFalse(cfg.getUseSystemDefaultTimeZoneForSQLDateAndTime());
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT2 + OUTPUT_AFTER_SETTZ_SQL_SAME, cfg);
    }

    @Test
    public void testWithGMT2AndUseDefSysForSQL() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setUseSystemDefaultTimeZoneForSQLDateAndTime(true);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTZ_GMT2 + OUTPUT_AFTER_SETTZ_SQL_DIFFERENT, cfg);
    }
    
    @Test
    public void testCacheFlushings() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setTimeZone(DateUtil.UTC);
        cfg.setDateFormat("yyyy-MM-dd E");
        cfg.setTimeFormat("HH:mm:ss E");
        cfg.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss E");
        
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting locale='de'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fr, 10:30:05 Do, 2014-07-12T10:30:05 Sa, 2014-07-12T10:30:05 Sa, 2014-07-12 Sa, 10:30:05 Sa\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting date_format='yyyy-MM-dd'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12, 10:30:05 Sat\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting time_format='HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fri, 10:30:05, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting datetime_format='yyyy-MM-dd\\'T\\'HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05, 2014-07-12T10:30:05, 2014-07-12 Sat, 10:30:05 Sat\n",
                cfg);
        
        cfg.setUseSystemDefaultTimeZoneForSQLDateAndTime(true);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting locale='de'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sa, 12:30:05 Do, 2014-07-12T10:30:05 Sa, 2014-07-12T10:30:05 Sa, 2014-07-12 Sa, 10:30:05 Sa\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting date_format='yyyy-MM-dd'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12, 10:30:05 Sat\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting time_format='HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sat, 12:30:05, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05\n",
                cfg);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting datetime_format='yyyy-MM-dd\\'T\\'HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05, 2014-07-12T10:30:05, 2014-07-12 Sat, 10:30:05 Sat\n",
                cfg);
    }
    
    private Configuration createConfiguration() {
        Configuration cfg = new Configuration(new Version(2, 3, 21));
        cfg.setLocale(Locale.US);
        cfg.setDateFormat("yyyy-MM-dd");
        cfg.setTimeFormat("HH:mm:ss");
        cfg.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
        return cfg;
    }
    
    @After
    public void teardown() {
        TimeZone.setDefault(lastDefaultTimeZone);
    }
    
    @Override
    protected Object createDataModel() {
        return this;
    }

    private long utcToLong(String isoDateTime) {
        try {
            return df.parse(isoDateTime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
}
