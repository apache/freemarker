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

package freemarker.core;

import static org.junit.Assert.*;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.utility.DateUtil;
import freemarker.test.TemplateTest;

public class SQLTimeZoneTest extends TemplateTest {

    private final static TimeZone GMT_P02 = TimeZone.getTimeZone("GMT+02");
    
    private TimeZone lastDefaultTimeZone;

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    {
        df.setTimeZone(DateUtil.UTC);
    }
    
    // Values that JDBC in GMT+02 would produce
    private final java.sql.Date sqlDate = new java.sql.Date(utcToLong("2014-07-11T22:00:00")); // 2014-07-12
    private final Time sqlTime = new Time(utcToLong("1970-01-01T10:30:05")); // 12:30:05
    private final Timestamp sqlTimestamp = new Timestamp(utcToLong("2014-07-12T10:30:05")); // 2014-07-12T12:30:05
    private final Date javaDate = new Date(utcToLong("2014-07-12T10:30:05")); // 2014-07-12T12:30:05
    private final Date javaDayErrorDate = new Date(utcToLong("2014-07-11T22:00:00")); // 2014-07-12T12:30:05
    
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
    
    public Date getJavaDayErrorDate() {
        return javaDayErrorDate;
    }

    private static final String FTL =
            "${sqlDate} ${sqlTime} ${sqlTimestamp} ${javaDate?datetime}\n"
            + "${sqlDate?string.iso_fz} ${sqlTime?string.iso_fz} "
            + "${sqlTimestamp?string.iso_fz} ${javaDate?datetime?string.iso_fz}\n"
            + "${sqlDate?string.xs_fz} ${sqlTime?string.xs_fz} "
            + "${sqlTimestamp?string.xs_fz} ${javaDate?datetime?string.xs_fz}\n"
            + "${sqlDate?string.xs} ${sqlTime?string.xs} "
            + "${sqlTimestamp?string.xs} ${javaDate?datetime?string.xs}\n"
            + "<#setting time_zone='GMT'>\n"
            + "${sqlDate} ${sqlTime} ${sqlTimestamp} ${javaDate?datetime}\n"
            + "${sqlDate?string.iso_fz} ${sqlTime?string.iso_fz} "
            + "${sqlTimestamp?string.iso_fz} ${javaDate?datetime?string.iso_fz}\n"
            + "${sqlDate?string.xs_fz} ${sqlTime?string.xs_fz} "
            + "${sqlTimestamp?string.xs_fz} ${javaDate?datetime?string.xs_fz}\n"
            + "${sqlDate?string.xs} ${sqlTime?string.xs} "
            + "${sqlTimestamp?string.xs} ${javaDate?datetime?string.xs}\n";

    private static final String OUTPUT_BEFORE_SETTING_GMT_CFG_GMT2
            = "2014-07-12 12:30:05 2014-07-12T12:30:05 2014-07-12T12:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T12:30:05+02:00 2014-07-12T12:30:05+02:00\n"
            + "2014-07-12+02:00 12:30:05+02:00 2014-07-12T12:30:05+02:00 2014-07-12T12:30:05+02:00\n"
            + "2014-07-12 12:30:05 2014-07-12T12:30:05+02:00 2014-07-12T12:30:05+02:00\n";

    private static final String OUTPUT_BEFORE_SETTING_GMT_CFG_GMT1_SQL_DIFFERENT
            = "2014-07-12 12:30:05 2014-07-12T11:30:05 2014-07-12T11:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n"
            + "2014-07-12+02:00 12:30:05+02:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n"
            + "2014-07-12 12:30:05 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n";

    private static final String OUTPUT_BEFORE_SETTING_GMT_CFG_GMT1_SQL_SAME
            = "2014-07-11 11:30:05 2014-07-12T11:30:05 2014-07-12T11:30:05\n"
            + "2014-07-11 11:30:05+01:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n"
            + "2014-07-11+01:00 11:30:05+01:00 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n"
            + "2014-07-11 11:30:05 2014-07-12T11:30:05+01:00 2014-07-12T11:30:05+01:00\n";
    
    private static final String OUTPUT_AFTER_SETTING_GMT_CFG_SQL_SAME
            = "2014-07-11 10:30:05 2014-07-12T10:30:05 2014-07-12T10:30:05\n"
            + "2014-07-11 10:30:05Z 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n"
            + "2014-07-11Z 10:30:05Z 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n"
            + "2014-07-11 10:30:05 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n";
    
