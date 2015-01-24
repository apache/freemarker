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

package freemarker.cache;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Locale;

import junit.framework.TestCase;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;

public class TemplateCacheTest extends TestCase
{
    public TemplateCacheTest(String name)
    {
        super(name);
    }

    public void testCachedException() throws Exception
    {
        MockTemplateLoader loader = new MockTemplateLoader();
        TemplateCache cache = new TemplateCache(loader, new StrongCacheStorage());
        cache.setDelay(1000L);
        loader.setThrowException(true);
        try
        {
            cache.getTemplate("t", Locale.getDefault(), "", true);
            fail();
        }
        catch(IOException e)
        {
            assertEquals("mock IO exception", e.getMessage());
            assertEquals(1, loader.getFindCount());
            try
            {
                cache.getTemplate("t", Locale.getDefault(), "", true);
                fail();
            }
            catch(IOException e2)
            {
                // Still 1 - returned cached exception
                assertEquals("There was an error loading the template on an " +
                        "earlier attempt; it's attached as a cause", e2.getMessage());
                assertSame(e, e2.getCause());
                assertEquals(1, loader.getFindCount());
                try
                {
                    Thread.sleep(1100L);
                    cache.getTemplate("t", Locale.getDefault(), "", true);
                    fail();
                }
                catch(IOException e3)
                {
                    // Cache had to retest
                    assertEquals("mock IO exception", e.getMessage());
                    assertEquals(2, loader.getFindCount());
                }
            }
        }
    }
    
    public void testCachedNotFound() throws Exception
    {
        MockTemplateLoader loader = new MockTemplateLoader();
        TemplateCache cache = new TemplateCache(loader, new StrongCacheStorage(), new Configuration());
        cache.setDelay(1000L);
        cache.setLocalizedLookup(false);
        assertNull(cache.getTemplate("t", Locale.getDefault(), "", true));
        assertEquals(1, loader.getFindCount());
        assertNull(cache.getTemplate("t", Locale.getDefault(), "", true));
        // Still 1 - returned cached exception
        assertEquals(1, loader.getFindCount());
        Thread.sleep(1100L);
        assertNull(cache.getTemplate("t", Locale.getDefault(), "", true));
        // Cache had to retest
        assertEquals(2, loader.getFindCount());
    }

    private static class MockTemplateLoader implements TemplateLoader
    {
        private boolean throwException;
        private int findCount; 
        
        public void setThrowException(boolean throwException)
        {
           this.throwException = throwException;
        }
        
        public int getFindCount()
        {
            return findCount;
        }
        
        public void closeTemplateSource(Object templateSource)
                throws IOException
        {
        }

        public Object findTemplateSource(String name) throws IOException
        {
            ++findCount;
            if(throwException)
            {
                throw new IOException("mock IO exception");
            }
            return null;
        }

        public long getLastModified(Object templateSource)
        {
            return 0;
        }

        public Reader getReader(Object templateSource, String encoding)
                throws IOException
        {
            return null;
        }
        
    }
    
    public void testManualRemovalPlain() throws IOException {
        Configuration cfg = new Configuration();
        cfg.setCacheStorage(new StrongCacheStorage());
        StringTemplateLoader loader = new StringTemplateLoader();
        cfg.setTemplateLoader(loader);
        cfg.setTemplateUpdateDelay(Integer.MAX_VALUE);
        
        loader.putTemplate("1.ftl", "1 v1");
        loader.putTemplate("2.ftl", "2 v1");
        assertEquals("1 v1", cfg.getTemplate("1.ftl").toString()); 
        assertEquals("2 v1", cfg.getTemplate("2.ftl").toString());
        
        loader.putTemplate("1.ftl", "1 v2");
        loader.putTemplate("2.ftl", "2 v2");
        assertEquals("1 v1", cfg.getTemplate("1.ftl").toString()); // no change 
        assertEquals("2 v1", cfg.getTemplate("2.ftl").toString()); // no change
        
        cfg.removeTemplateFromCache("1.ftl");
        assertEquals("1 v2", cfg.getTemplate("1.ftl").toString()); // changed 
        assertEquals("2 v1", cfg.getTemplate("2.ftl").toString());
        
        cfg.removeTemplateFromCache("2.ftl");
        assertEquals("1 v2", cfg.getTemplate("1.ftl").toString()); 
        assertEquals("2 v2", cfg.getTemplate("2.ftl").toString()); // changed
    }

    public void testManualRemovalI18ed() throws IOException {
        Configuration cfg = new Configuration();
        cfg.setCacheStorage(new StrongCacheStorage());
        cfg.setLocale(Locale.US);
        StringTemplateLoader loader = new StringTemplateLoader();
        cfg.setTemplateLoader(loader);
        cfg.setTemplateUpdateDelay(Integer.MAX_VALUE);
        
        loader.putTemplate("1_en_US.ftl", "1_en_US v1");
        loader.putTemplate("1_en.ftl", "1_en v1");
        loader.putTemplate("1.ftl", "1 v1");
        
        assertEquals("1_en_US v1", cfg.getTemplate("1.ftl").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.ftl", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.ftl", Locale.GERMANY).toString());
        
