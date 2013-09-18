package freemarker.cache;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CacheAPI {

    public static TemplateLoader createLegacyDefaultTemplateLoader() {
        return TemplateCache.createLegacyDefaultTemplateLoader();
    }
    
}
