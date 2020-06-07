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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import freemarker.template.Configuration;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't.
 */
public class _CoreTemporalUtils {

    private _CoreTemporalUtils() {
        // No meant to be instantiated
    }

    /**
     * {@link Temporal} subclasses directly supperted by FreeMarker.
     */
    public static final List<Class<? extends Temporal>> SUPPORTED_TEMPORAL_CLASSES = Arrays.asList(
            Instant.class,
            LocalDate.class,
            LocalDateTime.class,
            LocalTime.class,
            OffsetDateTime.class,
            OffsetTime.class,
            ZonedDateTime.class,
            Year.class,
            YearMonth.class);

    static final boolean SUPPORTED_TEMPORAL_CLASSES_ARE_FINAL = SUPPORTED_TEMPORAL_CLASSES.stream()
            .allMatch(cl -> (cl.getModifiers() & Modifier.FINAL) == Modifier.FINAL);

    /**
     * Ensures that {@code ==} can be used to check if the class is assignable to one of the {@link Temporal} subclasses
     * that FreeMarker directly supports. At least in Java 8 they are all final anyway, but just in case this changes in
     * a future Java version, use this method before using {@code ==}.
     *
     * @since 2.3.31
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
                return temporalClass;
            }
        }
    }

    /**
     * @throws IllegalArgumentException If {@link temporalClass} is not a supported {@link Temporal} subclass.
     */
    public static String temporalClassToFormatSettingName(Class<? extends Temporal> temporalClass) {
        temporalClass = normalizeSupportedTemporalClass(temporalClass);
        if (temporalClass == Instant.class) {
            return Configuration.INSTANT_FORMAT_KEY;
        } else if (temporalClass == LocalDate.class) {
            return Configuration.LOCAL_DATE_FORMAT_KEY;
        } else if (temporalClass == LocalDateTime.class) {
            return Configuration.LOCAL_DATE_TIME_FORMAT_KEY;
        } else if (temporalClass == LocalTime.class) {
            return Configuration.LOCAL_TIME_FORMAT_KEY;
        } else if (temporalClass == OffsetDateTime.class) {
            return Configuration.OFFSET_DATE_TIME_FORMAT_KEY;
        } else if (temporalClass == OffsetTime.class) {
            return Configuration.OFFSET_TIME_FORMAT_KEY;
        } else if (temporalClass == ZonedDateTime.class) {
            return Configuration.ZONED_DATE_TIME_FORMAT_KEY;
        } else if (temporalClass == YearMonth.class) {
            return Configuration.YEAR_MONTH_FORMAT_KEY;
        } else if (temporalClass == Year.class) {
            return Configuration.YEAR_FORMAT_KEY;
        } else {
            throw new IllegalArgumentException("Unsupported temporal class: " + temporalClass.getName());
        }
    }
    
}