        loader.putTemplate("1_en_US.ftl", "1_en_US v2");
        loader.putTemplate("1_en.ftl", "1_en v2");
        loader.putTemplate("1.ftl", "1 v2");
        assertEquals("1_en_US v1", cfg.getTemplate("1.ftl").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.ftl", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.ftl", Locale.GERMANY).toString());
        
        cfg.removeTemplateFromCache("1.ftl");
        assertEquals("1_en_US v2", cfg.getTemplate("1.ftl").toString());        
        assertEquals("1_en v1", cfg.getTemplate("1.ftl", Locale.UK).toString());        
        assertEquals("1 v1", cfg.getTemplate("1.ftl", Locale.GERMANY).toString());
        assertEquals("1 v2", cfg.getTemplate("1.ftl", Locale.ITALY).toString());
        
        cfg.removeTemplateFromCache("1.ftl", Locale.GERMANY);
        assertEquals("1_en v1", cfg.getTemplate("1.ftl", Locale.UK).toString());        
        assertEquals("1 v2", cfg.getTemplate("1.ftl", Locale.GERMANY).toString());

        cfg.removeTemplateFromCache("1.ftl", Locale.CANADA);
        assertEquals("1_en v1", cfg.getTemplate("1.ftl", Locale.UK).toString());
        
        cfg.removeTemplateFromCache("1.ftl", Locale.UK);
        assertEquals("1_en v2", cfg.getTemplate("1.ftl", Locale.UK).toString());        
    }

    public void testZeroUpdateDelay() throws IOException {
        Configuration cfg = new Configuration();
        cfg.setLocale(Locale.US);
        cfg.setCacheStorage(new StrongCacheStorage());
        StringTemplateLoader loader = new StringTemplateLoader();
        cfg.setTemplateLoader(loader);
        cfg.setTemplateUpdateDelay(0);
        for (int i = 1; i <= 3; i++) {
            loader.putTemplate("t.ftl", "v" + i, i);
            assertEquals("v" + i, cfg.getTemplate("t.ftl").toString());
        }
        
        loader.putTemplate("t.ftl", "v10", 10);
        assertEquals("v10", cfg.getTemplate("t.ftl").toString());
        loader.putTemplate("t.ftl", "v11", 10); // same time stamp, different content
        assertEquals("v10", cfg.getTemplate("t.ftl").toString()); // still v10
        assertEquals("v10", cfg.getTemplate("t.ftl").toString()); // still v10
    }
    
    public void testIncompatibleImprovementsChangesURLConCaching() throws IOException {
        Version newVersion = Configuration.VERSION_2_3_21;
        Version oldVersion = Configuration.VERSION_2_3_20;
        
        {
            Configuration cfg = new Configuration(oldVersion);
            cfg.setTemplateUpdateDelay(0);
            final MonitoredClassTemplateLoader templateLoader = new MonitoredClassTemplateLoader();
            assertNull(templateLoader.getURLConnectionUsesCaches());
            cfg.setTemplateLoader(templateLoader);
            
            assertNull(templateLoader.getLastTemplateSourceModification());
            cfg.getTemplate("test.ftl");
            assertNull(templateLoader.getLastTemplateSourceModification());
            
            cfg.setIncompatibleImprovements(newVersion);
            assertNull(templateLoader.getLastTemplateSourceModification());
            cfg.getTemplate("test.ftl");
            assertEquals(Boolean.FALSE, templateLoader.getLastTemplateSourceModification());
            
            templateLoader.setURLConnectionUsesCaches(Boolean.valueOf(true));
            templateLoader.setLastTemplateSourceModification(null);
            cfg.getTemplate("test.ftl");
            assertNull(templateLoader.getLastTemplateSourceModification());
            
            templateLoader.setURLConnectionUsesCaches(Boolean.valueOf(false));
            templateLoader.setLastTemplateSourceModification(null);
            cfg.getTemplate("test.ftl");
            assertNull(templateLoader.getLastTemplateSourceModification());

            templateLoader.setURLConnectionUsesCaches(null);
            templateLoader.setLastTemplateSourceModification(null);
            cfg.getTemplate("test.ftl");
            assertEquals(Boolean.FALSE, templateLoader.getLastTemplateSourceModification());
            
            templateLoader.setURLConnectionUsesCaches(null);
            cfg.setIncompatibleImprovements(oldVersion);
            templateLoader.setLastTemplateSourceModification(null);
            cfg.getTemplate("test.ftl");
            assertNull(templateLoader.getLastTemplateSourceModification());
            
            cfg.setTemplateLoader(new MultiTemplateLoader(
                    new TemplateLoader[] { new MultiTemplateLoader(
                                    new TemplateLoader[] { templateLoader }) }));
            cfg.setIncompatibleImprovements(newVersion);
            cfg.getTemplate("test.ftl");
            assertEquals(Boolean.FALSE, templateLoader.getLastTemplateSourceModification());
        }
    }
    
    public void testWrongEncodingReload() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setLocale(Locale.US);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("utf-8_en.ftl", "<#ftl encoding='utf-8'>Foo");
        tl.putTemplate("utf-8.ftl", "Bar");
        cfg.setTemplateLoader(tl);
        
        {
            Template t = cfg.getTemplate("utf-8.ftl", "Utf-8");
            assertEquals("utf-8.ftl", t.getName());
            assertEquals("utf-8_en.ftl", t.getSourceName());
            assertEquals("Utf-8", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
        
        {
            Template t = cfg.getTemplate("utf-8.ftl", "Utf-16");
            assertEquals("utf-8.ftl", t.getName());
            assertEquals("utf-8_en.ftl", t.getSourceName());
            assertEquals("utf-8", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
    }

    public void testEncodingSelection() throws IOException {
        Locale hungary = new Locale("hu", "HU"); 
                
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setDefaultEncoding("utf-8");
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("t.ftl", "Foo");
        tl.putTemplate("t_de.ftl", "Vuu");
        tl.putTemplate("t2.ftl", "<#ftl encoding='UTF-16LE'>Foo");
        tl.putTemplate("t2_de.ftl", "<#ftl encoding='UTF-16BE'>Vuu");
        cfg.setTemplateLoader(tl);

        // No locale-to-encoding mapping exists yet:
        {
            Template t = cfg.getTemplate("t.ftl", Locale.GERMANY);
            assertEquals("t.ftl", t.getName());
            assertEquals("t_de.ftl", t.getSourceName());
            assertEquals("utf-8", t.getEncoding());
            assertEquals("Vuu", t.toString());
        }
        
        cfg.setEncoding(Locale.GERMANY, "ISO-8859-1");
        cfg.setEncoding(hungary, "ISO-8859-2");
        {
            Template t = cfg.getTemplate("t.ftl", Locale.CHINESE);
            assertEquals("t.ftl", t.getName());
            assertEquals("t.ftl", t.getSourceName());
            assertEquals("utf-8", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
        {
            Template t = cfg.getTemplate("t.ftl", Locale.GERMANY);
            assertEquals("t.ftl", t.getName());
            assertEquals("t_de.ftl", t.getSourceName());
            assertEquals("ISO-8859-1", t.getEncoding());
            assertEquals("Vuu", t.toString());
        }
        {
            Template t = cfg.getTemplate("t.ftl", hungary);
            assertEquals("t.ftl", t.getName());
            assertEquals("t.ftl", t.getSourceName());
            assertEquals("ISO-8859-2", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
        
        // #ftl header overrides:
        {
            Template t = cfg.getTemplate("t2.ftl", Locale.CHINESE);
            assertEquals("t2.ftl", t.getName());
            assertEquals("t2.ftl", t.getSourceName());
            assertEquals("UTF-16LE", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
        {
            Template t = cfg.getTemplate("t2.ftl", Locale.GERMANY);
            assertEquals("t2.ftl", t.getName());
            assertEquals("t2_de.ftl", t.getSourceName());
            assertEquals("UTF-16BE", t.getEncoding());
            assertEquals("Vuu", t.toString());
        }
        {
            Template t = cfg.getTemplate("t2.ftl", hungary);
            assertEquals("t2.ftl", t.getName());
            assertEquals("t2.ftl", t.getSourceName());
            assertEquals("UTF-16LE", t.getEncoding());
            assertEquals("Foo", t.toString());
        }
    }
    
    public void testTemplateNameFormatExceptionAndBackwardCompatibility() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        assertNull(cfg.getTemplate("../x", null, null, null, true, true));
        try {
            cfg.getTemplate("../x");
            fail();
        } catch (TemplateNotFoundException e) {
            // expected
        }
        
        // [2.4] Test it with IcI 2.4
        
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        try {
            cfg.getTemplate("../x", null, null, null, true, true);
            fail();
        } catch (MalformedTemplateNameException e) {
            // expected
        }
        try {
            cfg.getTemplate("../x");
            fail();
        } catch (MalformedTemplateNameException e) {
            // expected
        }
    }
    
    private static class MonitoredClassTemplateLoader extends ClassTemplateLoader {
        
        private Boolean lastTemplateSourceModification;

        public MonitoredClassTemplateLoader() {
            super(TemplateCacheTest.class, "");
        }

        @Override
        public Object findTemplateSource(String name) throws IOException {
            final URL url = getURL(name);
            if (url == null) return null;
            return new SpyingURLTemplateSource(url, getURLConnectionUsesCaches());
        }

        Boolean getLastTemplateSourceModification() {
            return lastTemplateSourceModification;
        }

        void setLastTemplateSourceModification(Boolean lastTemplateSourceModification) {
            this.lastTemplateSourceModification = lastTemplateSourceModification;
        }
        
        private class SpyingURLTemplateSource extends URLTemplateSource {
            
            SpyingURLTemplateSource(URL url, Boolean useCaches) throws IOException {
                super(url, useCaches);
            }

            @Override
            void setUseCaches(boolean useCaches) {
                setLastTemplateSourceModification(Boolean.valueOf(useCaches));
                super.setUseCaches(useCaches);
            }
            
        }
        
    }

}
