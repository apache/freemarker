package freemarker.cache;

import freemarker.template.Configuration;

/**
 * Interface that can be implemented by template loaders that maintain some 
 * sort of internal state (i.e. caches of earlier lookups for performance 
 * optimization purposes etc.) and support resetting of their state. 
 * @author Attila Szegedi
 */
public interface StatefulTemplateLoader extends TemplateLoader
{
    /**
     * Invoked by {@link Configuration#clearTemplateCache()} to instruct this
     * template loader to throw away its current state and start afresh. 
     */
    public void resetState();
}
