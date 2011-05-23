package freemarker.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

import freemarker.cache.StrongCacheStorage;
import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateLoader;
import junit.framework.TestCase;

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
                        "earlier attempt; it is attached as a cause", e2.getMessage());
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
            // TODO Auto-generated method stub
            
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
            // TODO Auto-generated method stub
            return 0;
        }

        public Reader getReader(Object templateSource, String encoding)
                throws IOException
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
