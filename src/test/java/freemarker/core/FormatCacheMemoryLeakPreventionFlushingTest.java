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

import org.junit.Test;

import freemarker.template.TemplateDateModel;

/**
 * This is to cover the very rare situation where a format cache grows too big, and we fo some sort of flushing to
 * prevent a memory leak. The main goals is to catch any exception causing bug in that rarely ran code branch. But we
 * also assert when exactly the flushing happens; that can change in the future, in which case adjust the tests.
 */
public class FormatCacheMemoryLeakPreventionFlushingTest
        extends AbstractTemplateTemporalFormatAbstractCachingInEnvironmentTest {

    @Test
    public void testGlobalJavaTemporalFormatCacheLeakProtection() throws Exception {
        JavaTemplateTemporalFormatFactory formatFactory = JavaTemplateTemporalFormatFactory.INSTANCE;
        formatFactory.clear();
        int expectedMapSize = 0;
        for (int i = 0; i < JavaTemplateTemporalFormatFactory.GUARANTEED_RECENT_ENTRIES * 3 * 2 + 1; i++) {
            assertEquals(expectedMapSize, formatFactory.getSize());
            formatFactory.get(
                    "yyyyMMddHHmm '" + i + "'",
                    Instant.class, env.getLocale(), env.getTimeZone(), env);
            expectedMapSize++;
            // Following builds on implementation details (not on API contract), so adjust this if that changes.
            if (expectedMapSize > JavaTemplateTemporalFormatFactory.GUARANTEED_RECENT_ENTRIES * 2) {
                // The flushed entries are kept, and just become "old" entries, therefore after flushing we have
                // GUARANTEED_RECENT_ENTRIES entries. Then the new entry that caused the flushing is added to the
                // "recent" entries, hence the +1.
                expectedMapSize = JavaTemplateTemporalFormatFactory.GUARANTEED_RECENT_ENTRIES + 1;
            }
        }
    }

    @Test
    public void testGlobalJavaDateFormatCacheLeakProtection() throws Exception {
        JavaTemplateDateFormatFactory formatFactory = JavaTemplateDateFormatFactory.INSTANCE;
        formatFactory.clear();
        int expectedMapSize = 0;
        for (int i = 0; i < JavaTemplateDateFormatFactory.GUARANTEED_RECENT_ENTRIES * 3 * 2 + 1; i++) {
            assertEquals(expectedMapSize, formatFactory.getSize());
            formatFactory.get(
                    "yyyyMMddHHmm '" + i + "'",
                    TemplateDateModel.DATETIME, env.getLocale(), env.getTimeZone(), false, env);
            expectedMapSize++;
            // Following builds on implementation details (not on API contract), so adjust this if that changes.
            if (expectedMapSize > JavaTemplateDateFormatFactory.GUARANTEED_RECENT_ENTRIES * 2) {
                // The flushed entries are kept, and just become "old" entries, therefore after flushing we have
                // GUARANTEED_RECENT_ENTRIES entries. Then the new entry that caused the flushing is added to the
                // "recent" entries, hence the +1.
                expectedMapSize = JavaTemplateDateFormatFactory.GUARANTEED_RECENT_ENTRIES + 1;
            }
        }
    }

    @Test
    public void testGlobalNumberFormatCacheLeakProtection() throws Exception {
        JavaTemplateNumberFormatFactory formatFactory = JavaTemplateNumberFormatFactory.INSTANCE;
        formatFactory.clear();
        int expectedMapSize = 0;
        for (int i = 0; i < JavaTemplateNumberFormatFactory.GUARANTEED_RECENT_ENTRIES * 3 * 2 + 1; i++) {
            assertEquals(expectedMapSize, formatFactory.getSize());
            formatFactory.get(
                    "#.# '" + i + "'",
                    env.getLocale(), env);
            expectedMapSize++;
            // Following builds on implementation details (not on API contract), so adjust this if that changes.
            if (expectedMapSize > JavaTemplateNumberFormatFactory.GUARANTEED_RECENT_ENTRIES * 2) {
                // The flushed entries are kept, and just become "old" entries, therefore after flushing we have
                // GUARANTEED_RECENT_ENTRIES entries. Then the new entry that caused the flushing is added to the
                // "recent" entries, hence the +1.
                expectedMapSize = JavaTemplateNumberFormatFactory.GUARANTEED_RECENT_ENTRIES + 1;
            }
        }
    }

    @Test
    public void testEnvironmentLevelByFormatCacheMemoryLeakProtection() throws Exception {
        env.clearCachedTemplateTemporalFormatsByFormatString();
        int expectedMapSize = 0;
        for (int i = 0; i < Environment.CACHED_TEMPORAL_FORMATS_BY_FORMAT_STRING_MAX_SIZE_PER_CLASS * 3 + 1; i++) {
            assertEquals(expectedMapSize, env.getTemplateTemporalFormatsByFormatStringMapSize(Instant.class));
            env.getTemplateTemporalFormat("yyyyMMddHHmm '" + i + "'", Instant.class);
            expectedMapSize++;
            if (expectedMapSize > Environment.CACHED_TEMPORAL_FORMATS_BY_FORMAT_STRING_MAX_SIZE_PER_CLASS) {
                expectedMapSize = 1; // Because we should store the new entry that caused the flushing
            }
        }
    }
}
