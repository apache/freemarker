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
package freemarker.template;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

/**
 * Any {@link Temporal} value that's included in Java; in Java 8 these are: {@link LocalDateTime}, {@link LocalDate},
 * {@link LocalTime}, {@link OffsetDateTime}, {@link OffsetTime}, {@link ZonedDateTime},
 * {@link YearMonth}, {@link Year}.
 * This does not deal with {@link java.time.Duration}, and {@link java.time.Period}, because those don't implement the
 * {@link Temporal} interface.
 *
 * <p>Objects of this type should be immutable, that is, {@link #getAsTemporal()}} should always return the same value
 * as for the first time.
 *
 * <p>{@link java.util.Date} values (the way date/time values were represented prior Java 8) are handled by
 * {@link TemplateDateModel}.
 *
 * @since 2.3.33
 */
public interface TemplateTemporalModel extends TemplateModel {
	/**
	 * Returns the temporal value; can't be {@code null}.
	 */
	Temporal getAsTemporal() throws TemplateModelException;
}
