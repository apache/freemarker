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

package org.apache.freemarker.core.templateresolver;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StrongCacheStorage;
import org.apache.freemarker.test.MonitoredTemplateLoader;
import org.apache.freemarker.test.MonitoredTemplateLoader.CloseSessionEvent;
import org.apache.freemarker.test.MonitoredTemplateLoader.CreateSessionEvent;
import org.apache.freemarker.test.MonitoredTemplateLoader.LoadEvent;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DefaultTemplateResolverTest {

    @Test
    public void testCachedException() throws Exception {
        MockTemplateLoader loader = new MockTemplateLoader();
        Configuration cfg = new TestConfigurationBuilder()
                .templateLoader(loader)
                .templateCacheStorage(new StrongCacheStorage())
                .templateUpdateDelayMilliseconds(100L)
                .build();
        TemplateResolver tr = cfg.getTemplateResolver();
        assertThat(tr, instanceOf(DefaultTemplateResolver.class));

        loader.setThrowException(true);
        try {
            tr.getTemplate("t", Locale.getDefault(), null).getTemplate();
            fail();
        } catch (IOException e) {
            assertEquals("mock IO exception", e.getMessage());
            assertEquals(1, loader.getLoadAttemptCount());
            try {
                tr.getTemplate("t", Locale.getDefault(), null).getTemplate();
                fail();
            } catch (IOException e2) {
                // Still 1 - returned cached exception
                assertThat(e2.getMessage(),
                        Matchers.allOf(Matchers.containsString("There was an error loading the template on an " +
                        "earlier attempt")));
                assertSame(e, e2.getCause());
                assertEquals(1, loader.getLoadAttemptCount());
                try {
                    Thread.sleep(132L);
                    tr.getTemplate("t", Locale.getDefault(), null).getTemplate();
                    fail();
                } catch (IOException e3) {
                    // Cache had to retest
                    assertEquals("mock IO exception", e.getMessage());
                    assertEquals(2, loader.getLoadAttemptCount());
                }
            }
        }
    }
    
    @Test
    public void testCachedNotFound() throws Exception {
        MockTemplateLoader loader = new MockTemplateLoader();
        Configuration cfg = new TestConfigurationBuilder()
                .templateLoader(loader)
                .templateCacheStorage(new StrongCacheStorage())
                .templateUpdateDelayMilliseconds(100L)
                .localizedTemplateLookup(false)
                .build();
        TemplateResolver tr = cfg.getTemplateResolver();
        assertThat(tr, instanceOf(DefaultTemplateResolver.class));

        assertNull(tr.getTemplate("t", Locale.getDefault(), null).getTemplate());
        assertEquals(1, loader.getLoadAttemptCount());
        assertNull(tr.getTemplate("t", Locale.getDefault(), null).getTemplate());
        // Still 1 - returned cached exception
        assertEquals(1, loader.getLoadAttemptCount());
        Thread.sleep(132L);
        assertNull(tr.getTemplate("t", Locale.getDefault(), null).getTemplate());
        // Cache had to retest
        assertEquals(2, loader.getLoadAttemptCount());
    }

    private static class MockTemplateLoader implements TemplateLoader {
        private boolean throwException;
        private int loadAttemptCount; 
        
        public void setThrowException(boolean throwException) {
           this.throwException = throwException;
        }
        
        public int getLoadAttemptCount() {
            return loadAttemptCount;
        }
        
        @Override
        public TemplateLoaderSession createSession() {
            return null;
        }

        @Override
        public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
                Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
            ++loadAttemptCount;
            if (throwException) {
                throw new IOException("mock IO exception");
            }
            return TemplateLoadingResult.NOT_FOUND;
        }

        @Override
        public void resetState() {
            //
        }
        
    }
    
    @Test
    public void testManualRemovalPlain() throws Exception {
        StringTemplateLoader loader = new StringTemplateLoader();
        Configuration cfg = new TestConfigurationBuilder()
                .templateCacheStorage(new StrongCacheStorage())
                .templateLoader(loader)
                .templateUpdateDelayMilliseconds(Long.MAX_VALUE)
                .build();

        loader.putTemplate("1.f3ah", "1 v1");
        loader.putTemplate("2.f3ah", "2 v1");
        assertEquals("1 v1", cfg.getTemplate("1.f3ah").toString()); 
        assertEquals("2 v1", cfg.getTemplate("2.f3ah").toString());
        
        loader.putTemplate("1.f3ah", "1 v2");
        loader.putTemplate("2.f3ah", "2 v2");
        assertEquals("1 v1", cfg.getTemplate("1.f3ah").toString()); // no change 
        assertEquals("2 v1", cfg.getTemplate("2.f3ah").toString()); // no change
        
        cfg.removeTemplateFromCache("1.f3ah", cfg.getLocale(), null);
        assertEquals("1 v2", cfg.getTemplate("1.f3ah").toString()); // changed 
        assertEquals("2 v1", cfg.getTemplate("2.f3ah").toString());
        
        cfg.removeTemplateFromCache("2.f3ah", cfg.getLocale(), null);
        assertEquals("1 v2", cfg.getTemplate("1.f3ah").toString()); 
        assertEquals("2 v2", cfg.getTemplate("2.f3ah").toString()); // changed
    }

    @Test
    public void testManualRemovalI18ed() throws Exception {
        StringTemplateLoader loader = new StringTemplateLoader();
        Configuration cfg = new TestConfigurationBuilder()
                .templateCacheStorage(new StrongCacheStorage())
                .templateLoader(loader)
                .templateUpdateDelayMilliseconds(Long.MAX_VALUE)
                .build();

        loader.putTemplate("1_en_US.f3ah", "1_en_US v1");
        loader.putTemplate("1_en.f3ah", "1_en v1");
        loader.putTemplate("1.f3ah", "1 v1");
        
        assertEquals("1_en_US v1", cfg.getTemplate("1.f3ah").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.f3ah", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.f3ah", Locale.GERMANY).toString());
        
        loader.putTemplate("1_en_US.f3ah", "1_en_US v2");
        loader.putTemplate("1_en.f3ah", "1_en v2");
        loader.putTemplate("1.f3ah", "1 v2");
        assertEquals("1_en_US v1", cfg.getTemplate("1.f3ah").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.f3ah", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.f3ah", Locale.GERMANY).toString());
        
        cfg.removeTemplateFromCache("1.f3ah", cfg.getLocale(), null);
        assertEquals("1_en_US v2", cfg.getTemplate("1.f3ah").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.f3ah", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.f3ah", Locale.GERMANY).toString());
        assertEquals("1 v2", cfg.getTemplate("1.f3ah", Locale.ITALY).toString());
        
        cfg.removeTemplateFromCache("1.f3ah", Locale.GERMANY, null);
        assertEquals("1_en v1", cfg.getTemplate("1.f3ah", Locale.UK).toString());        
        assertEquals("1 v2", cfg.getTemplate("1.f3ah", Locale.GERMANY).toString());

        cfg.removeTemplateFromCache("1.f3ah", Locale.CANADA, null);
        assertEquals("1_en v1", cfg.getTemplate("1.f3ah", Locale.UK).toString());
        
        cfg.removeTemplateFromCache("1.f3ah", Locale.UK, null);
        assertEquals("1_en v2", cfg.getTemplate("1.f3ah", Locale.UK).toString());        
    }

    @Test
    public void testZeroUpdateDelay() throws Exception {
        MonitoredTemplateLoader loader = new MonitoredTemplateLoader();

        {
            Configuration cfg = new TestConfigurationBuilder()
                    .templateCacheStorage(new StrongCacheStorage())
                    .templateLoader(loader)
                    .templateUpdateDelayMilliseconds(0L)
                    .build();
            for (int i = 1; i <= 3; i++) {
                loader.putTextTemplate("t.f3ah", "v" + i);
                assertEquals("v" + i, cfg.getTemplate("t.f3ah").toString());
            }

            loader.clearEvents();
            loader.putTextTemplate("t.f3ah", "v8");
            assertEquals("v8", cfg.getTemplate("t.f3ah").toString());
            assertEquals("v8", cfg.getTemplate("t.f3ah").toString());
            loader.putTextTemplate("t.f3ah", "v9");
            assertEquals("v9", cfg.getTemplate("t.f3ah").toString());
            assertEquals("v9", cfg.getTemplate("t.f3ah").toString());
            assertEquals(
                    ImmutableList.of(
                            new LoadEvent("t_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND), // v8
                            new LoadEvent("t_en.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.OPENED),

                            new LoadEvent("t_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND), // v8
                            new LoadEvent("t_en.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED),

                            new LoadEvent("t_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND), // v9
                            new LoadEvent("t_en.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.OPENED),

                            new LoadEvent("t_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND), // v9
                            new LoadEvent("t_en.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED)
                    ),
                    loader.getEvents(LoadEvent.class));
        }

        {
            Configuration cfg = new TestConfigurationBuilder()
                    .templateCacheStorage(new StrongCacheStorage())
                    .templateLoader(loader)
                    .templateUpdateDelayMilliseconds(0L)
                    .localizedTemplateLookup(false)
                    .build();
            loader.clearEvents();
            loader.putTextTemplate("t.f3ah", "v10");
            assertEquals("v10", cfg.getTemplate("t.f3ah").toString());
            loader.putTextTemplate("t.f3ah", "v11"); // same time stamp, different content
            assertEquals("v11", cfg.getTemplate("t.f3ah").toString());
            assertEquals("v11", cfg.getTemplate("t.f3ah").toString());
            assertEquals("v11", cfg.getTemplate("t.f3ah").toString());
            Thread.sleep(17L);
            assertEquals("v11", cfg.getTemplate("t.f3ah").toString());
            loader.putTextTemplate("t.f3ah", "v12");
            assertEquals("v12", cfg.getTemplate("t.f3ah").toString());
            assertEquals("v12", cfg.getTemplate("t.f3ah").toString());
            assertEquals(
                    ImmutableList.of(
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.OPENED), // v10
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.OPENED), // v11
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED),
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.OPENED), // v12
                            new LoadEvent("t.f3ah", TemplateLoadingResultStatus.NOT_MODIFIED)
                    ),
                    loader.getEvents(LoadEvent.class));
        }
    }
    
    @Test
    public void testWrongEncodingReload() throws Exception {
        MonitoredTemplateLoader loader = new MonitoredTemplateLoader();
        loader.putBinaryTemplate("utf-8_en.f3ah", "<#ftl encoding='utf-8'>Béka");
        loader.putBinaryTemplate("utf-8.f3ah", "Bar");
        loader.putBinaryTemplate("iso-8859-1_en_US.f3ah", "<#ftl encoding='ISO-8859-1'>Béka",
                StandardCharsets.ISO_8859_1, "v1");
        Configuration cfg = new TestConfigurationBuilder().templateLoader(loader).build();

        {
            Template t = cfg.getTemplate("utf-8.f3ah");
            assertEquals("utf-8.f3ah", t.getLookupName());
            assertEquals("utf-8_en.f3ah", t.getSourceName());
            assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
            assertEquals("Béka", t.toString());
            
            assertEquals(
                    ImmutableList.of(
                            CreateSessionEvent.INSTANCE,
                            new LoadEvent("utf-8_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("utf-8_en.f3ah", TemplateLoadingResultStatus.OPENED),
                            CloseSessionEvent.INSTANCE),
                    loader.getEvents());
        }

        {
            loader.clearEvents();
            
            Template t = cfg.getTemplate("iso-8859-1.f3ah");
            assertEquals("iso-8859-1.f3ah", t.getLookupName());
            assertEquals("iso-8859-1_en_US.f3ah", t.getSourceName());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
            assertEquals("Béka", t.toString());
            
            assertEquals(
                    ImmutableList.of(
                            CreateSessionEvent.INSTANCE,
                            new LoadEvent("iso-8859-1_en_US.f3ah", TemplateLoadingResultStatus.OPENED),
                            CloseSessionEvent.INSTANCE),
                    loader.getEvents());
        }
    }

    @Test
    public void testNoWrongEncodingForTemplateLoader2WithReader() throws Exception {
        MonitoredTemplateLoader loader = new MonitoredTemplateLoader();
        loader.putTextTemplate("foo_en.f3ah", "<#ftl encoding='utf-8'>ő");
        loader.putTextTemplate("foo.f3ah", "B");
        Configuration cfg = new TestConfigurationBuilder().templateLoader(loader).build();
        
        {
            Template t = cfg.getTemplate("foo.f3ah");
            assertEquals("foo.f3ah", t.getLookupName());
            assertEquals("foo_en.f3ah", t.getSourceName());
            assertNull(t.getActualSourceEncoding());
            assertEquals("ő", t.toString());
            
            assertEquals(
                    ImmutableList.of(
                            CreateSessionEvent.INSTANCE,
                            new LoadEvent("foo_en_US.f3ah", TemplateLoadingResultStatus.NOT_FOUND),
                            new LoadEvent("foo_en.f3ah", TemplateLoadingResultStatus.OPENED),
                            CloseSessionEvent.INSTANCE),                
                    loader.getEvents());
        }
    }

    @Test
    public void testTemplateNameFormatException() throws Exception {
        Configuration cfg = new TestConfigurationBuilder()
                .templateNameFormat(DefaultTemplateNameFormat.INSTANCE)
                .build();
        try {
            cfg.getTemplate("../x");
            fail();
        } catch (MalformedTemplateNameException e) {
            // expected
        }
        try {
            cfg.getTemplate("\\x");
            fail();
        } catch (MalformedTemplateNameException e) {
            // expected
        }
    }

}
