package freemarker.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

import junit.framework.TestCase;
import freemarker.template.Configuration;

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
        TemplateCache cache = new TemplateCache(loader, new StrongCacheStorage());
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

}
