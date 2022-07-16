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

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import freemarker.template.Configuration;

/**
 * Used as a parameter to {@link TemplateTemporalFormat#parse(String, MissingTimeZoneParserPolicy)}, specifies what to
 * do if we have to parse a string that contains no time zone, nor offset information to a non-local {@link Temporal}
 * (like to {@link OffsetDateTime}).
 *
 * <p>There's no {@link Configuration} setting for this. Instead, the build-ins that parse to given non-local temporal
 * type have 3 variants, one for each policy. For example, in the case of parsing a string to {@link OffsetDateTime},
 * {@code ?offset_date_time} uses {@link #ASSUME_CURRENT_TIME_ZONE}, {@code ?offset_or_local_date_time} uses {@link
 * #FALL_BACK_TO_LOCAL_TEMPORAL}, and {@code ?unambiguous_offset_date_time} uses {@link #FAIL}.
 *
 * <p>This is not used when parsing to {@link java.util.Date}, and the policy there is always effectively
 * {@link #ASSUME_CURRENT_TIME_ZONE}.
 *
 * @since 2.3.32
 */
public enum MissingTimeZoneParserPolicy {
    /**
     * Use {@link Environment#getTimeZone()}.
     */
    ASSUME_CURRENT_TIME_ZONE,
    /**
     * Return a local temporal instead of the requested type.
     */
    FALL_BACK_TO_LOCAL_TEMPORAL,
    /**
     * Give up with error.
     */
    FAIL
}