    private static final String OUTPUT_AFTER_SETTING_GMT_CFG_SQL_DIFFERENT
            = "2014-07-12 12:30:05 2014-07-12T10:30:05 2014-07-12T10:30:05\n"
            + "2014-07-12 12:30:05+02:00 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n"
            + "2014-07-12+02:00 12:30:05+02:00 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n"
            + "2014-07-12 12:30:05 2014-07-12T10:30:05Z 2014-07-12T10:30:05Z\n";
    
    @Test
    public void testWithDefaultTZAndNullSQL() throws Exception {
        TimeZone prevSysDefTz = TimeZone.getDefault();
        TimeZone.setDefault(GMT_P02);
        try {
            Configuration cfg = createConfiguration();
            setConfiguration(cfg);
            
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            
            assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT2 + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_SAME);
        } finally {
            TimeZone.setDefault(prevSysDefTz);
        }
    }

    @Test
    public void testWithDefaultTZAndGMT2SQL() throws Exception {
        TimeZone prevSysDefTz = TimeZone.getDefault();
        TimeZone.setDefault(GMT_P02);
        try {
            Configuration cfg = createConfiguration();
            cfg.setSQLDateAndTimeTimeZone(GMT_P02);
            setConfiguration(cfg);
            
            assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT2 + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_DIFFERENT);
        } finally {
            TimeZone.setDefault(prevSysDefTz);
        }
    }
    
    @Test
    public void testWithGMT1AndNullSQL() throws Exception {
        Configuration cfg = createConfiguration();
        assertNull(cfg.getSQLDateAndTimeTimeZone());
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        setConfiguration(cfg);
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT1_SQL_SAME + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_SAME);
    }

    @Test
    public void testWithGMT1AndGMT2SQL() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setSQLDateAndTimeTimeZone(GMT_P02);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
        setConfiguration(cfg);
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT1_SQL_DIFFERENT + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_DIFFERENT);
    }

    @Test
    public void testWithGMT2AndNullSQL() throws Exception {
        Configuration cfg = createConfiguration();
        assertNull(cfg.getSQLDateAndTimeTimeZone());
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+02"));
        setConfiguration(cfg);
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT2 + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_SAME);
    }

    @Test
    public void testWithGMT2AndGMT2SQL() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setSQLDateAndTimeTimeZone(GMT_P02);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+02"));
        setConfiguration(cfg);
        
        assertOutput(FTL, OUTPUT_BEFORE_SETTING_GMT_CFG_GMT2 + OUTPUT_AFTER_SETTING_GMT_CFG_SQL_DIFFERENT);
    }
    
    @Test
    public void testCacheFlushings() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setTimeZone(DateUtil.UTC);
        cfg.setDateFormat("yyyy-MM-dd E");
        cfg.setTimeFormat("HH:mm:ss E");
        cfg.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss E");
        setConfiguration(cfg);
        
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting locale='de'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fr, 10:30:05 Do, 2014-07-12T10:30:05 Sa, 2014-07-12T10:30:05 Sa, 2014-07-12 Sa, 10:30:05 Sa\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting date_format='yyyy-MM-dd'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12, 10:30:05 Sat\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting time_format='HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fri, 10:30:05, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting datetime_format='yyyy-MM-dd\\'T\\'HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-11 Fri, 10:30:05 Thu, 2014-07-12T10:30:05, 2014-07-12T10:30:05, 2014-07-12 Sat, 10:30:05 Sat\n");
        
        cfg.setSQLDateAndTimeTimeZone(GMT_P02);
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting locale='de'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sa, 12:30:05 Do, 2014-07-12T10:30:05 Sa, 2014-07-12T10:30:05 Sa, 2014-07-12 Sa, 10:30:05 Sa\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting date_format='yyyy-MM-dd'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12, 10:30:05 Sat\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting time_format='HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sat, 12:30:05, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05\n");
        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n"
                + "<#setting datetime_format='yyyy-MM-dd\\'T\\'HH:mm:ss'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}, ${javaDate?date}, ${javaDate?time}\n",
                "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05 Sat, 2014-07-12T10:30:05 Sat, 2014-07-12 Sat, 10:30:05 Sat\n"
                + "2014-07-12 Sat, 12:30:05 Thu, 2014-07-12T10:30:05, 2014-07-12T10:30:05, 2014-07-12 Sat, 10:30:05 Sat\n");
    }

    @Test
    public void testDateAndTimeBuiltInsHasNoEffect() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setTimeZone(DateUtil.UTC);
        cfg.setSQLDateAndTimeTimeZone(GMT_P02);
        setConfiguration(cfg);
        assertOutput(
                "${javaDayErrorDate?date} ${javaDayErrorDate?time} ${sqlTimestamp?date} ${sqlTimestamp?time} "
                + "${sqlDate?date} ${sqlTime?time}\n"
                + "<#setting time_zone='GMT+02'>\n"
                + "${javaDayErrorDate?date} ${javaDayErrorDate?time} ${sqlTimestamp?date} ${sqlTimestamp?time} "
                + "${sqlDate?date} ${sqlTime?time}\n"
                + "<#setting time_zone='GMT-11'>\n"
                + "${javaDayErrorDate?date} ${javaDayErrorDate?time} ${sqlTimestamp?date} ${sqlTimestamp?time} "
                + "${sqlDate?date} ${sqlTime?time}\n",
                "2014-07-11 22:00:00 2014-07-12 10:30:05 2014-07-12 12:30:05\n"
                + "2014-07-12 00:00:00 2014-07-12 12:30:05 2014-07-12 12:30:05\n"
                + "2014-07-11 11:00:00 2014-07-11 23:30:05 2014-07-12 12:30:05\n");
    }

    @Test
    public void testChangeSettingInTemplate() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setTimeZone(DateUtil.UTC);
        setConfiguration(cfg);
        
        assertNull(cfg.getSQLDateAndTimeTimeZone());

        assertOutput(
                "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting sql_date_and_time_time_zone='GMT+02'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting sql_date_and_time_time_zone='null'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting time_zone='GMT+03'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting sql_date_and_time_time_zone='GMT+02'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting sql_date_and_time_time_zone='GMT-11'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting date_format='xs fz'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting time_format='xs fz'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n"
                + "<#setting datetime_format='iso m'>\n"
                + "${sqlDate}, ${sqlTime}, ${sqlTimestamp}, ${javaDate?datetime}\n",
                "2014-07-11, 10:30:05, 2014-07-12T10:30:05, 2014-07-12T10:30:05\n"
                + "2014-07-12, 12:30:05, 2014-07-12T10:30:05, 2014-07-12T10:30:05\n"
                + "2014-07-11, 10:30:05, 2014-07-12T10:30:05, 2014-07-12T10:30:05\n"
                + "2014-07-12, 13:30:05, 2014-07-12T13:30:05, 2014-07-12T13:30:05\n"
                + "2014-07-12, 12:30:05, 2014-07-12T13:30:05, 2014-07-12T13:30:05\n"
                + "2014-07-11, 23:30:05, 2014-07-12T13:30:05, 2014-07-12T13:30:05\n"
                + "2014-07-11-11:00, 23:30:05, 2014-07-12T13:30:05, 2014-07-12T13:30:05\n"
                + "2014-07-11-11:00, 23:30:05-11:00, 2014-07-12T13:30:05, 2014-07-12T13:30:05\n"
                + "2014-07-11-11:00, 23:30:05-11:00, 2014-07-12T13:30+03:00, 2014-07-12T13:30+03:00\n");
    }
    
    @Test
    public void testFormatUTCFlagHasNoEffect() throws Exception {
        Configuration cfg = createConfiguration();
        cfg.setSQLDateAndTimeTimeZone(GMT_P02);
        cfg.setTimeZone(TimeZone.getTimeZone("GMT-01"));
        setConfiguration(cfg);
        
        assertOutput(
                "<#setting date_format='xs fz'><#setting time_format='xs fz'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n"
                + "<#setting date_format='xs fz u'><#setting time_format='xs fz u'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n"
                + "<#setting sql_date_and_time_time_zone='GMT+03'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n"
                + "<#setting sql_date_and_time_time_zone='null'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n"
                + "<#setting date_format='xs fz'><#setting time_format='xs fz'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n"
                + "<#setting date_format='xs fz fu'><#setting time_format='xs fz fu'>\n"
                + "${sqlDate}, ${sqlTime}, ${javaDate?time}\n",
                "2014-07-12+02:00, 12:30:05+02:00, 09:30:05-01:00\n"
                + "2014-07-12+02:00, 12:30:05+02:00, 10:30:05Z\n"
                + "2014-07-12+03:00, 13:30:05+03:00, 10:30:05Z\n"
                + "2014-07-11-01:00, 09:30:05-01:00, 10:30:05Z\n"
                + "2014-07-11-01:00, 09:30:05-01:00, 09:30:05-01:00\n"
                + "2014-07-11Z, 10:30:05Z, 10:30:05Z\n");
    }
    
    private Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
        cfg.setLocale(Locale.US);
        cfg.setDateFormat("yyyy-MM-dd");
        cfg.setTimeFormat("HH:mm:ss");
        cfg.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
        return cfg;
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
