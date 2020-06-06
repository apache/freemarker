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

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.Temporal;

import org.junit.Test;

import freemarker.template.Configuration;

public class TemporalConfigurableTest {

    @Test
    public void testSupportedTemporalClassAreFinal() {
        assertTrue(
                "FreeMarker was implemented with the assumption that temporal classes are final. While there "
                        + "are mesures in palce to handle if it's not a case, it would be better to review the code.",
                _CoreTemporalUtils.SUPPORTED_TEMPORAL_CLASSES_ARE_FINAL);
    }

    @Test
    public void testGetTemporalFormat() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        for (Class<? extends Temporal> supportedTemporalClass : _CoreTemporalUtils.SUPPORTED_TEMPORAL_CLASSES) {
            assertNotNull(cfg.getTemporalFormat(supportedTemporalClass));
        }

        try {
            assertNotNull(cfg.getTemporalFormat(ChronoLocalDate.class));
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

}